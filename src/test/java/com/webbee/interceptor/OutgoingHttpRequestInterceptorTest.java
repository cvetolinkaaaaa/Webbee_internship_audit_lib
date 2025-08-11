package com.webbee.interceptor;

import com.webbee.audit_lib.starter.http.event.HttpRequestEvent;
import com.webbee.audit_lib.starter.http.interceptor.OutgoingHttpRequestInterceptor;
import com.webbee.audit_lib.starter.http.service.HttpRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutgoingHttpRequestInterceptorTest {

    @Mock
    private HttpRequestService httpRequestService;

    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse response;

    private OutgoingHttpRequestInterceptor interceptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        interceptor = new OutgoingHttpRequestInterceptor(httpRequestService);
    }

    @Test
    void shouldLogSuccessfulRequest() throws IOException {
        byte[] requestBody = "request body".getBytes();
        String responseBody = "response body";
        
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
        when(execution.execute(request, requestBody)).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(response.getBody()).thenReturn(new ByteArrayInputStream(responseBody.getBytes()));

        ClientHttpResponse result = interceptor.intercept(request, requestBody, execution);

        assertEquals(response, result);
        
        ArgumentCaptor<HttpRequestEvent> eventCaptor = ArgumentCaptor.forClass(HttpRequestEvent.class);
        verify(httpRequestService).log(eventCaptor.capture());
        
        HttpRequestEvent event = eventCaptor.getValue();
        assertEquals("Outgoing", event.getRequestType());
        assertEquals("POST", event.getMethod());
        assertEquals("http://example.com/api", event.getUrl());
        assertEquals(200, event.getStatusCode());
        assertEquals("request body", event.getRequestBody());
        assertEquals(responseBody, event.getResponseBody());
        assertNotNull(event.getCorrelationId());
        assertTrue(event.getExecutionTime() >= 0);
    }

    @Test
    void shouldLogRequestWithException() throws IOException {
        byte[] requestBody = "request body".getBytes();
        IOException exception = new IOException("Connection failed");
        
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
        when(execution.execute(request, requestBody)).thenThrow(exception);

        assertThrows(IOException.class, () -> interceptor.intercept(request, requestBody, execution));
        
        ArgumentCaptor<HttpRequestEvent> eventCaptor = ArgumentCaptor.forClass(HttpRequestEvent.class);
        verify(httpRequestService).log(eventCaptor.capture());
        
        HttpRequestEvent event = eventCaptor.getValue();
        assertEquals("Outgoing", event.getRequestType());
        assertEquals("GET", event.getMethod());
        assertEquals("http://example.com/api", event.getUrl());
        assertEquals(0, event.getStatusCode());
        assertEquals("request body", event.getRequestBody());
        assertTrue(event.getResponseBody().contains("IOException"));
        assertTrue(event.getResponseBody().contains("Connection failed"));
    }

    @Test
    void shouldHandleEmptyRequestBody() throws IOException {
        byte[] requestBody = new byte[0];
        
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
        when(execution.execute(request, requestBody)).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(response.getBody()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        interceptor.intercept(request, requestBody, execution);

        ArgumentCaptor<HttpRequestEvent> eventCaptor = ArgumentCaptor.forClass(HttpRequestEvent.class);
        verify(httpRequestService).log(eventCaptor.capture());
        assertEquals("{}", eventCaptor.getValue().getRequestBody());
    }

    @Test
    void shouldHandleNullRequestBody() throws IOException {
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
        when(execution.execute(request, null)).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(response.getBody()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        interceptor.intercept(request, null, execution);

        ArgumentCaptor<HttpRequestEvent> eventCaptor = ArgumentCaptor.forClass(HttpRequestEvent.class);
        verify(httpRequestService).log(eventCaptor.capture());
        assertEquals("{}", eventCaptor.getValue().getRequestBody());
    }

    @Test
    void shouldHandleEmptyResponseBody() throws IOException {
        byte[] requestBody = "request".getBytes();
        
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
        when(execution.execute(request, requestBody)).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.NO_CONTENT);
        when(response.getBody()).thenReturn(new ByteArrayInputStream("".getBytes()));

        interceptor.intercept(request, requestBody, execution);

        ArgumentCaptor<HttpRequestEvent> eventCaptor = ArgumentCaptor.forClass(HttpRequestEvent.class);
        verify(httpRequestService).log(eventCaptor.capture());
        assertEquals("{}", eventCaptor.getValue().getResponseBody());
    }

    @Test
    void shouldHandleStatusCodeException() throws IOException {
        byte[] requestBody = "request".getBytes();
        
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
        when(execution.execute(request, requestBody)).thenReturn(response);
        when(response.getStatusCode()).thenThrow(new IOException("Status code error"));
        when(response.getBody()).thenReturn(new ByteArrayInputStream("response".getBytes()));

        interceptor.intercept(request, requestBody, execution);

        ArgumentCaptor<HttpRequestEvent> eventCaptor = ArgumentCaptor.forClass(HttpRequestEvent.class);
        verify(httpRequestService).log(eventCaptor.capture());
        assertEquals(0, eventCaptor.getValue().getStatusCode());
    }
}