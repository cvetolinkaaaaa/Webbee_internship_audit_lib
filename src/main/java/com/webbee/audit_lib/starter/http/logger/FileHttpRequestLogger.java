
package com.webbee.audit_lib.starter.http.logger;

import com.webbee.audit_lib.starter.config.AuditProperties;
import com.webbee.audit_lib.starter.http.event.HttpRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Component
@ConditionalOnProperty(prefix = "audit.http.file", name = "enabled", havingValue = "true", matchIfMissing = false)
public class FileHttpRequestLogger implements HttpRequestLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileHttpRequestLogger.class);

    private final AuditProperties auditProperties;

    public FileHttpRequestLogger(AuditProperties auditProperties) {
        this.auditProperties = auditProperties;
        ensureDirectoryExists();
    }

    @Override
    public void log(HttpRequestEvent event) {
        try {
            String logEntry = event.toString() + System.lineSeparator();
            writeToFile(logEntry);
        } catch (Exception e) {
            LOGGER.error("Failed to write HTTP request event to file: {}",
                auditProperties.getHttp().getFile().getPath(), e);
        }
    }

    private void writeToFile(String logEntry) throws IOException {
        Path filePath = Paths.get(auditProperties.getHttp().getFile().getPath());
        if (Files.exists(filePath) && shouldRotate(filePath)) {
            rotateLogFile(filePath);
        }
        Files.write(filePath, logEntry.getBytes(),
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private boolean shouldRotate(Path filePath) throws IOException {
        long maxSizeBytes = parseSize(auditProperties.getHttp().getFile().getMaxFileSize());
        return Files.size(filePath) >= maxSizeBytes;
    }

    private void rotateLogFile(Path currentFile) throws IOException {
        int maxFiles = auditProperties.getHttp().getFile().getMaxFiles();
        String baseName = currentFile.toString();
        for (int i = maxFiles - 1; i > 0; i--) {
            Path oldFile = Paths.get(baseName + "." + i);
            Path newFile = Paths.get(baseName + "." + (i + 1));
            if (Files.exists(oldFile)) {
                if (i == maxFiles - 1) {
                    Files.deleteIfExists(newFile);
                }
                Files.move(oldFile, newFile);
            }
        }
        Path rotatedFile = Paths.get(baseName + ".1");
        Files.move(currentFile, rotatedFile);
    }

    private long parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty()) {
            return 1024 * 1024;
        }
        String size = sizeStr.toUpperCase();
        long multiplier = 1;
        if (size.endsWith("KB")) {
            multiplier = 1024;
            size = size.substring(0, size.length() - 2);
        } else if (size.endsWith("MB")) {
            multiplier = 1024 * 1024;
            size = size.substring(0, size.length() - 2);
        } else if (size.endsWith("GB")) {
            multiplier = 1024 * 1024 * 1024;
            size = size.substring(0, size.length() - 2);
        }
        try {
            return Long.parseLong(size.trim()) * multiplier;
        } catch (NumberFormatException e) {
            return 1024 * 1024;
        }
    }

    private void ensureDirectoryExists() {
        try {
            Path filePath = Paths.get(auditProperties.getHttp().getFile().getPath());
            Path directory = filePath.getParent();
            if (directory != null && !Files.exists(directory)) {
                Files.createDirectories(directory);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create HTTP request log directory", e);
        }
    }

    @Override
    public boolean supports(AuditProperties.LoggingMode mode) {
        return mode == AuditProperties.LoggingMode.FILE;
    }

}
