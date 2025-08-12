
package com.webbee.audit_lib.starter.aspect;

import com.webbee.audit_lib.starter.annotation.AuditLog;
import com.webbee.audit_lib.starter.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Spring AOP аспект для автоматического аудита методов.
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Перехватывает выполнение методов с аннотацией @AuditLog.
     */
    @Around("@annotation(com.webbee.audit_lib.starter.annotation.AuditLog)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditLog auditLog = method.getAnnotation(AuditLog.class);

        String methodName = signature.getDeclaringType().getSimpleName() + "." + method.getName();
        Object[] args = joinPoint.getArgs();
        auditService.logStart(methodName, args, auditLog.logLevel());
        try {
            Object result = joinPoint.proceed();
            auditService.logEnd(methodName, result, auditLog.logLevel());
            return result;
        } catch (Throwable throwable) {
            auditService.logError(methodName, throwable, auditLog.logLevel());
            throw throwable;
        }
    }

}
