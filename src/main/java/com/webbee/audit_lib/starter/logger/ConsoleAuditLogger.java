package com.webbee.audit_lib.starter.logger;

import com.webbee.audit_lib.starter.model.AuditEvent;
import com.webbee.audit_lib.starter.config.AuditProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Реализация логгера для вывода событий аудита в консоль.
 */
@Component
@ConditionalOnProperty(prefix = "audit.console", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConsoleAuditLogger implements AuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("AUDIT_CONSOLE");

    /**
     * Записывает событие аудита в консоль.
     */
    @Override
    public void log(AuditEvent event) {
        String logLevel = event.getLogLevel().toLowerCase();
        String message = event.toString();
        switch (logLevel) {
            case "trace" -> LOGGER.trace(message);
            case "debug" -> LOGGER.debug(message);
            case "info" -> LOGGER.info(message);
            case "warn" -> LOGGER.warn(message);
            case "error" -> LOGGER.error(message);
            default -> LOGGER.info(message);
        }
    }

    @Override
    public boolean supports(AuditProperties.LoggingMode mode) {
        return mode == AuditProperties.LoggingMode.CONSOLE;
    }

}
