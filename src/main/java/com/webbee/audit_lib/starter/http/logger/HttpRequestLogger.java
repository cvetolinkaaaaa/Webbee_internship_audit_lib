package com.webbee.audit_lib.starter.http.logger;

import com.webbee.audit_lib.starter.config.AuditProperties;
import com.webbee.audit_lib.starter.http.event.HttpRequestEvent;

public interface HttpRequestLogger {
    
    void log(HttpRequestEvent event);
    
    boolean supports(AuditProperties.LoggingMode mode);
}