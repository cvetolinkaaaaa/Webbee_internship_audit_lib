package com.webbee.audit_lib.starter.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Arrays;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEvent {
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    private String logLevel;
    private String eventType;
    private String correlationId;
    private String methodName;
    private Object[] arguments;
    private Object result;
    private String errorMessage;
    
    public AuditEvent() {
        this.timestamp = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp).append(" ")
          .append(logLevel).append(" ")
          .append(eventType).append(" ")
          .append(correlationId).append(" ")
          .append(methodName);
        
        if (arguments != null && arguments.length > 0) {
            sb.append("\nargs = ").append(Arrays.toString(arguments));
        }
        
        if (result != null) {
            sb.append(" result = ").append(result);
        }
        
        if (errorMessage != null) {
            sb.append(" error = ").append(errorMessage);
        }
        
        return sb.toString();
    }
}