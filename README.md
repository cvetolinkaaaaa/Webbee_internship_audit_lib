# Webbee Audit Library
Библиотека для автоматического аудита выполнения методов и HTTP запросов в Spring Boot приложениях с поддержкой множественных способов логирования.

- **Аудит методов**: Автоматическое логирование выполнения методов через аннотацию `@AuditLog`
- **HTTP аудит**: Автоматический перехват и логирование входящих и исходящих HTTP запросов
- **Множественные логгеры**: Консоль, файл, Apache Kafka

## Установка
### Добавьте зависимость в ваш `pom.xml`
``` xml
<dependency>
    <groupId>com.webbee</groupId>
    <artifactId>webbee-internship-audit-lib</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```
