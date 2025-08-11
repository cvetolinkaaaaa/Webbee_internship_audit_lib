package com.webbee.audit_lib.starter.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webbee.audit_lib.starter.core.AuditEvent;
import com.webbee.audit_lib.starter.config.AuditProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "audit.kafka", name = "enabled", havingValue = "true", matchIfMissing = false)
public class KafkaAuditLogger implements AuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAuditLogger.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AuditProperties auditProperties;
    private final ObjectMapper objectMapper;

    public KafkaAuditLogger(@Qualifier("generalAuditKafkaTemplate")
                            KafkaTemplate<String, String> kafkaTemplate,
                            AuditProperties auditProperties,
                            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.auditProperties = auditProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void log(AuditEvent event) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(event);
            String topic = auditProperties.getKafka().getTopic();
            String key = event.getCorrelationId() != null ? event.getCorrelationId() :
                    event.getMethodName() + "_" + System.currentTimeMillis();

            kafkaTemplate.executeInTransaction(operations -> {
                operations.send(topic, key, jsonMessage);
                return true;
            });

        } catch (Exception e) {
            LOGGER.error("Failed to send message to Kafka", e);
        }

    }

    @Override
    public boolean supports(AuditProperties.LoggingMode mode) {
        return mode == AuditProperties.LoggingMode.KAFKA;
    }

}
