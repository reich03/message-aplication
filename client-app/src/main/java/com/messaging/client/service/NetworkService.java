package com.messaging.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messaging.client.model.Message;
import com.messaging.client.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio de red para comunicación con el servidor
 * Implementa patrón Singleton y maneja la conexión TCP
 */
public class NetworkService {
    
    private static final Logger logger = LoggerFactory.getLogger(NetworkService.class);
    private static volatile NetworkService instance;
    
    private final String serverHost;
    private final int serverPort;
    private final ObjectMapper objectMapper;
    
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final AtomicBoolean connected;
    
    private NetworkService() {
        this.serverHost = "localhost"; // En producción sería configurable
        this.serverPort = 9998; // Puerto externo del servidor
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.connected = new AtomicBoolean(false);
    }
    
    public static NetworkService getInstance() {
        if (instance == null) {
            synchronized (NetworkService.class) {
                if (instance == null) {
                    instance = new NetworkService();
                }
            }
        }
        return instance;
    }
    
    /**
     * Conectar al servidor
     */
    public boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            connected.set(true);
            
            logger.info("Conectado al servidor: {}:{}", serverHost, serverPort);
            return true;
            
        } catch (IOException e) {
            logger.error("Error conectando al servidor: " + e.getMessage());
            connected.set(false);
            return false;
        }
    }
    
    /**
     * Desconectar del servidor
     */
    public void disconnect() {
        connected.set(false);
        
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Error desconectando: " + e.getMessage());
        }
        
        logger.info("Desconectado del servidor");
    }
    
    /**
     * Autenticar usuario
     */
    public boolean authenticate(String username, String password) {
        if (!ensureConnection()) {
            return false;
        }
        
        try {
            // Enviar solicitud de autenticación
            String authRequest = "AUTH:" + username + ":" + password;
            writer.println(authRequest);
            
            // Leer respuesta
            String response = reader.readLine();
            if (response != null && response.startsWith("AUTH_SUCCESS")) {
                logger.info("Autenticación exitosa para usuario: {}", username);
                return true;
            } else {
                logger.warn("Autenticación fallida para usuario: {}", username);
                return false;
            }
            
        } catch (IOException e) {
            logger.error("Error en autenticación: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Registrar nuevo usuario
     */
    public boolean register(String username, String password, String email) {
        if (!ensureConnection()) {
            return false;
        }
        
        try {
            // Crear usuario
            User user = new User(username, password, email);
            String userJson = objectMapper.writeValueAsString(user);
            
            // Enviar solicitud de registro
            String registerRequest = "REGISTER:" + userJson;
            writer.println(registerRequest);
            
            // Leer respuesta
            String response = reader.readLine();
            if (response != null && response.startsWith("REGISTER_SUCCESS")) {
                logger.info("Registro exitoso para usuario: {}", username);
                return true;
            } else {
                logger.warn("Registro fallido para usuario: {}", username);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error en registro: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Enviar mensaje
     */
    public boolean sendMessage(int receiverId, String content) {
        if (!ensureConnection()) {
            return false;
        }
        
        try {
            Message message = new Message();
            message.setReceiverId(receiverId);
            message.setMessageType("TEXT");
            message.setContent(content);
            
            String messageJson = objectMapper.writeValueAsString(message);
            String request = "SEND_MESSAGE:" + messageJson;
            
            writer.println(request);
            
            String response = reader.readLine();
            return response != null && response.startsWith("MESSAGE_SENT");
            
        } catch (Exception e) {
            logger.error("Error enviando mensaje: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Enviar archivo
     */
    public boolean sendFile(int receiverId, File file) {
        if (!ensureConnection()) {
            return false;
        }
        
        try {
            // Verificar tamaño del archivo
            if (file.length() > 10 * 1024 * 1024) { // 10MB
                logger.warn("Archivo demasiado grande: {}", file.getName());
                return false;
            }
            
            // Enviar información del archivo
            String fileInfo = String.format("SEND_FILE:%d:%s:%d", 
                receiverId, file.getName(), file.length());
            writer.println(fileInfo);
            
            // Leer respuesta
            String response = reader.readLine();
            if (response != null && response.startsWith("FILE_ACCEPTED")) {
                // Enviar contenido del archivo
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        // Enviar datos del archivo (implementación simplificada)
                        writer.println("FILE_DATA:" + new String(buffer, 0, bytesRead));
                    }
                }
                
                // Confirmar fin de archivo
                writer.println("FILE_END");
                
                response = reader.readLine();
                return response != null && response.startsWith("FILE_SENT");
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("Error enviando archivo: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtener usuarios conectados
     */
    public List<User> getConnectedUsers() {
        if (!ensureConnection()) {
            return new ArrayList<>();
        }
        
        try {
            writer.println("GET_USERS:");
            String response = reader.readLine();
            
            if (response != null && response.startsWith("USERS:")) {
                String usersJson = response.substring(6); // Remover "USERS:"
                return objectMapper.readValue(usersJson, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, User.class));
            }
            
        } catch (Exception e) {
            logger.error("Error obteniendo usuarios: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Obtener mensajes del usuario
     */
    public List<Message> getUserMessages() {
        if (!ensureConnection()) {
            return new ArrayList<>();
        }
        
        try {
            writer.println("GET_MESSAGES:");
            String response = reader.readLine();
            
            if (response != null && response.startsWith("MESSAGES:")) {
                String messagesJson = response.substring(9); // Remover "MESSAGES:"
                return objectMapper.readValue(messagesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Message.class));
            }
            
        } catch (Exception e) {
            logger.error("Error obteniendo mensajes: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Obtener mensajes con un usuario específico
     */
    public List<Message> getMessagesWithUser(int userId) {
        if (!ensureConnection()) {
            return new ArrayList<>();
        }
        
        try {
            String request = "GET_MESSAGES_WITH_USER:" + userId;
            writer.println(request);
            String response = reader.readLine();
            
            if (response != null && response.startsWith("MESSAGES:")) {
                String messagesJson = response.substring(9);
                return objectMapper.readValue(messagesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Message.class));
            }
            
        } catch (Exception e) {
            logger.error("Error obteniendo mensajes con usuario: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Enviar ping al servidor
     */
    public void sendPing() {
        if (!ensureConnection()) {
            return;
        }
        
        try {
            writer.println("PING:");
            String response = reader.readLine();
            
            if (response != null && response.startsWith("PONG")) {
                logger.debug("Ping exitoso");
            } else {
                logger.warn("Ping fallido");
                connected.set(false);
            }
            
        } catch (IOException e) {
            logger.error("Error enviando ping: " + e.getMessage());
            connected.set(false);
        }
    }
    
    /**
     * Verificar y mantener conexión
     */
    private boolean ensureConnection() {
        if (!connected.get() || socket == null || socket.isClosed()) {
            return connect();
        }
        return true;
    }
    
    /**
     * Verificar si está conectado
     */
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed();
    }
}
