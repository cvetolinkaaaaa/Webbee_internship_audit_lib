package com.webbee.audit_lib.starter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    
    /**
     * Уровень логирования
     */
    LogLevel logLevel() default LogLevel.DEBUG;
    
    /**
     * Перечисление уровней логирования
     */
    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}