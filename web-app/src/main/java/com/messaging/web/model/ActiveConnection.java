package com.messaging.web.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "active_connections")
public class ActiveConnection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "client_ip", nullable = false)
    private String clientIp;
    
    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt = LocalDateTime.now();
    
    @Column(name = "messages_count", nullable = false)
    private Integer messagesCount = 0;
    
    // Relación opcional
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    // Constructores
    public ActiveConnection() {}
    
    public ActiveConnection(Long userId, String clientIp) {
        this.userId = userId;
        this.clientIp = clientIp;
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
    
    public Integer getMessagesCount() {
        return messagesCount;
    }
    
    public void setMessagesCount(Integer messagesCount) {
        this.messagesCount = messagesCount;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
}