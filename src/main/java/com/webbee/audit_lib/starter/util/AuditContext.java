package com.webbee.audit_lib.starter.util;

/**
 * Утилита для управления контекстом аудита в рамках текущего потока.
 */
public final class AuditContext {

    private AuditContext() {
    }

    /**
     * Потокобезопасное хранилище correlation ID.
     */
    private static final ThreadLocal<String> CORRELATION_ID_HOLDER = new ThreadLocal<>();

    /**
     * Устанавливает correlation ID для текущего потока.
     */
    public static void setCorrelationId(String correlationId) {
        CORRELATION_ID_HOLDER.set(correlationId);
    }

    /**
     * Получает correlation ID для текущего потока.
     */
    public static String getCorrelationId() {
        return CORRELATION_ID_HOLDER.get();
    }

    /**
     * Очищает correlation ID для текущего потока.
     */
    public static void clear() {
        CORRELATION_ID_HOLDER.remove();
    }

}
