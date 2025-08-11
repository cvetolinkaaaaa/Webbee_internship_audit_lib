
package com.webbee.audit_lib.starter.http.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

public class ContentCachingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentCachingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper((HttpServletRequest) request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper((HttpServletResponse) response);

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("ContentCachingFilter initialized via FilterRegistrationBean");
    }
    
    @Override
    public void destroy() {
        logger.info("ContentCachingFilter destroyed");
    }
}