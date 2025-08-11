package com.webbee.audit_lib.starter.http.interceptor;

import com.webbee.audit_lib.starter.http.service.HttpRequestService;
import com.webbee.audit_lib.starter.http.event.HttpRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OutgoingHttpRequestInterceptor implements ClientHttpRequestInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(OutgoingHttpRequestInterceptor.class);
    
    private final HttpRequestService httpRequestService;
    
    public OutgoingHttpRequestInterceptor(HttpRequestService httpRequestService) {
        this.httpRequestService = httpRequestService;
    }
    
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, 
                                      ClientHttpRequestExecution execution) throws IOException {
        
        long startTime = System.currentTimeMillis();
        String correlationId = UUID.randomUUID().toString();
        
        ClientHttpResponse response = null;
        try {
            response = execution.execute(request, body);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            HttpRequestEvent event = createOutgoingHttpRequestEvent(
                request, response, body, correlationId, executionTime);
            
            httpRequestService.log(event);
            
            return response;
            
        } catch (Exception e) {

            long executionTime = System.currentTimeMillis() - startTime;
            HttpRequestEvent event = createErrorHttpRequestEvent(
                request, body, correlationId, executionTime, e);
            
            httpRequestService.log(event);
            
            throw e;
        }
    }
    
    private HttpRequestEvent createOutgoingHttpRequestEvent(HttpRequest request, ClientHttpResponse response,
                                                          byte[] requestBody, String correlationId, long executionTime) {
        HttpRequestEvent event = new HttpRequestEvent();
        
        event.setRequestType("Outgoing");
        event.setMethod(request.getMethod().name());
        event.setUrl(request.getURI().toString());
        event.setCorrelationId(correlationId);
        event.setExecutionTime(executionTime);
        
        try {
            event.setStatusCode(response.getStatusCode().value());
        } catch (IOException e) {
            event.setStatusCode(0);
        }
        
        if (requestBody != null && requestBody.length > 0) {
            event.setRequestBody(new String(requestBody, StandardCharsets.UTF_8));
        } else {
            event.setRequestBody("{}");
        }
        
        try {
            String responseBody = readResponseBody(response);
            event.setResponseBody(responseBody);
        } catch (Exception e) {
            event.setResponseBody("{}");
        }
        
        return event;
    }
    
    private HttpRequestEvent createErrorHttpRequestEvent(HttpRequest request, byte[] requestBody,
                                                       String correlationId, long executionTime, Exception error) {
        HttpRequestEvent event = new HttpRequestEvent();
        
        event.setRequestType("Outgoing");
        event.setMethod(request.getMethod().name());
        event.setUrl(request.getURI().toString());
        event.setCorrelationId(correlationId);
        event.setExecutionTime(executionTime);
        event.setStatusCode(0);
        

        if (requestBody != null && requestBody.length > 0) {
            event.setRequestBody(new String(requestBody, StandardCharsets.UTF_8));
        } else {
            event.setRequestBody("{}");
        }

        event.setResponseBody(String.format("{\"error\": \"%s\", \"message\": \"%s\"}", 
            error.getClass().getSimpleName(), error.getMessage()));
        
        return event;
    }
    
    private String readResponseBody(ClientHttpResponse response) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            
            String body = reader.lines().collect(Collectors.joining("\n"));
            return body.isEmpty() ? "{}" : body;
        }
    }
}