package com.webbee.audit_lib.starter.logger;

import com.webbee.audit_lib.starter.core.AuditEvent;
import com.webbee.audit_lib.starter.config.AuditProperties;

public interface AuditLogger {
    
    void log(AuditEvent event);
    
    boolean supports(AuditProperties.LoggingMode mode);
}