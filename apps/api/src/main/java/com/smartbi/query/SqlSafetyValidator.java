package com.smartbi.query;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Component;

@Component
public class SqlSafetyValidator {
    public void validate(String sql) {
        if (sql == null || sql.isBlank()) throw new QueryException("SQL_INVALID", "SQL is required");
        if (sql.contains(";") || sql.contains("--") || sql.contains("/*")) throw new QueryException("SQL_UNSAFE", "Comments and multiple statements are not allowed");
        try {
            if (!(CCJSqlParserUtil.parse(sql) instanceof Select)) throw new QueryException("SQL_UNSAFE", "Only a single SELECT or WITH query is allowed");
        } catch (QueryException e) { throw e; }
        catch (Exception e) { throw new QueryException("SQL_INVALID", "SQL could not be parsed", e); }
    }
}
