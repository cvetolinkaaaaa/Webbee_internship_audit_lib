package com.webbee.audit_lib.starter.http.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webbee.audit_lib.starter.http.service.HttpRequestService;
import com.webbee.audit_lib.starter.http.event.HttpRequestEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class HttpRequestInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestInterceptor.class);

    private static final String START_TIME_ATTRIBUTE = "startTime";
    private static final String CORRELATION_ID_ATTRIBUTE = "correlationId";

    private final HttpRequestService httpRequestService;
    private final ObjectMapper objectMapper;

    public HttpRequestInterceptor(HttpRequestService httpRequestService, ObjectMapper objectMapper) {
        this.httpRequestService = httpRequestService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        String correlationId = UUID.randomUUID().toString();

        request.setAttribute(START_TIME_ATTRIBUTE, startTime);
        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           org.springframework.web.servlet.ModelAndView modelAndView) {
        // Можем добавить дополнительную логику здесь, если нужно
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
            String correlationId = (String) request.getAttribute(CORRELATION_ID_ATTRIBUTE);

            if (startTime != null) {
                long executionTime = System.currentTimeMillis() - startTime;

                HttpRequestEvent event = createHttpRequestEvent(request, response, correlationId, executionTime, ex);
                httpRequestService.log(event);
            }
        } catch (Exception e) {
            logger.error("Ошибка при логировании HTTP-запроса", e);
        }
    }

    private HttpRequestEvent createHttpRequestEvent(HttpServletRequest request, HttpServletResponse response,
                                                    String correlationId, long executionTime, Exception ex) {
        HttpRequestEvent event = new HttpRequestEvent();
        event.setTimestamp(LocalDateTime.now());
        event.setRequestType("INCOMING");
        event.setMethod(request.getMethod());
        event.setUrl(request.getRequestURL().toString());
        event.setStatusCode(response.getStatus());
        event.setCorrelationId(correlationId);
        event.setExecutionTime(executionTime);
        event.setUserAgent(request.getHeader("User-Agent"));
        event.setRemoteAddress(getClientIpAddress(request));

        // Получение тела запроса
        if (request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                String requestBody = new String(content, StandardCharsets.UTF_8);
                if (StringUtils.hasText(requestBody)) {
                    event.setRequestBody(requestBody);
                }
            }
        }

        // Получение тела ответа
        if (response instanceof ContentCachingResponseWrapper) {
            ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                String responseBody = new String(content, StandardCharsets.UTF_8);
                if (StringUtils.hasText(responseBody)) {
                    event.setResponseBody(responseBody);
                }
                // Важно: копируем содержимое обратно в response
                try {
                    wrapper.copyBodyToResponse();
                } catch (Exception e) {
                    logger.warn("Не удалось скопировать тело ответа", e);
                }
            }
        }

        return event;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}