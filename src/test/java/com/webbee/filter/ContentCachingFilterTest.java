package com.webbee.filter;

import com.webbee.audit_lib.starter.http.filter.ContentCachingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContentCachingFilterTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private FilterChain mockFilterChain;

    @Mock
    private FilterConfig mockFilterConfig;

    private final ContentCachingFilter filter = new ContentCachingFilter();

    @Test
    void shouldProcessFilterChain() throws IOException, ServletException {
        filter.doFilter(mockRequest, mockResponse, mockFilterChain);
        
        verify(mockFilterChain).doFilter(any(), any());
    }

    @Test
    void shouldInitializeFilter() throws ServletException {
        filter.init(mockFilterConfig);
    }

    @Test
    void shouldDestroyFilter() {
        filter.destroy();
    }
}