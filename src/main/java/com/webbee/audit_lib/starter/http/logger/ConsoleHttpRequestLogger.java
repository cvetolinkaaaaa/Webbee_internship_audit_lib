package com.webbee.audit_lib.starter.http.logger;

import com.webbee.audit_lib.starter.config.AuditProperties;
import com.webbee.audit_lib.starter.http.event.HttpRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Консольный логгер для HTTP запросов.
 */
@Component
@ConditionalOnProperty(prefix = "audit.http.console", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConsoleHttpRequestLogger implements HttpRequestLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("HTTP_REQUEST_CONSOLE");

    /**
     * Выводит HTTP событие в консоль.
     */
    @Override
    public void log(HttpRequestEvent event) {
        LOGGER.info(event.toString());
    }

    @Override
    public boolean supports(AuditProperties.LoggingMode mode) {
        return mode == AuditProperties.LoggingMode.CONSOLE;
    }

}
