package com.messaging.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio de logging centralizado
 * Implementa patrón Singleton y maneja logging en consola y archivo
 */
public class LoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);
    private static volatile LoggingService instance;
    
    private final String logFilePath;
    private final AtomicBoolean fileLoggingEnabled;
    private final DateTimeFormatter formatter;
    
    private LoggingService() {
        this.logFilePath = "./logs/server.log";
        this.fileLoggingEnabled = new AtomicBoolean(true);
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Crear directorio de logs si no existe
        try {
            java.io.File logDir = new java.io.File("./logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
        } catch (Exception e) {
            System.err.println("Error creando directorio de logs: " + e.getMessage());
        }
    }
    
    public static LoggingService getInstance() {
        if (instance == null) {
            synchronized (LoggingService.class) {
                if (instance == null) {
                    instance = new LoggingService();
                }
            }
        }
        return instance;
    }
    
    public void info(String message) {
        log("INFO", message);
    }
    
    public void warn(String message) {
        log("WARN", message);
    }
    
    public void error(String message) {
        log("ERROR", message);
    }
    
    public void debug(String message) {
        log("DEBUG", message);
    }
    
    private void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);
        
        // Log en consola
        System.out.println(logEntry);
        
        // Log en archivo
        if (fileLoggingEnabled.get()) {
            writeToFile(logEntry);
        }
        
        // Log usando SLF4J
        switch (level) {
            case "INFO":
                logger.info(message);
                break;
            case "WARN":
                logger.warn(message);
                break;
            case "ERROR":
                logger.error(message);
                break;
            case "DEBUG":
                logger.debug(message);
                break;
        }
    }
    
    private void writeToFile(String logEntry) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
            writer.println(logEntry);
        } catch (IOException e) {
            System.err.println("Error escribiendo al archivo de log: " + e.getMessage());
        }
    }
    
    public void setFileLoggingEnabled(boolean enabled) {
        fileLoggingEnabled.set(enabled);
    }
    
    public boolean isFileLoggingEnabled() {
        return fileLoggingEnabled.get();
    }
    
    public String getLogFilePath() {
        return logFilePath;
    }
}
