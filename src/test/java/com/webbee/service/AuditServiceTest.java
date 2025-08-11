package com.webbee.service;

import com.webbee.audit_lib.starter.config.AuditProperties;
import com.webbee.audit_lib.starter.core.AuditContext;
import com.webbee.audit_lib.starter.core.AuditEvent;
import com.webbee.audit_lib.starter.core.AuditLog;
import com.webbee.audit_lib.starter.core.AuditService;
import com.webbee.audit_lib.starter.logger.AuditLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AuditServiceTest {
    
    @Mock
    private AuditLogger auditLogger1;
    
    @Mock
    private AuditLogger auditLogger2;
    
    @Mock
    private AuditProperties auditProperties;
    
    private AuditService auditService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auditService = new AuditService(List.of(auditLogger1, auditLogger2), auditProperties);
        AuditContext.clear();
    }
    
    @AfterEach
    void cleanup() {
        AuditContext.clear();
    }
    
    @Test
    void shouldLogStartWhenEnabled() {
        when(auditProperties.isEnabled()).thenReturn(true);
        when(auditProperties.getModes()).thenReturn((List.of(AuditProperties.LoggingMode.CONSOLE)));
        when(auditLogger1.supports(AuditProperties.LoggingMode.CONSOLE)).thenReturn(true);
        when(auditLogger2.supports(AuditProperties.LoggingMode.CONSOLE)).thenReturn(false);
        
        Object[] args = {"arg1", "arg2"};
        
        auditService.logStart("TestMethod", args, AuditLog.LogLevel.INFO);
        
        verify(auditLogger1).log(any(AuditEvent.class));
        verify(auditLogger2, never()).log(any(AuditEvent.class));
        assertNotNull(AuditContext.getCorrelationId());
    }
    
    @Test
    void shouldNotLogStartWhenDisabled() {
        when(auditProperties.isEnabled()).thenReturn(false);
        
        auditService.logStart("TestMethod", new Object[]{}, AuditLog.LogLevel.INFO);
        
        verify(auditLogger1, never()).log(any(AuditEvent.class));
        verify(auditLogger2, never()).log(any(AuditEvent.class));
        assertNull(AuditContext.getCorrelationId());
    }
    
    @Test
    void shouldLogEndWithExistingCorrelationId() {
        when(auditProperties.isEnabled()).thenReturn(true);
        when(auditProperties.getModes()).thenReturn(List.of(AuditProperties.LoggingMode.FILE));
        when(auditLogger1.supports(AuditProperties.LoggingMode.FILE)).thenReturn(true);
        
        AuditContext.setCorrelationId("existing-correlation-id");
        
        auditService.logEnd("TestMethod", "result", AuditLog.LogLevel.INFO);
        
        verify(auditLogger1).log(any(AuditEvent.class));
        assertNull(AuditContext.getCorrelationId());
    }
    
    @Test
    void shouldLogEndWithoutExistingCorrelationId() {
        when(auditProperties.isEnabled()).thenReturn(true);
        when(auditProperties.getModes()).thenReturn(List.of(AuditProperties.LoggingMode.CONSOLE));
        when(auditLogger1.supports(AuditProperties.LoggingMode.CONSOLE)).thenReturn(true);
        
        auditService.logEnd("TestMethod", "result", AuditLog.LogLevel.INFO);
        
        verify(auditLogger1).log(any(AuditEvent.class));
        assertNull(AuditContext.getCorrelationId());
    }
    
    @Test
    void shouldLogError() {
        when(auditProperties.isEnabled()).thenReturn(true);
        when(auditProperties.getModes()).thenReturn(List.of(AuditProperties.LoggingMode.KAFKA));
        when(auditLogger2.supports(AuditProperties.LoggingMode.KAFKA)).thenReturn(true);
        
        RuntimeException exception = new RuntimeException("Test error");
        AuditContext.setCorrelationId("error-correlation-id");
        
        auditService.logError("TestMethod", exception, AuditLog.LogLevel.ERROR);
        
        verify(auditLogger2).log(any(AuditEvent.class));
        assertNull(AuditContext.getCorrelationId());
    }
    
    @Test
    void shouldLogToMultipleLoggers() {
        when(auditProperties.isEnabled()).thenReturn(true);
        when(auditProperties.getModes()).thenReturn(List.of(
            AuditProperties.LoggingMode.CONSOLE, 
            AuditProperties.LoggingMode.FILE
        ));
        when(auditLogger1.supports(AuditProperties.LoggingMode.CONSOLE)).thenReturn(true);
        when(auditLogger1.supports(AuditProperties.LoggingMode.FILE)).thenReturn(false);
        when(auditLogger2.supports(AuditProperties.LoggingMode.CONSOLE)).thenReturn(false);
        when(auditLogger2.supports(AuditProperties.LoggingMode.FILE)).thenReturn(true);
        
        auditService.logStart("TestMethod", new Object[]{}, AuditLog.LogLevel.INFO);
        
        verify(auditLogger1).log(any(AuditEvent.class));
        verify(auditLogger2).log(any(AuditEvent.class));
    }
}