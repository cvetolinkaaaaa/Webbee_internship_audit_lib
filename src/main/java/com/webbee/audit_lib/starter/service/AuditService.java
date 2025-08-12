package com.webbee.audit_lib.starter.service;

import com.webbee.audit_lib.starter.config.AuditProperties;
import com.webbee.audit_lib.starter.util.AuditContext;
import com.webbee.audit_lib.starter.model.AuditEvent;
import com.webbee.audit_lib.starter.annotation.AuditLog;
import com.webbee.audit_lib.starter.logger.AuditLogger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Основной сервис для аудита выполнения методов.
 */
@Service
public class AuditService {

    private final List<AuditLogger> auditLoggers;
    private final AuditProperties auditProperties;

    public AuditService(List<AuditLogger> auditLoggers, AuditProperties auditProperties) {
        this.auditLoggers = auditLoggers;
        this.auditProperties = auditProperties;
    }

    /**
     * Логирует начало выполнения метода.
     */
    public void logStart(String methodName, Object[] args, AuditLog.LogLevel logLevel) {
        if (!auditProperties.isEnabled()) {
            return;
        }

        String correlationId = UUID.randomUUID().toString();
        AuditEvent event = createEvent(correlationId, methodName, "START", logLevel);
        event.setArguments(args);

        AuditContext.setCorrelationId(correlationId);

        logEvent(event);
    }

    /**
     * Логирует успешное окончание выполнения метода.
     */
    public void logEnd(String methodName, Object result, AuditLog.LogLevel logLevel) {
        if (!auditProperties.isEnabled()) {
            return;
        }

        String correlationId = AuditContext.getCorrelationId();
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        AuditEvent event = createEvent(correlationId, methodName, "END", logLevel);
        event.setResult(result);
        logEvent(event);
        AuditContext.clear();
    }

    /**
     * Логирует ошибку выполнения метода.
     */
    public void logError(String methodName, Throwable throwable, AuditLog.LogLevel logLevel) {
        if (!auditProperties.isEnabled()) {
            return;
        }
        String correlationId = AuditContext.getCorrelationId();
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        AuditEvent event = createEvent(correlationId, methodName, "ERROR", logLevel);
        event.setErrorMessage(throwable.getMessage());
        logEvent(event);
        AuditContext.clear();
    }

    /**
     * Создает событие аудита с базовыми параметрами.
     */
    private AuditEvent createEvent(String correlationId, String methodName, String eventType, AuditLog.LogLevel logLevel) {
        AuditEvent event = new AuditEvent();
        event.setCorrelationId(correlationId);
        event.setMethodName(methodName);
        event.setEventType(eventType);
        event.setLogLevel(logLevel.name());
        return event;
    }

    /**
     * Отправляет событие всем подходящим логгерам.
     */
    private void logEvent(AuditEvent event) {
        auditProperties.getModes().forEach(mode ->
            auditLoggers.stream()
                .filter(logger -> logger.supports(mode))
                .forEach(logger -> logger.log(event))
        );
    }

}
