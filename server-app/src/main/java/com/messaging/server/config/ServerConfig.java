package com.messaging.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuración del servidor usando patrón Singleton
 * Aplica principio de responsabilidad única (SRP)
 */
public class ServerConfig {
    
    private static volatile ServerConfig instance;
    private final Properties properties;
    
    private ServerConfig() {
        this.properties = new Properties();
        loadConfiguration();
    }
    
    public static ServerConfig getInstance() {
        if (instance == null) {
            synchronized (ServerConfig.class) {
                if (instance == null) {
                    instance = new ServerConfig();
                }
            }
        }
        return instance;
    }
    
    private void loadConfiguration() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("server.properties")) {
            if (input != null) {
                properties.load(input);
            } else {
                // Configuración por defecto
                setDefaultProperties();
            }
        } catch (IOException e) {
            System.err.println("Error cargando configuración: " + e.getMessage());
            setDefaultProperties();
        }
    }
    
    private void setDefaultProperties() {
        properties.setProperty("server.port", "9999");
        properties.setProperty("server.max.connections", "100");
        properties.setProperty("server.max.threads", "50");
        properties.setProperty("server.connection.timeout", "300000"); // 5 minutos
        properties.setProperty("server.max.file.size", "10485760"); // 10MB
        properties.setProperty("server.files.directory", "./files");
    }
    
    public int getPort() {
        return Integer.parseInt(properties.getProperty("server.port", "9999"));
    }
    
    public int getMaxConnections() {
        return Integer.parseInt(properties.getProperty("server.max.connections", "100"));
    }
    
    public int getMaxThreads() {
        return Integer.parseInt(properties.getProperty("server.max.threads", "50"));
    }
    
    public int getConnectionTimeout() {
        return Integer.parseInt(properties.getProperty("server.connection.timeout", "300000"));
    }
    
    public long getMaxFileSize() {
        return Long.parseLong(properties.getProperty("server.max.file.size", "10485760"));
    }
    
    public String getFilesDirectory() {
        return properties.getProperty("server.files.directory", "./files");
    }
}
