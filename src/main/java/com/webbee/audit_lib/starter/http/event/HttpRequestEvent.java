
package com.webbee.audit_lib.starter.http.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpRequestEvent {
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    private String requestType;
    private String method;
    private int statusCode;
    private String url;
    private String requestBody;
    private String responseBody;
    private String correlationId;
    private Long executionTime;
    private String userAgent;
    private String remoteAddress;
    
    public HttpRequestEvent() {
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("%s %s %s %d %s RequestBody = %s ResponseBody = %s",
            timestamp.toString().replace("T", " "),
            requestType,
            method,
            statusCode,
            url,
            Objects.toString(requestBody, "{}"),
            Objects.toString(responseBody, "{}")
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpRequestEvent that = (HttpRequestEvent) o;
        return statusCode == that.statusCode &&
                Objects.equals(correlationId, that.correlationId) &&
                Objects.equals(requestType, that.requestType) &&
                Objects.equals(method, that.method) &&
                Objects.equals(url, that.url);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(requestType, method, statusCode, url, correlationId);
    }
}