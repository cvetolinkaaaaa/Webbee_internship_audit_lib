package com.webbee.audit_lib.starter.http.service;

import com.webbee.audit_lib.starter.config.AuditProperties;
import com.webbee.audit_lib.starter.http.event.HttpRequestEvent;
import com.webbee.audit_lib.starter.http.logger.HttpRequestLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HttpRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestService.class);

    private final List<HttpRequestLogger> httpRequestLoggers;
    private final AuditProperties auditProperties;

    public HttpRequestService(List<HttpRequestLogger> httpRequestLoggers, AuditProperties auditProperties) {
        this.httpRequestLoggers = httpRequestLoggers;
        this.auditProperties = auditProperties;
    }

    public void log(HttpRequestEvent event) {

        if (!auditProperties.getHttp().isEnabled()) {
            return;
        }

        List<AuditProperties.LoggingMode> modes = auditProperties.getHttp().getModes();

        if (modes.isEmpty()) {
            return;
        }

        for (AuditProperties.LoggingMode mode : modes) {

            httpRequestLoggers.stream()
                    .filter(httpLogger -> httpLogger.supports(mode))
                    .forEach(httpLogger -> {
                        try {
                            httpLogger.log(event);
                        } catch (Exception e) {
                            LOGGER.error("Failed to log HTTP request event with mode: {} using logger: {}",
                                    mode, httpLogger.getClass().getSimpleName(), e);
                        }
                    });
        }
    }

}
