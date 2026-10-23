package com.smartbi.query;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public record NamedParameterSql(String sql, List<String> names) {
    private enum State { CODE, SINGLE_QUOTE, DOUBLE_QUOTE, LINE_COMMENT, BLOCK_COMMENT, DOLLAR_QUOTE }
    public static NamedParameterSql parse(String input) {
        Objects.requireNonNull(input, "input");
        var out = new StringBuilder(input.length()); var names = new ArrayList<String>();
        State state = State.CODE; String dollarTag = null; int blockDepth = 0;
        for (int i = 0; i < input.length();) {
            char c = input.charAt(i), n = i + 1 < input.length() ? input.charAt(i + 1) : '\0';
            if (state == State.CODE) {
                if (c == '\'') { state = State.SINGLE_QUOTE; out.append(c); i++; continue; }
                if (c == '"') { state = State.DOUBLE_QUOTE; out.append(c); i++; continue; }
                if (c == '-' && n == '-') { state = State.LINE_COMMENT; out.append("--"); i += 2; continue; }
                if (c == '/' && n == '*') { state = State.BLOCK_COMMENT; blockDepth = 1; out.append("/*"); i += 2; continue; }
                if (c == '$') { String tag = dollarTagAt(input, i); if (tag != null) { state = State.DOLLAR_QUOTE; dollarTag = tag; out.append(tag); i += tag.length(); continue; } }
                if (c == ':' && n != ':' && (i == 0 || input.charAt(i - 1) != ':') && Character.isJavaIdentifierStart(n)) { int j = i + 2; while (j < input.length() && Character.isJavaIdentifierPart(input.charAt(j))) j++; names.add(input.substring(i + 1, j)); out.append('?'); i = j; continue; }
                out.append(c); i++; continue;
            }
            if (state == State.SINGLE_QUOTE) { out.append(c); i++; if (c == '\\' && i < input.length()) { out.append(input.charAt(i++)); continue; } if (c == '\'' && i < input.length() && input.charAt(i) == '\'') { out.append(input.charAt(i++)); continue; } if (c == '\'') state = State.CODE; }
            else if (state == State.DOUBLE_QUOTE) { out.append(c); i++; if (c == '"' && i < input.length() && input.charAt(i) == '"') { out.append(input.charAt(i++)); continue; } if (c == '"') state = State.CODE; }
            else if (state == State.LINE_COMMENT) { out.append(c); i++; if (c == '\n' || c == '\r') state = State.CODE; }
            else if (state == State.BLOCK_COMMENT) { if (c == '/' && n == '*') { blockDepth++; out.append("/*"); i += 2; } else if (c == '*' && n == '/') { blockDepth--; out.append("*/"); i += 2; if (blockDepth == 0) state = State.CODE; } else { out.append(c); i++; } }
            else { if (input.startsWith(dollarTag, i)) { out.append(dollarTag); i += dollarTag.length(); state = State.CODE; } else { out.append(c); i++; } }
        }
        if (state == State.SINGLE_QUOTE || state == State.DOUBLE_QUOTE || state == State.BLOCK_COMMENT || state == State.DOLLAR_QUOTE) throw new QueryException("SQL_INVALID", "Unterminated SQL quote or comment");
        return new NamedParameterSql(out.toString(), List.copyOf(names));
    }
    static String dollarTagAt(String input, int at) { int end = input.indexOf('$', at + 1); if (end < 0) return null; String body = input.substring(at + 1, end); if (!body.isEmpty() && (!Character.isLetter(body.charAt(0)) && body.charAt(0) != '_')) return null; if (!body.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '_')) return null; return input.substring(at, end + 1); }
    public void bind(PreparedStatement statement, Map<String,Object> values) throws SQLException { for (int i=0;i<names.size();i++) { String name=names.get(i); if (!values.containsKey(name)) throw new QueryException("PARAMETER_MISSING", "Missing parameter: " + name); Object value=values.get(name); if (!(value == null || value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof UUID)) throw new QueryException("PARAMETER_TYPE_INVALID", "Unsupported parameter type: " + name); statement.setObject(i+1, value); } }
}
