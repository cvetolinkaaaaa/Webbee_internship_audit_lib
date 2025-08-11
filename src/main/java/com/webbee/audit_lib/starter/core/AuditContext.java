package com.webbee.audit_lib.starter.core;

public final class AuditContext {

    private AuditContext() {
    }

    private static final ThreadLocal<String> CORRELATION_ID_HOLDER = new ThreadLocal<>();

    public static void setCorrelationId(String correlationId) {
        CORRELATION_ID_HOLDER.set(correlationId);
    }

    public static String getCorrelationId() {
        return CORRELATION_ID_HOLDER.get();
    }

    public static void clear() {
        CORRELATION_ID_HOLDER.remove();
    }

}
