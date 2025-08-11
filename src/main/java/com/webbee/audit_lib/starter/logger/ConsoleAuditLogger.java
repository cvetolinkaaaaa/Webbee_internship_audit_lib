
package com.webbee.audit_lib.starter.logger;

import com.webbee.audit_lib.starter.core.AuditEvent;
import com.webbee.audit_lib.starter.config.AuditProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "audit.console", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConsoleAuditLogger implements AuditLogger {
    
    private static final Logger logger = LoggerFactory.getLogger("AUDIT_CONSOLE");
    
    @Override
    public void log(AuditEvent event) {
        String logLevel = event.getLogLevel().toLowerCase();
        String message = event.toString();
        
        switch (logLevel) {
            case "trace" -> logger.trace(message);
            case "debug" -> logger.debug(message);
            case "info" -> logger.info(message);
            case "warn" -> logger.warn(message);
            case "error" -> logger.error(message);
            default -> logger.info(message);
        }
    }
    
    @Override
    public boolean supports(AuditProperties.LoggingMode mode) {
        return mode == AuditProperties.LoggingMode.CONSOLE;
    }
}