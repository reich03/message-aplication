package com.messaging.client.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Modelo de Mensaje para el cliente
 * Representa un mensaje en el sistema
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    
    private int id;
    private int senderId;
    private int receiverId;
    private String messageType; // TEXT, FILE
    private String content;
    private String fileName;
    private String filePath;
    private Long fileSize;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;
    
    private boolean isRead;
    
    // Campos adicionales para la interfaz
    private String senderUsername;
    private String receiverUsername;
    
    // Constructores
    public Message() {}
    
    public Message(int senderId, int receiverId, String messageType, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageType = messageType;
        this.content = content;
        this.sentAt = LocalDateTime.now();
        this.isRead = false;
    }
    
    // Getters y Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getSenderId() {
        return senderId;
    }
    
    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }
    
    public int getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(int receiverId) {
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
    
    // Métodos de utilidad para la interfaz
    public String getFormattedTimestamp() {
        if (sentAt == null) {
            return "";
        }
        return sentAt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
    
    public String getTypeDisplay() {
        if ("FILE".equals(messageType)) {
            return "📎 Archivo";
        } else {
            return "💬 Texto";
        }
    }
    
    public String getContentPreview() {
        if (content == null) {
            return "";
        }
        if (content.length() > 50) {
            return content.substring(0, 47) + "...";
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
    
    @Override
    public String toString() {
        return String.format("Message{id=%d, senderId=%d, receiverId=%d, type='%s', content='%s', sentAt=%s}", 
            id, senderId, receiverId, messageType, content, sentAt);
    }
}
