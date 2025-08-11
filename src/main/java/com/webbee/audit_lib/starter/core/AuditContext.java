package com.webbee.audit_lib.starter.core;

public class AuditContext {
    
    private static final ThreadLocal<String> correlationIdHolder = new ThreadLocal<>();
    
    public static void setCorrelationId(String correlationId) {
        correlationIdHolder.set(correlationId);
    }
    
    public static String getCorrelationId() {
        return correlationIdHolder.get();
    }
    
    public static void clear() {
        correlationIdHolder.remove();
    }
}