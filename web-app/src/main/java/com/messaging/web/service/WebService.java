package com.messaging.web.service;

import com.messaging.web.model.User;
import com.messaging.web.model.Message;
import com.messaging.web.repository.UserRepository;
import com.messaging.web.repository.MessageRepository;
import com.messaging.web.repository.ConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio principal de la aplicación web
 * Implementa la lógica de negocio y acceso a datos
 */
@Service
public class WebService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private ConnectionRepository connectionRepository;
    
    /**
     * Obtener estadísticas del dashboard
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total de usuarios
        long totalUsers = userRepository.count();
        stats.put("totalUsers", totalUsers);
        
        // Usuarios conectados
        long connectedUsers = userRepository.countByIsConnectedTrue();
        stats.put("connectedUsers", connectedUsers);
        
        // Usuarios pendientes de aprobación
        long pendingUsers = userRepository.countByStatus(User.UserStatus.PENDING);
        stats.put("pendingUsers", pendingUsers);
        
        // Total de mensajes
        long totalMessages = messageRepository.count();
        stats.put("totalMessages", totalMessages);
        
        // Mensajes de hoy
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todayMessages = messageRepository.countBySentAtAfter(startOfDay);
        stats.put("todayMessages", todayMessages);
        
        // Archivos enviados
        long totalFiles = messageRepository.countByMessageType(Message.MessageType.FILE);
        stats.put("totalFiles", totalFiles);
        
        return stats;
    }
    
    /**
     * Obtener usuarios recientes
     */
    public List<User> getRecentUsers() {
        return userRepository.findTop10ByOrderByCreatedAtDesc();
    }
    
    /**
     * Obtener todos los usuarios
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Aprobar usuario
     */
    public void approveUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStatus(User.UserStatus.APPROVED);
            userRepository.save(user);
        }
    }
    
    /**
     * Rechazar usuario
     */
    public void rejectUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStatus(User.UserStatus.REJECTED);
            userRepository.save(user);
        }
    }
    
    /**
     * Obtener mensajes con paginación
     */
    public List<Message> getMessages(int page, int size, String type) {
        Pageable pageable = PageRequest.of(page, size);
        
        if (type != null && !type.isEmpty()) {
            Message.MessageType messageType = Message.MessageType.valueOf(type.toUpperCase());
            return messageRepository.findByMessageTypeOrderBySentAtDesc(messageType, pageable);
        } else {
            Page<Message> messagePage = messageRepository.findAll(pageable);
            return messagePage.getContent();
        }
    }
    
    /**
     * Obtener mensaje por ID
     */
    public Message getMessageById(Long id) {
        return messageRepository.findById(id).orElse(null);
    }
    
    /**
     * Obtener usuario con más mensajes enviados
     */
    public Map<String, Object> getTopMessageSender() {
        List<Object[]> results = messageRepository.findTopMessageSender();
        
        if (!results.isEmpty()) {
            Object[] result = results.get(0);
            Map<String, Object> topSender = new HashMap<>();
            topSender.put("username", result[0]);
            topSender.put("messageCount", result[1]);
            return topSender;
        }
        
        return new HashMap<>();
    }
    
    /**
     * Obtener archivos por tamaño
     */
    public List<Map<String, Object>> getFilesBySize() {
        List<Object[]> results = messageRepository.findFilesBySize();
        List<Map<String, Object>> files = new ArrayList<>();
        
        for (Object[] result : results) {
            Map<String, Object> file = new HashMap<>();
            file.put("fileName", result[0]);
            file.put("fileSize", result[1]);
            file.put("owner", result[2]);
            file.put("sentAt", result[3]);
            files.add(file);
        }
        
        return files;
    }
    
    /**
     * Obtener usuarios conectados
     */
    public List<Map<String, Object>> getConnectedUsers() {
        List<Object[]> results = connectionRepository.findConnectedUsers();
        List<Map<String, Object>> users = new ArrayList<>();
        
        for (Object[] result : results) {
            Map<String, Object> user = new HashMap<>();
            user.put("username", result[0]);
            user.put("clientIp", result[1]);
            user.put("connectedAt", result[2]);
            user.put("messagesCount", result[3]);
            users.add(user);
        }
        
        return users;
    }
    
    /**
     * Obtener usuarios desconectados
     */
    public List<Map<String, Object>> getDisconnectedUsers() {
        List<Object[]> results = connectionRepository.findDisconnectedUsers();
        List<Map<String, Object>> users = new ArrayList<>();
        
        for (Object[] result : results) {
            Map<String, Object> user = new HashMap<>();
            user.put("username", result[0]);
            user.put("clientIp", result[1]);
            user.put("connectedAt", result[2]);
            user.put("disconnectedAt", result[3]);
            user.put("messagesSent", result[4]);
            users.add(user);
        }
        
        return users;
    }
    
    /**
     * API REST - Información completa de usuario
     */
    public Map<String, Object> getUserCompleteInfo(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (!userOpt.isPresent()) {
            return new HashMap<>();
        }
        
        User user = userOpt.get();
        Map<String, Object> info = new HashMap<>();
        
        info.put("username", user.getUsername());
        info.put("messagesSent", messageRepository.countBySenderId(id));
        info.put("messagesReceived", messageRepository.countByReceiverId(id));
        info.put("lastConnection", user.getLastConnection());
        info.put("status", user.isConnected() ? "connected" : "disconnected");
        
        return info;
    }
    
    /**
     * API REST - Mensajes enviados por usuario
     */
    public List<Map<String, Object>> getUserSentMessages(Long id) {
        List<Object[]> results = messageRepository.findSentMessagesForAPI(id);
        List<Map<String, Object>> messages = new ArrayList<>();
        
        for (Object[] result : results) {
            Map<String, Object> message = new HashMap<>();
            message.put("userIp", result[0]);
            message.put("remoteUserIp", result[1]);
            message.put("date", result[2]);
            message.put("time", result[3]);
            messages.add(message);
        }
        
        return messages;
    }
    
    /**
     * API REST - Mensajes recibidos por usuario
     */
    public List<Map<String, Object>> getUserReceivedMessages(Long id) {
        List<Object[]> results = messageRepository.findReceivedMessagesForAPI(id);
        List<Map<String, Object>> messages = new ArrayList<>();
        
        for (Object[] result : results) {
            Map<String, Object> message = new HashMap<>();
            message.put("userIp", result[0]);
            message.put("remoteUserIp", result[1]);
            message.put("date", result[2]);
            message.put("time", result[3]);
            messages.add(message);
        }
        
        return messages;
    }
    
    /**
     * API REST - Todos los usuarios con información completa
     */
    public List<Map<String, Object>> getAllUsersAPI() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> usersInfo = new ArrayList<>();
        
        for (User user : users) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", user.getId());
            info.put("username", user.getUsername());
            info.put("email", user.getEmail());
            info.put("status", user.getStatus());
            info.put("messagesSent", messageRepository.countBySenderId(user.getId()));
            info.put("messagesReceived", messageRepository.countByReceiverId(user.getId()));
            info.put("lastConnection", user.getLastConnection());
            info.put("connected", user.isConnected());
            usersInfo.add(info);
        }
        
        return usersInfo;
    }
}
