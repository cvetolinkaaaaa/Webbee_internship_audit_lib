package com.webbee.audit_lib.starter.logger;

import com.webbee.audit_lib.starter.model.AuditEvent;
import com.webbee.audit_lib.starter.config.AuditProperties;

/**
 * Интерфейс для реализации различных способов логирования событий аудита.
 */
public interface AuditLogger {

    /**
     * Записывает событие аудита.
     */
    void log(AuditEvent event);

    /**
     * Проверяет, поддерживает ли данный логгер указанный режим логирования.
     */
    boolean supports(AuditProperties.LoggingMode mode);

}
