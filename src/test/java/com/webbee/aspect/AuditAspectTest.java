package com.webbee.aspect;

import com.webbee.audit_lib.starter.aspect.AuditAspect;
import com.webbee.audit_lib.starter.annotation.AuditLog;
import com.webbee.audit_lib.starter.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;

import static org.mockito.Mockito.*;

class AuditAspectTest {

    @Mock
    private AuditService auditService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private AuditAspect auditAspect;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auditAspect = new AuditAspect(auditService);
    }

    @Test
    void shouldHandleDifferentLogLevel() throws Throwable {
        Method errorMethod = TestService.class.getMethod("errorMethod");
        Object[] args = {};

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(errorMethod);
        when(methodSignature.getDeclaringType()).thenReturn((Class) TestService.class);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("success");

        auditAspect.auditMethod(joinPoint);

        verify(auditService).logStart("TestService.errorMethod", args, AuditLog.LogLevel.ERROR);
        verify(auditService).logEnd("TestService.errorMethod", "success", AuditLog.LogLevel.ERROR);
    }

    static class TestService {
        @AuditLog
        public String testMethod(String param) {
            return "test";
        }

        @AuditLog(logLevel = AuditLog.LogLevel.ERROR)
        public String errorMethod() {
            return "error";
        }
    }
}