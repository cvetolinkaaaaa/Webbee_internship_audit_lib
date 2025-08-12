package com.webbee.service;

import com.webbee.audit_lib.starter.config.AuditProperties;
import com.webbee.audit_lib.starter.http.event.HttpRequestEvent;
import com.webbee.audit_lib.starter.http.logger.HttpRequestLogger;
import com.webbee.audit_lib.starter.http.service.HttpRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpRequestServiceTest {

    @Mock
    private HttpRequestLogger mockLogger;

    @Mock
    private AuditProperties mockProperties;

    @Mock
    private AuditProperties.HttpLogging mockHttpLogging;

    private HttpRequestService httpRequestService;

    @BeforeEach
    void setUp() {
        when(mockProperties.getHttp()).thenReturn(mockHttpLogging);
        httpRequestService = new HttpRequestService(List.of(mockLogger), mockProperties);
    }

    @Test
    void shouldNotLogWhenHttpDisabled() {
        when(mockHttpLogging.isEnabled()).thenReturn(false);
        
        HttpRequestEvent event = new HttpRequestEvent();
        httpRequestService.log(event);
        
        verifyNoInteractions(mockLogger);
    }

    @Test
    void shouldNotLogWhenNoModes() {
        when(mockHttpLogging.isEnabled()).thenReturn(true);
        when(mockHttpLogging.getModes()).thenReturn(List.of());
        
        HttpRequestEvent event = new HttpRequestEvent();
        httpRequestService.log(event);
        
        verifyNoInteractions(mockLogger);
    }

    @Test
    void shouldLogWhenEnabledAndModeSupported() {
        when(mockHttpLogging.isEnabled()).thenReturn(true);
        when(mockHttpLogging.getModes()).thenReturn(List.of(AuditProperties.LoggingMode.CONSOLE));
        when(mockLogger.supports(AuditProperties.LoggingMode.CONSOLE)).thenReturn(true);
        
        HttpRequestEvent event = new HttpRequestEvent();
        httpRequestService.log(event);
        
        verify(mockLogger).log(event);
    }

    @Test
    void shouldHandleExceptionInLogger() {
        when(mockHttpLogging.isEnabled()).thenReturn(true);
        when(mockHttpLogging.getModes()).thenReturn(List.of(AuditProperties.LoggingMode.CONSOLE));
        when(mockLogger.supports(AuditProperties.LoggingMode.CONSOLE)).thenReturn(true);
        doThrow(new RuntimeException("Test exception")).when(mockLogger).log(any());
        
        HttpRequestEvent event = new HttpRequestEvent();
        httpRequestService.log(event);
        
        verify(mockLogger).log(event);
    }
}