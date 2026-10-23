package com.smartbi.query;

public class QueryException extends RuntimeException {
    private final String code;
    public QueryException(String code, String message) { super(message); this.code = code; }
    public QueryException(String code, String message, Throwable cause) { super(message, cause); this.code = code; }
    public String code() { return code; }
}
