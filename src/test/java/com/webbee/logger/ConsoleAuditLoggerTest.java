package com.webbee.logger;

import com.webbee.audit_lib.starter.config.AuditProperties;
import com.webbee.audit_lib.starter.core.AuditEvent;
import com.webbee.audit_lib.starter.logger.ConsoleAuditLogger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleAuditLoggerTest {

    private final ConsoleAuditLogger logger = new ConsoleAuditLogger();

    @Test
    void shouldSupportConsoleMode() {
        boolean supports = logger.supports(AuditProperties.LoggingMode.CONSOLE);
        
        assertThat(supports).isTrue();
    }

    @Test
    void shouldNotSupportOtherModes() {
        assertThat(logger.supports(AuditProperties.LoggingMode.FILE)).isFalse();
        assertThat(logger.supports(AuditProperties.LoggingMode.KAFKA)).isFalse();
    }

}