package com.webbee.event;

import com.webbee.audit_lib.starter.http.event.HttpRequestEvent;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRequestEventTest {

    @Test
    void shouldCreateHttpRequestEventWithTimestamp() {
        HttpRequestEvent event = new HttpRequestEvent();
        
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getTimestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    void shouldGenerateToStringCorrectly() {
        HttpRequestEvent event = new HttpRequestEvent();
        event.setRequestType("INCOMING");
        event.setMethod("GET");
        event.setStatusCode(200);
        event.setUrl("http://localhost:8080/test");
        event.setRequestBody("{\"test\": \"data\"}");
        event.setResponseBody("{\"result\": \"success\"}");
        
        String result = event.toString();
        
        assertThat(result).contains("INCOMING", "GET", "200", "http://localhost:8080/test");
        assertThat(result).contains("{\"test\": \"data\"}");
        assertThat(result).contains("{\"result\": \"success\"}");
    }

    @Test
    void shouldHandleNullValuesInToString() {
        HttpRequestEvent event = new HttpRequestEvent();
        event.setRequestType("INCOMING");
        event.setMethod("POST");
        event.setStatusCode(404);
        event.setUrl("http://localhost:8080/notfound");
        
        String result = event.toString();
        
        assertThat(result).contains("INCOMING", "POST", "404");
        assertThat(result).contains("{}");
    }

    @Test
    void shouldImplementEqualsAndHashCodeCorrectly() {
        HttpRequestEvent event1 = createTestEvent();
        HttpRequestEvent event2 = createTestEvent();
        HttpRequestEvent event3 = new HttpRequestEvent();
        event3.setRequestType("DIFFERENT");
        
        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    private HttpRequestEvent createTestEvent() {
        HttpRequestEvent event = new HttpRequestEvent();
        event.setRequestType("INCOMING");
        event.setMethod("GET");
        event.setStatusCode(200);
        event.setUrl("http://test.com");
        event.setCorrelationId("test-id");
        return event;
    }
}