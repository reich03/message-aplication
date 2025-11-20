package com.messaging.server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String messageType;
    private String content;
    private String fileName;
    private String filePath;
    private Long fileSize;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    private boolean isRead;
    private String senderUsername;
    private String receiverUsername;
    
    public Message() {}
    
    public Message(Long senderId, Long receiverId, String messageType, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageType = messageType;
        this.content = content;
        this.sentAt = LocalDateTime.now();
        this.isRead = false;
    }
    
    // Constructor privado para el Builder
    private Message(Builder builder) {
        this.id = builder.id;
        this.senderId = builder.senderId;
        this.receiverId = builder.receiverId;
        this.messageType = builder.messageType;
        this.content = builder.content;
        this.fileName = builder.fileName;
        this.filePath = builder.filePath;
        this.fileSize = builder.fileSize;
        this.sentAt = builder.sentAt != null ? builder.sentAt : LocalDateTime.now();
        this.createdAt = builder.createdAt;
        this.isRead = builder.isRead;
        this.senderUsername = builder.senderUsername;
        this.receiverUsername = builder.receiverUsername;
    }
    
    // Método estático para obtener un Builder
    public static Builder builder() {
        return new Builder();
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
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
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
    
    public LocalDateTime getCreatedAt() {
        return sentAt; // Alias para compatibilidad
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.sentAt = createdAt;
        this.createdAt = createdAt;
    }
    
    public String getSenderUsername() {
        return senderUsername;
    }
    
    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }
    
    public String getReceiverUsername() {
        return receiverUsername;
    }
    
    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }
    
    @Override
    public String toString() {
        return String.format("Message{id=%d, senderId=%d, receiverId=%d, type='%s', content='%s', sentAt=%s}", 
            id, senderId, receiverId, messageType, content, sentAt);
    }
    
    // Patrón Builder
    public static class Builder {
        private Long id;
        private Long senderId;
        private Long receiverId;
        private String messageType;
        private String content;
        private String fileName;
        private String filePath;
        private Long fileSize;
        private LocalDateTime sentAt;
        private LocalDateTime createdAt;
        private boolean isRead = false;
        private String senderUsername;
        private String receiverUsername;
        
        public Builder id(Long id) {
            this.id = id;
            return this;
        }
        
        public Builder senderId(Long senderId) {
            this.senderId = senderId;
            return this;
        }
        
        public Builder receiverId(Long receiverId) {
            this.receiverId = receiverId;
            return this;
        }
        
        public Builder messageType(String messageType) {
            this.messageType = messageType;
            return this;
        }
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }
        
        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }
        
        public Builder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }
        
        public Builder sentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder isRead(boolean isRead) {
            this.isRead = isRead;
            return this;
        }
        
        public Builder senderUsername(String senderUsername) {
            this.senderUsername = senderUsername;
            return this;
        }
        
        public Builder receiverUsername(String receiverUsername) {
            this.receiverUsername = receiverUsername;
            return this;
        }
        
        public Message build() {
            // Validaciones opcionales
            if (senderId == null) {
                throw new IllegalStateException("senderId es requerido");
            }
            if (receiverId == null) {
                throw new IllegalStateException("receiverId es requerido");
            }
            if (messageType == null || messageType.isEmpty()) {
                throw new IllegalStateException("messageType es requerido");
            }
            
            return new Message(this);
        }
    }
}
