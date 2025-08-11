
package com.webbee.audit_lib.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webbee.audit_lib.starter.http.filter.ContentCachingFilter;
import com.webbee.audit_lib.starter.http.interceptor.HttpRequestInterceptor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(AuditProperties.class)
@ComponentScan(basePackages = "com.webbee.audit_lib.starter")
@ConditionalOnProperty(prefix = "audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditAutoConfiguration implements WebMvcConfigurer {

    private final HttpRequestInterceptor httpRequestInterceptor;
    private final AuditProperties auditProperties;

    public AuditAutoConfiguration(@Lazy HttpRequestInterceptor httpRequestInterceptor,
                                  AuditProperties auditProperties) {
        this.httpRequestInterceptor = httpRequestInterceptor;
        this.auditProperties = auditProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (auditProperties.getHttp().isEnabled()) {
            registry.addInterceptor(httpRequestInterceptor);
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "audit.http", name = "enabled", havingValue = "true", matchIfMissing = false)
    public FilterRegistrationBean<ContentCachingFilter> httpRequestCachingFilter() {
        FilterRegistrationBean<ContentCachingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ContentCachingFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(-2147483648);
        registration.setName("httpRequestCachingFilter");

        return registration;
    }

    @Bean
    @ConditionalOnProperty(prefix = "audit.http.kafka", name = "enabled", havingValue = "true")
    public ProducerFactory<String, String> httpAuditKafkaProducerFactory(AuditProperties auditProperties) {
        Map<String, Object> configProps = new HashMap<>();

        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                auditProperties.getHttp().getKafka().getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "http-audit-producer");
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);

        return new DefaultKafkaProducerFactory<>(configProps);

    }

    @Bean
    @ConditionalOnProperty(prefix = "audit.http.kafka", name = "enabled", havingValue = "true")
    public KafkaTemplate<String, String> httpAuditKafkaTemplate(
            @Qualifier("httpAuditKafkaProducerFactory") ProducerFactory<String, String> producerFactory) {

        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);
        return template;
    }

    @Bean
    @ConditionalOnProperty(prefix = "audit.kafka", name = "enabled", havingValue = "true")
    public ProducerFactory<String, String> generalAuditKafkaProducerFactory(AuditProperties auditProperties) {
        Map<String, Object> configProps = new HashMap<>();

        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                auditProperties.getHttp().getKafka().getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "general-audit-producer");
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);

        return new DefaultKafkaProducerFactory<>(configProps);

    }

    @Bean
    @ConditionalOnProperty(prefix = "audit.kafka", name = "enabled", havingValue = "true")
    public KafkaTemplate<String, String> generalAuditKafkaTemplate(
            @Qualifier("generalAuditKafkaProducerFactory") ProducerFactory<String, String> producerFactory) {

        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);
        return template;
    }

    @Bean
    @ConditionalOnProperty(prefix = "audit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ObjectMapper auditObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

}
