package com.messaging.web.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "connection_history")
public class ConnectionHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "client_ip", nullable = false)
    private String clientIp;
    
    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;
    
    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;
    
    @Column(name = "messages_sent", nullable = false)
    private Integer messagesSent = 0;
    
    // Relación opcional
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    // Constructores
    public ConnectionHistory() {}
    
    public ConnectionHistory(Long userId, String clientIp, LocalDateTime connectedAt) {
        this.userId = userId;
        this.clientIp = clientIp;
        this.connectedAt = connectedAt;
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getClientIp() {
        return clientIp;
    }
    
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
    
    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }
    
    public void setConnectedAt(LocalDateTime connectedAt) {
        this.connectedAt = connectedAt;
    }
    
    public LocalDateTime getDisconnectedAt() {
        return disconnectedAt;
    }
    
    public void setDisconnectedAt(LocalDateTime disconnectedAt) {
        this.disconnectedAt = disconnectedAt;
    }
    
    public Integer getMessagesSent() {
        return messagesSent;
    }
    
    public void setMessagesSent(Integer messagesSent) {
        this.messagesSent = messagesSent;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
}