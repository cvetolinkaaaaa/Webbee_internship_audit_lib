package com.webbee.audit_lib.starter.http.logger;

import com.webbee.audit_lib.starter.config.AuditProperties;
import com.webbee.audit_lib.starter.http.event.HttpRequestEvent;

/**
 * Интерфейс для логгеров HTTP запросов.
 */
public interface HttpRequestLogger {

    /**
     * Записывает HTTP событие.
     */
    void log(HttpRequestEvent event);

    /**
     * Проверяет, поддерживает ли данный логгер указанный режим логирования.
     */
    boolean supports(AuditProperties.LoggingMode mode);

}
