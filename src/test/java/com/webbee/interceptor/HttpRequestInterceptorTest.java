package com.webbee.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webbee.audit_lib.starter.http.event.HttpRequestEvent;
import com.webbee.audit_lib.starter.http.interceptor.HttpRequestInterceptor;
import com.webbee.audit_lib.starter.http.service.HttpRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpRequestInterceptorTest {

    @Mock
    private HttpRequestService httpRequestService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object handler;

    private HttpRequestInterceptor interceptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        interceptor = new HttpRequestInterceptor(httpRequestService, objectMapper);
    }

    @Test
    void shouldSetAttributesInPreHandle() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(request).setAttribute(eq("startTime"), any(Long.class));
        verify(request).setAttribute(eq("correlationId"), any(String.class));
    }

    @Test
    void shouldLogRequestInAfterCompletion() {
        long startTime = System.currentTimeMillis() - 100;
        String correlationId = "test-correlation-id";
        
        when(request.getAttribute("startTime")).thenReturn(startTime);
        when(request.getAttribute("correlationId")).thenReturn(correlationId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(request.getHeader("User-Agent")).thenReturn("test-agent");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, handler, null);

        ArgumentCaptor<HttpRequestEvent> eventCaptor = ArgumentCaptor.forClass(HttpRequestEvent.class);
        verify(httpRequestService).log(eventCaptor.capture());
        
        HttpRequestEvent event = eventCaptor.getValue();
        assertEquals("INCOMING", event.getRequestType());
        assertEquals("GET", event.getMethod());
        assertEquals("http://localhost/test", event.getUrl());
        assertEquals(correlationId, event.getCorrelationId());
        assertEquals(200, event.getStatusCode());
        assertEquals("test-agent", event.getUserAgent());
        assertEquals("127.0.0.1", event.getRemoteAddress());
        assertTrue(event.getExecutionTime() >= 0);
    }

    @Test
    void shouldHandleContentCachingRequestWrapper() {
        ContentCachingRequestWrapper requestWrapper = mock(ContentCachingRequestWrapper.class);
        byte[] requestContent = "request body".getBytes();
        
        when(requestWrapper.getContentAsByteArray()).thenReturn(requestContent);
        when(requestWrapper.getAttribute("startTime")).thenReturn(System.currentTimeMillis());
        when(requestWrapper.getAttribute("correlationId")).thenReturn("test-id");
        when(requestWrapper.getMethod()).thenReturn("POST");
        when(requestWrapper.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(requestWrapper.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getStatus()).thenReturn(201);

        interceptor.afterCompletion(requestWrapper, response, handler, null);

        verify(httpRequestService).log(any(HttpRequestEvent.class));
    }

    @Test
    void shouldHandleContentCachingResponseWrapper() throws Exception {
        ContentCachingResponseWrapper responseWrapper = mock(ContentCachingResponseWrapper.class);
        byte[] responseContent = "response body".getBytes();
        
        when(responseWrapper.getContentAsByteArray()).thenReturn(responseContent);
        when(request.getAttribute("startTime")).thenReturn(System.currentTimeMillis());
        when(request.getAttribute("correlationId")).thenReturn("test-id");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(responseWrapper.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, responseWrapper, handler, null);

        verify(httpRequestService).log(any(HttpRequestEvent.class));
        verify(responseWrapper).copyBodyToResponse();
    }

    @Test
    void shouldGetClientIpFromXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(request.getAttribute("startTime")).thenReturn(System.currentTimeMillis());
        when(request.getAttribute("correlationId")).thenReturn("test-id");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, handler, null);

        ArgumentCaptor<HttpRequestEvent> eventCaptor = ArgumentCaptor.forClass(HttpRequestEvent.class);
        verify(httpRequestService).log(eventCaptor.capture());
        assertEquals("192.168.1.1", eventCaptor.getValue().getRemoteAddress());
    }

    @Test
    void shouldGetClientIpFromXRealIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.2");
        when(request.getAttribute("startTime")).thenReturn(System.currentTimeMillis());
        when(request.getAttribute("correlationId")).thenReturn("test-id");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, handler, null);

        ArgumentCaptor<HttpRequestEvent> eventCaptor = ArgumentCaptor.forClass(HttpRequestEvent.class);
        verify(httpRequestService).log(eventCaptor.capture());
        assertEquals("192.168.1.2", eventCaptor.getValue().getRemoteAddress());
    }

    @Test
    void shouldHandleNullStartTime() {
        when(request.getAttribute("startTime")).thenReturn(null);
        
        interceptor.afterCompletion(request, response, handler, null);
        
        verify(httpRequestService, never()).log(any(HttpRequestEvent.class));
    }

    @Test
    void shouldHandleExceptionInAfterCompletion() {
        when(request.getAttribute("startTime")).thenThrow(new RuntimeException("Test exception"));
        
        assertDoesNotThrow(() -> interceptor.afterCompletion(request, response, handler, null));
        
        verify(httpRequestService, never()).log(any(HttpRequestEvent.class));
    }
}