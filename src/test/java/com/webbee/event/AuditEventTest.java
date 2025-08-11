package com.webbee.event;

import com.webbee.audit_lib.starter.core.AuditEvent;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class AuditEventTest {

    @Test
    void shouldCreateAuditEventWithTimestamp() {
        AuditEvent event = new AuditEvent();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getTimestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    void shouldGenerateToStringWithAllFields() {
        AuditEvent event = new AuditEvent();
        event.setLogLevel("INFO");
        event.setEventType("START");
        event.setCorrelationId("test-id");
        event.setMethodName("TestMethod");
        event.setArguments(new Object[]{"arg1", "arg2"});
        event.setResult("test-result");
        event.setErrorMessage("test-error");
        
        String result = event.toString();
        
        assertThat(result).contains("INFO", "START", "test-id", "TestMethod");
        assertThat(result).contains("args = [arg1, arg2]");
        assertThat(result).contains("result = test-result");
        assertThat(result).contains("error = test-error");
    }

    @Test
    void shouldGenerateToStringWithoutOptionalFields() {
        AuditEvent event = new AuditEvent();
        event.setLogLevel("INFO");
        event.setEventType("START");
        event.setCorrelationId("test-id");
        event.setMethodName("TestMethod");
        
        String result = event.toString();
        
        assertThat(result).contains("INFO", "START", "test-id", "TestMethod");
        assertThat(result).doesNotContain("args =");
        assertThat(result).doesNotContain("result =");
        assertThat(result).doesNotContain("error =");
    }

    @Test
    void shouldSetAndGetAllProperties() {
        AuditEvent event = new AuditEvent();
        LocalDateTime timestamp = LocalDateTime.now();
        Object[] args = {"test"};
        
        event.setTimestamp(timestamp);
        event.setLogLevel("DEBUG");
        event.setEventType("END");
        event.setCorrelationId("correlation-123");
        event.setMethodName("testMethod");
        event.setArguments(args);
        event.setResult("success");
        event.setErrorMessage("error occurred");
        
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
        assertThat(event.getLogLevel()).isEqualTo("DEBUG");
        assertThat(event.getEventType()).isEqualTo("END");
        assertThat(event.getCorrelationId()).isEqualTo("correlation-123");
        assertThat(event.getMethodName()).isEqualTo("testMethod");
        assertThat(event.getArguments()).isEqualTo(args);
        assertThat(event.getResult()).isEqualTo("success");
        assertThat(event.getErrorMessage()).isEqualTo("error occurred");
    }
}