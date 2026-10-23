package com.smartbi.query;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class SqlSafetyValidator {
    private static final Pattern FORBIDDEN_WORD = Pattern.compile("\\b(insert|update|delete|merge|call|do|copy|create|alter|drop|truncate|grant|revoke|vacuum|analyze|refresh|lock)\\b");
    private static final Pattern SELECT_INTO = Pattern.compile("\\bselect\\b[\\s\\S]*?\\binto\\b");
    private static final Set<String> SIDE_EFFECT_FUNCTIONS = Set.of("pg_sleep","pg_terminate_backend","pg_cancel_backend","set_config","nextval","setval","lo_import","lo_export","pg_read_file","pg_read_binary_file","pg_write_file","dblink_exec","dblink_connect","dblink_connect_u");
    public void validate(String sql) {
        if (sql == null || sql.isBlank()) throw new QueryException("SQL_INVALID", "SQL is required");
        NamedParameterSql.parse(sql); String code = codeOnly(sql).toLowerCase(Locale.ROOT);
        if (code.indexOf(';') >= 0) throw new QueryException("SQL_UNSAFE", "Multiple statements are not allowed");
        if (FORBIDDEN_WORD.matcher(code).find() || SELECT_INTO.matcher(code).find()) throw new QueryException("SQL_UNSAFE", "Mutating SQL and SELECT INTO are not allowed");
        for (String function : SIDE_EFFECT_FUNCTIONS) if (Pattern.compile("\\b" + Pattern.quote(function) + "\\s*\\(").matcher(code).find()) throw new QueryException("SQL_UNSAFE", "Function is not allowed in read-only queries: " + function);
        try { if (!(CCJSqlParserUtil.parse(sql) instanceof Select)) throw new QueryException("SQL_UNSAFE", "Only a single SELECT or WITH query is allowed"); }
        catch (QueryException e) { throw e; } catch (Exception e) { throw new QueryException("SQL_INVALID", "SQL could not be parsed", e); }
    }
    static String codeOnly(String input) {
        StringBuilder out = new StringBuilder(input.length()); boolean single=false,dbl=false,line=false,block=false; String dollar=null;
        for(int i=0;i<input.length();) { char c=input.charAt(i), n=i+1<input.length()?input.charAt(i+1):'\0';
            if(dollar!=null){if(input.startsWith(dollar,i)){i+=dollar.length();dollar=null;}else i++;out.append(' ');continue;}
            if(line){if(c=='\n'||c=='\r'){line=false;out.append(c);}else out.append(' ');i++;continue;}
            if(block){if(c=='*'&&n=='/'){block=false;i+=2;out.append("  ");}else{i++;out.append(' ');}continue;}
            if(single){if(c=='\\'&&i+1<input.length()){out.append("  ");i+=2;}else if(c=='\''&&n=='\''){out.append("  ");i+=2;}else{if(c=='\'')single=false;out.append(' ');i++;}continue;}
            if(dbl){if(c=='"'&&n=='"'){out.append("  ");i+=2;}else{if(c=='"')dbl=false;out.append(' ');i++;}continue;}
            if(c=='\''){single=true;out.append(' ');i++;}else if(c=='"'){dbl=true;out.append(' ');i++;}else if(c=='-'&&n=='-'){line=true;out.append("  ");i+=2;}else if(c=='/'&&n=='*'){block=true;out.append("  ");i+=2;}else if(c=='$'&&NamedParameterSql.dollarTagAt(input,i)!=null){dollar=NamedParameterSql.dollarTagAt(input,i);out.append(" ".repeat(dollar.length()));i+=dollar.length();}else{out.append(c);i++;}
        } return out.toString();
    }
}
