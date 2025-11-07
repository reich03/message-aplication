package com.messaging.web.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Entidad Usuario para la aplicación web
 * Representa un usuario del sistema de mensajería
 */
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
    @Column(unique = true, nullable = false)
    private String username;
    
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    @Column(nullable = false)
    private String password;
    
    @Email(message = "El email debe ser válido")
    @NotBlank(message = "El email es obligatorio")
    @Column(unique = true, nullable = false)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.PENDING;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "last_connection")
    private LocalDateTime lastConnection;
    
    @Column(name = "connected", nullable = false)
    private boolean connected = false;
    
    @Column(name = "connection_count", nullable = false)
    private int connectionCount = 0;
    
    @Column(name = "max_connections", nullable = false)
    private int maxConnections = 3;
    
    @Column(name = "files_sent_count", nullable = false)
    private int filesSentCount = 0;
    
    @Column(name = "max_files_per_day", nullable = false)
    private int maxFilesPerDay = 10;
    
    // Constructores
    public User() {}
    
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
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
    
    public UserStatus getStatus() {
        return status;
    }
    
    public void setStatus(UserStatus status) {
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
    
    // Métodos de utilidad
    public String getStatusDisplay() {
        return status.getDisplayName();
    }
    
    public String getFormattedCreatedAt() {
        return createdAt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
    
    public String getFormattedLastConnection() {
        if (lastConnection == null) {
            return "Nunca";
        }
        return lastConnection.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
    
    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', email='%s', status='%s', connected=%s}", 
            id, username, email, status, connected);
    }
    
    // Enum para el estado del usuario
    public enum UserStatus {
        PENDING("Pendiente"),
        APPROVED("Aprobado"),
        REJECTED("Rechazado");
        
        private final String displayName;
        
        UserStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
