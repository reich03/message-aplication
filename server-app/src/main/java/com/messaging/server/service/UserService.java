package com.messaging.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messaging.server.config.DatabaseConfig;
import com.messaging.server.model.ClientConnection;
import com.messaging.server.model.Message;
import com.messaging.server.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final DatabaseConfig databaseConfig;
    private final ObjectMapper objectMapper;
    private User currentUser;
    
    public UserService(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    public User authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND status = 'APPROVED'";
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, password);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error autenticando usuario: " + e.getMessage());
        }
        
        return null;
    }
    
    public boolean createUser(User user) {
        String sql = "INSERT INTO users (username, password, email, status, max_connections, max_files_per_day) " +
                    "VALUES (?, ?, ?, 'PENDING', ?, ?)";
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getEmail());
            stmt.setInt(4, user.getMaxConnections());
            stmt.setInt(5, user.getMaxFilesPerDay());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getLong(1));
                    }
                }
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error creando usuario: " + e.getMessage());
        }
        
        return false;
    }
    
    public boolean approveUser(int userId) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ?";
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logger.error("Error aprobando usuario: " + e.getMessage());
            return false;
        }
    }
    
    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error obteniendo usuario: " + e.getMessage());
        }
        
        return null;
    }
    
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error obteniendo usuarios: " + e.getMessage());
        }
        
        return users;
    }
    
    public boolean saveMessage(Message message) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, message_type, content, sent_at) " +
                    "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, message.getSenderId());
            stmt.setLong(2, message.getReceiverId());
            stmt.setString(3, message.getMessageType());
            stmt.setString(4, message.getContent());
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logger.error("Error guardando mensaje: " + e.getMessage());
            return false;
        }
    }
    
    public boolean saveFileMessage(Long senderId, Long receiverId, String messageType, 
                                   String filePath, String fileName) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, message_type, content, file_name, sent_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, senderId);
            stmt.setLong(2, receiverId);
            stmt.setString(3, messageType);
            stmt.setString(4, filePath);  // Ruta del archivo en el servidor
            stmt.setString(5, fileName);  // Nombre original del archivo
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            
            int rowsAffected = stmt.executeUpdate();
            logger.info("Mensaje de archivo guardado en DB: {} -> {} ({})", senderId, receiverId, fileName);
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.error("Error guardando mensaje de archivo: " + e.getMessage());
            return false;
        }
    }
    
    public String getUserMessages(Long userId) {
        String sql = "SELECT m.*, u1.username as sender_name, u2.username as receiver_name " +
                    "FROM messages m " +
                    "JOIN users u1 ON m.sender_id = u1.id " +
                    "JOIN users u2 ON m.receiver_id = u2.id " +
                    "WHERE m.sender_id = ? OR m.receiver_id = ? " +
                    "ORDER BY m.sent_at DESC LIMIT 50";
        
        List<Message> messages = new ArrayList<>();
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Uso del patrón Builder para construir Message
                    Message message = Message.builder()
                        .id(rs.getLong("id"))
                        .senderId(rs.getLong("sender_id"))
                        .receiverId(rs.getLong("receiver_id"))
                        .messageType(rs.getString("message_type"))
                        .content(rs.getString("content"))
                        .sentAt(rs.getTimestamp("sent_at").toLocalDateTime())
                        .isRead(false)
                        .build();
                    messages.add(message);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error obteniendo mensajes: " + e.getMessage());
        }
        
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (Exception e) {
            logger.error("Error serializando mensajes: " + e.getMessage());
            return "[]";
        }
    }
    
    public void registerConnection(ClientConnection connection) {
        try (Connection conn = databaseConfig.getConnection()) {
            String updateUserSql = "UPDATE users SET connected = TRUE, last_connection = ? WHERE id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateUserSql)) {
                updateStmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                updateStmt.setLong(2, connection.getUserId());
                updateStmt.executeUpdate();
                logger.info("Usuario {} marcado como conectado", connection.getUserId());
            }
            
            String insertConnSql = "INSERT INTO active_connections (user_id, client_ip, connected_at) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertConnSql)) {
                insertStmt.setLong(1, connection.getUserId());
                insertStmt.setString(2, connection.getClientIp());
                insertStmt.setTimestamp(3, Timestamp.valueOf(connection.getConnectedAt()));
                insertStmt.executeUpdate();
                logger.info("Conexión registrada en active_connections para usuario {}", connection.getUserId());
            }
            
        } catch (SQLException e) {
            logger.error("Error registrando conexión: " + e.getMessage(), e);
        }
    }
    
    public void registerDisconnection(ClientConnection connection) {
        String sql = "INSERT INTO connection_history (user_id, client_ip, connected_at, disconnected_at, messages_sent) " +
                    "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, connection.getUserId());
            stmt.setString(2, connection.getClientIp());
            stmt.setTimestamp(3, Timestamp.valueOf(connection.getConnectedAt()));
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(5, connection.getMessagesCount());
            
            stmt.executeUpdate();
            
            updateUserConnectionStatus(connection.getUserId(), false);
            
        } catch (SQLException e) {
            logger.error("Error registrando desconexión: " + e.getMessage());
        }
    }
    
    public String getConnectedUsers() {
        String sql = "SELECT u.id, u.username, u.email, u.status, u.created_at, u.last_connection, " +
                    "u.connected, u.connection_count, u.max_connections, u.files_sent_count, u.max_files_per_day " +
                    "FROM users u " +
                    "WHERE u.status = 'APPROVED' AND u.connected = TRUE " +
                    "ORDER BY u.last_connection DESC NULLS LAST";
        
        List<User> connectedUsers = new ArrayList<>();
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            logger.info("Ejecutando consulta de usuarios conectados...");
            
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setStatus(rs.getString("status"));
                
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    user.setCreatedAt(createdAt.toLocalDateTime());
                }
                
                Timestamp lastConnection = rs.getTimestamp("last_connection");
                if (lastConnection != null) {
                    user.setLastConnection(lastConnection.toLocalDateTime());
                }
                
                user.setConnected(rs.getBoolean("connected"));
                user.setConnectionCount(rs.getInt("connection_count"));
                user.setMaxConnections(rs.getInt("max_connections"));
                user.setFilesSentCount(rs.getInt("files_sent_count"));
                user.setMaxFilesPerDay(rs.getInt("max_files_per_day"));
                
                logger.info("Usuario conectado encontrado: {} (ID: {})", user.getUsername(), user.getId());
                connectedUsers.add(user);
            }
            
            logger.info("Total de usuarios conectados encontrados: {}", connectedUsers.size());
            
        } catch (SQLException e) {
            logger.error("Error obteniendo usuarios conectados: {}", e.getMessage(), e);
        }
        
        try {
            String json = objectMapper.writeValueAsString(connectedUsers);
            logger.info("JSON generado con {} usuarios: {}", connectedUsers.size(), json);
            return json;
        } catch (Exception e) {
            logger.error("Error serializando usuarios: {}", e.getMessage(), e);
            return "[]";
        }
    }
    
    public void cleanupInactiveUsers() {
        String sql = "UPDATE users SET connected = FALSE WHERE id NOT IN " +
                    "(SELECT DISTINCT user_id FROM active_connections)";
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                logger.info("Actualizados " + updated + " usuarios como desconectados");
            }
            
        } catch (SQLException e) {
            logger.error("Error limpiando usuarios inactivos: " + e.getMessage());
        }
    }
    
    private void updateUserConnectionStatus(Long userId, boolean isConnected) {
        String sql = "UPDATE users SET connected = ?, last_connection = ? WHERE id = ?";
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, isConnected);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(3, userId);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Error actualizando estado de conexión: " + e.getMessage());
        }
    }
    
    public String getMessagesWithUser(Long userId1, Long userId2) {
        String sql = "SELECT m.id, m.sender_id, m.receiver_id, m.content, m.message_type, " +
                    "m.file_name, m.sent_at, s.username as sender_username, r.username as receiver_username " +
                    "FROM messages m " +
                    "JOIN users s ON m.sender_id = s.id " +
                    "JOIN users r ON m.receiver_id = r.id " +
                    "WHERE (m.sender_id = ? AND m.receiver_id = ?) OR " +
                    "(m.sender_id = ? AND m.receiver_id = ?) " +
                    "ORDER BY m.sent_at ASC";
        
        List<Message> messages = new ArrayList<>();
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId1);
            stmt.setLong(2, userId2);
            stmt.setLong(3, userId2);
            stmt.setLong(4, userId1);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Uso del patrón Builder para construir Message con todos sus campos
                    Message message = Message.builder()
                        .id(rs.getLong("id"))
                        .senderId(rs.getLong("sender_id"))
                        .receiverId(rs.getLong("receiver_id"))
                        .content(rs.getString("content"))
                        .messageType(rs.getString("message_type"))
                        .fileName(rs.getString("file_name"))
                        .createdAt(rs.getTimestamp("sent_at").toLocalDateTime())
                        .senderUsername(rs.getString("sender_username"))
                        .receiverUsername(rs.getString("receiver_username"))
                        .isRead(false)
                        .build();
                    
                    messages.add(message);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error obteniendo mensajes entre usuarios: " + e.getMessage());
        }
        
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (Exception e) {
            logger.error("Error serializando mensajes: " + e.getMessage());
            return "[]";
        }
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setStatus(rs.getString("status"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        user.setLastConnection(rs.getTimestamp("last_connection") != null ? 
                              rs.getTimestamp("last_connection").toLocalDateTime() : null);
        user.setConnected(rs.getBoolean("connected"));
        user.setConnectionCount(rs.getInt("connection_count"));
        user.setMaxConnections(rs.getInt("max_connections"));
        user.setFilesSentCount(rs.getInt("files_sent_count"));
        user.setMaxFilesPerDay(rs.getInt("max_files_per_day"));
        return user;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}
