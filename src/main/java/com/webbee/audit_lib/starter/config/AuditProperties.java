package com.webbee.audit_lib.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "audit")
public class AuditProperties {

    private boolean enabled = true;

    /**
     * Способы логирования
     */
    private List<LoggingMode> modes = new ArrayList<>();

    /**
     * Настройки для консольного логирования
     */
    private ConsoleLogging console = new ConsoleLogging();

    /**
     * Настройки для файлового логирования
     */
    private FileLogging file = new FileLogging();

    /**
     * Настройки для Kafka
     */
    private KafkaLogging kafka = new KafkaLogging();

    /**
     * Настройки для HTTP логирования
     */
    private HttpLogging http = new HttpLogging();

    public enum LoggingMode {
        CONSOLE, FILE, KAFKA
    }

    @Data
    public static class ConsoleLogging {

        private boolean enabled = true;

    }

    @Data
    public static class FileLogging {

        private boolean enabled = false;
        private String path = "logs/audit.log";
        private String maxFileSize = "1MB";
        private int maxFiles = 10;

    }

    @Data
    public static class KafkaLogging {
        private boolean enabled = false;
        private String topic = "general-audit";
        private String bootstrapServers = "localhost:9092";
    }

    @Data
    public static class HttpLogging {
        private boolean enabled = false;
        private List<LoggingMode> modes = new ArrayList<>();
        private ConsoleLogging console = new ConsoleLogging();
        private FileLogging file = new FileLogging();
        private KafkaLogging kafka = new KafkaLogging();

        @Data
        public static class FileLogging {

            private boolean enabled = false;
            private String path = "logs/http-requests.log";
            private String maxFileSize = "1MB";
            private int maxFiles = 10;

        }

        @Data
        public static class KafkaLogging {
            private boolean enabled = false;
            private String topic = "http-audit";
            private String bootstrapServers = "localhost:9092";

        }
    }

}
