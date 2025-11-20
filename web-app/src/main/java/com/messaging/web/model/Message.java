package com.messaging.web.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Entidad Mensaje para la aplicación web
 * Representa un mensaje en el sistema
 */
@Entity
@Table(name = "messages")
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "El ID del remitente es obligatorio")
    @Column(name = "sender_id", nullable = false)
    private Long senderId;
    
    @NotNull(message = "El ID del destinatario es obligatorio")
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "file_path")
    private String filePath;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();
    
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;
    
    // Relaciones - EAGER para evitar LazyInitializationException en vistas
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", insertable = false, updatable = false)
    private User sender;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_id", insertable = false, updatable = false)
    private User receiver;
    
    // Constructores
    public Message() {}
    
    public Message(Long senderId, Long receiverId, MessageType messageType, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageType = messageType;
        this.content = content;
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getSenderId() {
        return senderId;
    }
    
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }
    
    public Long getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean read) {
        isRead = read;
    }
    
    public User getSender() {
        return sender;
    }
    
    public void setSender(User sender) {
        this.sender = sender;
    }
    
    public User getReceiver() {
        return receiver;
    }
    
    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }
    
    // Métodos de utilidad
    public String getTypeDisplay() {
        return messageType.getDisplayName();
    }
    
    public String getFormattedSentAt() {
        return sentAt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
    
    public String getContentPreview() {
        if (content == null) {
            return "";
        }
        if (content.length() > 100) {
            return content.substring(0, 97) + "...";
        }
        return content;
    }
    
    public String getFileSizeDisplay() {
        if (fileSize == null || fileSize == 0) {
            return "";
        }
        
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
    
    public String getSenderUsername() {
        return sender != null ? sender.getUsername() : "Usuario " + senderId;
    }
    
    public String getReceiverUsername() {
        return receiver != null ? receiver.getUsername() : "Usuario " + receiverId;
    }
    
    @Override
    public String toString() {
        return String.format("Message{id=%d, senderId=%d, receiverId=%d, type='%s', content='%s', sentAt=%s}", 
            id, senderId, receiverId, messageType, content, sentAt);
    }
    
    // Enum para el tipo de mensaje
    public enum MessageType {
        TEXT("Texto"),
        FILE("Archivo"),
        IMAGE("Imagen");
        
        private final String displayName;
        
        MessageType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
