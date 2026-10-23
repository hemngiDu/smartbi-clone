package com.smartbi.query;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public record NamedParameterSql(String sql, List<String> names) {
    public static NamedParameterSql parse(String input) {
        var out = new StringBuilder(); var names = new ArrayList<String>(); boolean quoted = false;
        for (int i=0; i<input.length(); i++) {
            char c=input.charAt(i); if (c=='\'') { quoted=!quoted; out.append(c); continue; }
            if (!quoted && c==':' && i+1<input.length() && Character.isJavaIdentifierStart(input.charAt(i+1))) {
                int j=i+2; while(j<input.length() && Character.isJavaIdentifierPart(input.charAt(j))) j++;
                names.add(input.substring(i+1,j)); out.append('?'); i=j-1;
            } else out.append(c);
        }
        return new NamedParameterSql(out.toString(), List.copyOf(names));
    }
    public void bind(PreparedStatement statement, Map<String,Object> values) throws SQLException {
        for (int i=0;i<names.size();i++) {
            String name=names.get(i); if (!values.containsKey(name)) throw new QueryException("PARAMETER_MISSING", "Missing parameter: " + name);
            Object value=values.get(name);
            if (!(value == null || value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof UUID))
                throw new QueryException("PARAMETER_TYPE_INVALID", "Unsupported parameter type: " + name);
            statement.setObject(i+1, value);
        }
    }
}
