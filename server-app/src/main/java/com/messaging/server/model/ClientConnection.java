package com.messaging.server.model;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ClientConnection {
    
    private final String connectionId;
    private final Long userId;
    private final String username;
    private final Socket socket;
    private final String clientIp;
    private final LocalDateTime connectedAt;
    private final int maxConnections;
    private final int maxFilesPerDay;
    
    private final AtomicInteger messagesCount;
    private final AtomicInteger filesSentCount;
    private final AtomicLong lastActivity;
    
    public ClientConnection(String connectionId, Long userId, String username, 
                          Socket socket, String clientIp, int maxConnections, int maxFilesPerDay) {
        this.connectionId = connectionId;
        this.userId = userId;
        this.username = username;
        this.socket = socket;
        this.clientIp = clientIp;
        this.connectedAt = LocalDateTime.now();
        this.maxConnections = maxConnections;
        this.maxFilesPerDay = maxFilesPerDay;
        
        this.messagesCount = new AtomicInteger(0);
        this.filesSentCount = new AtomicInteger(0);
        this.lastActivity = new AtomicLong(System.currentTimeMillis());
    }
    
    // Getters
    public String getConnectionId() {
        return connectionId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public Socket getSocket() {
        return socket;
    }
    
    public String getClientIp() {
        return clientIp;
    }
    
    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }
    
    public int getMaxConnections() {
        return maxConnections;
    }
    
    public int getMaxFilesPerDay() {
        return maxFilesPerDay;
    }
    
    public int getMessagesCount() {
        return messagesCount.get();
    }
    
    public int getFilesSentCount() {
        return filesSentCount.get();
    }
    
    public long getLastActivity() {
        return lastActivity.get();
    }
    
    // Métodos de actualización
    public void incrementMessagesCount() {
        messagesCount.incrementAndGet();
        updateLastActivity();
    }
    
    public void incrementFilesSentCount() {
        filesSentCount.incrementAndGet();
        updateLastActivity();
    }
    
    public void updateLastActivity() {
        lastActivity.set(System.currentTimeMillis());
    }
    
    public boolean canSendFile() {
        return filesSentCount.get() < maxFilesPerDay;
    }
    
    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }
    
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
    
    @Override
    public String toString() {
        return String.format("ClientConnection{id='%s', user=%d, username='%s', ip='%s', connected=%s, messages=%d, files=%d}",
            connectionId, userId, username, clientIp, connectedAt, messagesCount.get(), filesSentCount.get());
    }
}
