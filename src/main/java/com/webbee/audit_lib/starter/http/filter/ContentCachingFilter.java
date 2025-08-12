
package com.webbee.audit_lib.starter.http.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Servlet фильтр для кэширования содержимого HTTP запросов и ответов.
 */
public class ContentCachingFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentCachingFilter.class);

    /**
     * Выполняет фильтрацию HTTP запроса.
     */
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

    /**
     * Инициализирует фильтр.
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.info("ContentCachingFilter initialized via FilterRegistrationBean");
    }

    /**
     * Уничтожает фильтр.
     */
    @Override
    public void destroy() {
        LOGGER.info("ContentCachingFilter destroyed");
    }

}
