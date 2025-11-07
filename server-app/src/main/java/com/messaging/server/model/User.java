package com.messaging.server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    
    private Long id;
    private String username;
    private String password;
    private String email;
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastConnection;
    private boolean connected;
    private int connectionCount;
    private int maxConnections;
    private int filesSentCount;
    private int maxFilesPerDay;
    
    // Constructores
    public User() {}
    
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
        this.connected = false;
        this.connectionCount = 0;
        this.maxConnections = 3;
        this.filesSentCount = 0;
        this.maxFilesPerDay = 10;
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastConnection() {
        return lastConnection;
    }
    
    public void setLastConnection(LocalDateTime lastConnection) {
        this.lastConnection = lastConnection;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    
    public int getConnectionCount() {
        return connectionCount;
    }
    
    public void setConnectionCount(int connectionCount) {
        this.connectionCount = connectionCount;
    }
    
    public int getMaxConnections() {
        return maxConnections;
    }
    
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
    
    public int getFilesSentCount() {
        return filesSentCount;
    }
    
    public void setFilesSentCount(int filesSentCount) {
        this.filesSentCount = filesSentCount;
    }
    
    public int getMaxFilesPerDay() {
        return maxFilesPerDay;
    }
    
    public void setMaxFilesPerDay(int maxFilesPerDay) {
        this.maxFilesPerDay = maxFilesPerDay;
    }
    
    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', email='%s', status='%s', connected=%s}", 
            id, username, email, status, connected);
    }
}
