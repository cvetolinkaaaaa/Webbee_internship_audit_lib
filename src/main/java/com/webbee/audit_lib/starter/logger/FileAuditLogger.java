package com.webbee.audit_lib.starter.logger;

import com.webbee.audit_lib.starter.core.AuditEvent;
import com.webbee.audit_lib.starter.config.AuditProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@ConditionalOnProperty(prefix = "audit.file", name = "enabled", havingValue = "true", matchIfMissing = false)
public class FileAuditLogger implements AuditLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(FileAuditLogger.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private final AuditProperties auditProperties;
    
    public FileAuditLogger(AuditProperties auditProperties) {
        this.auditProperties = auditProperties;
        ensureDirectoryExists();
    }
    
    @Override
    public void log(AuditEvent event) {
        try {
            String logEntry = String.format("%s AUDIT_FILE - %s%n", 
                LocalDateTime.now().format(formatter), 
                event.toString());
            
            writeToFile(logEntry);
        } catch (Exception e) {
            logger.error("Failed to write audit event to file: {}", auditProperties.getFile().getPath(), e);
        }
    }
    
    private void writeToFile(String logEntry) throws IOException {
        String filePath = auditProperties.getFile().getPath();
        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.write(logEntry);
            writer.flush();
        }
    }
    
    private void ensureDirectoryExists() {
        try {
            Path filePath = Paths.get(auditProperties.getFile().getPath());
            Path directory = filePath.getParent();
            if (directory != null && !Files.exists(directory)) {
                Files.createDirectories(directory);
            }
        } catch (IOException e) {
            logger.error("Failed to create audit log directory", e);
        }
    }
    
    @Override
    public boolean supports(AuditProperties.LoggingMode mode) {
        return mode == AuditProperties.LoggingMode.FILE;
    }
}