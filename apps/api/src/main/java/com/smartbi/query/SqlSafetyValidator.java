package com.smartbi.query;

import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.NextValExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.Set;

/** Static inspection is defense-in-depth, not a proof that arbitrary functions are pure. */
@Component
public class SqlSafetyValidator {
    private static final Set<String> DENIED_FUNCTIONS = Set.of("pg_sleep","pg_terminate_backend","pg_cancel_backend","set_config","nextval","setval","lo_import","lo_export","pg_read_file","pg_read_binary_file","pg_write_file","dblink_exec","dblink_connect","dblink_connect_u");
    public void validate(String sql) {
        if (sql == null || sql.isBlank()) throw new QueryException("SQL_INVALID", "SQL is required");
        final Statement statement;
        try {var statements=CCJSqlParserUtil.parseStatements(NamedParameterSql.parse(sql).sql()).getStatements();if(statements.size()!=1)throw new QueryException("SQL_UNSAFE","Exactly one SQL statement is required");statement=statements.getFirst();}
        catch (QueryException e) { throw e; }
        catch (Exception e) { throw new QueryException("SQL_INVALID", "SQL could not be parsed as one statement", e); }
        if (!(statement instanceof Select select)) throw new QueryException("SQL_UNSAFE", "Only one SELECT or WITH query is allowed");
        new ReadOnlyAstInspector().inspect(select);
    }
    private static final class ReadOnlyAstInspector extends TablesNamesFinder<Void> {
        void inspect(Select select) { rejectLocks(select); getTableList((Statement)select); }
        @Override public <S> Void visit(WithItem<?> item,S context){if(!(item.getParenthesedStatement() instanceof Select))unsafe("Data-modifying CTEs are not allowed");return super.visit(item,context);}
        @Override public <S> Void visit(PlainSelect select,S context){if((select.getIntoTables()!=null&&!select.getIntoTables().isEmpty())||select.getIntoTempTable()!=null)unsafe("SELECT INTO is not allowed");rejectLocks(select);return super.visit(select,context);}
        @Override public <S> Void visit(Function function,S context){String name=function.getName();if(name!=null){String simple=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);if(DENIED_FUNCTIONS.contains(simple))unsafe("Function is not allowed in read-only queries: "+simple);}return super.visit(function,context);}
        @Override public <S> Void visit(NextValExpression expression,S context){unsafe("Sequence mutation is not allowed");return null;}
        private static void rejectLocks(Select select){if(select.getForClause()!=null||select.getForMode()!=null||select.getForUpdateTable()!=null||select.isSkipLocked())unsafe("Row-locking SELECT clauses are not allowed");}
        private static void unsafe(String message){throw new QueryException("SQL_UNSAFE",message);}
    }
}
