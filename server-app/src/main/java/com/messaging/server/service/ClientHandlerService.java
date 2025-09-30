package com.messaging.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messaging.server.model.ClientConnection;
import com.messaging.server.model.Message;
import com.messaging.server.model.User;
import com.messaging.server.pool.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio para manejar conexiones de clientes individuales
 * Implementa concurrencia usando hilos
 */
public class ClientHandlerService implements Runnable {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientHandlerService.class);
    
    private final Socket clientSocket;
    private final UserService userService;
    private final LoggingService loggingService;
    private final ConnectionPool connectionPool;
    private final ObjectMapper objectMapper;
    
    private BufferedReader reader;
    private PrintWriter writer;
    private ClientConnection clientConnection;
    private final AtomicBoolean isRunning;
    
    public ClientHandlerService(Socket clientSocket, UserService userService, 
                              LoggingService loggingService, ConnectionPool connectionPool) {
        this.clientSocket = clientSocket;
        this.userService = userService;
        this.loggingService = loggingService;
        this.connectionPool = connectionPool;
        this.objectMapper = new ObjectMapper();
        this.isRunning = new AtomicBoolean(false);
    }
    
    @Override
    public void run() {
        try {
            initializeStreams();
            handleClientConnection();
        } catch (Exception e) {
            logger.error("Error en handler de cliente: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    private void initializeStreams() throws IOException {
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer = new PrintWriter(clientSocket.getOutputStream(), true);
    }
    
    private void handleClientConnection() {
        isRunning.set(true);
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        
        try {
            // Proceso de autenticación
            if (!authenticateClient()) {
                sendResponse("AUTH_FAILED", "Autenticación fallida");
                return;
            }
            
            // Registrar conexión en el pool
            String connectionId = UUID.randomUUID().toString();
            clientConnection = new ClientConnection(
                connectionId,
                userService.getCurrentUser().getId(),
                userService.getCurrentUser().getUsername(),
                clientSocket,
                clientIp,
                userService.getCurrentUser().getMaxConnections(),
                userService.getCurrentUser().getMaxFilesPerDay()
            );
            
            if (!connectionPool.addConnection(clientConnection)) {
                sendResponse("CONNECTION_LIMIT", "Límite de conexiones alcanzado");
                return;
            }
            
            // Registrar conexión en base de datos
            userService.registerConnection(clientConnection);
            
            sendResponse("AUTH_SUCCESS", "Autenticación exitosa");
            loggingService.info("Cliente autenticado: " + userService.getCurrentUser().getUsername() + 
                              " desde " + clientIp);
            
            // Bucle principal de manejo de mensajes
            handleMessages();
            
        } catch (Exception e) {
            logger.error("Error en handler: " + e.getMessage());
        }
    }
    
    private boolean authenticateClient() {
        try {
            String authRequest = reader.readLine();
            if (authRequest == null) return false;
            
            // Parsear solicitud de autenticación
            String[] parts = authRequest.split(":");
            if (parts.length != 3 || !"AUTH".equals(parts[0])) {
                return false;
            }
            
            String username = parts[1];
            String password = parts[2];
            
            // Verificar credenciales
            User user = userService.authenticateUser(username, password);
            if (user == null) {
                loggingService.warn("Intento de autenticación fallido para usuario: " + username);
                return false;
            }
            
            // Verificar si el usuario está aprobado
            if (!"APPROVED".equals(user.getStatus())) {
                loggingService.warn("Usuario no aprobado intenta conectarse: " + username);
                return false;
            }
            
            userService.setCurrentUser(user);
            return true;
            
        } catch (IOException e) {
            logger.error("Error en autenticación: " + e.getMessage());
            return false;
        }
    }
    
    private void handleMessages() {
        String message;
        
        while (isRunning.get() && (message = readMessage()) != null) {
            try {
                processMessage(message);
            } catch (Exception e) {
                logger.error("Error procesando mensaje: " + e.getMessage());
                sendResponse("ERROR", "Error procesando mensaje");
            }
        }
    }
    
    private String readMessage() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            logger.error("Error leyendo mensaje: " + e.getMessage());
            return null;
        }
    }
    
    private void processMessage(String message) {
        try {
            String[] parts = message.split(":", 2);
            String command = parts[0];
            String data = parts.length > 1 ? parts[1] : "";
            
            switch (command) {
                case "SEND_MESSAGE":
                    handleSendMessage(data);
                    break;
                case "SEND_FILE":
                    handleSendFile(data);
                    break;
                case "GET_MESSAGES":
                    handleGetMessages(data);
                    break;
                case "GET_USERS":
                    handleGetUsers();
                    break;
                case "PING":
                    handlePing();
                    break;
                default:
                    sendResponse("UNKNOWN_COMMAND", "Comando no reconocido: " + command);
            }
            
        } catch (Exception e) {
            logger.error("Error procesando comando: " + e.getMessage());
            sendResponse("ERROR", "Error procesando comando");
        }
    }
    
    private void handleSendMessage(String data) {
        try {
            Message message = objectMapper.readValue(data, Message.class);
            message.setSenderId(clientConnection.getUserId());
            
            // Guardar mensaje en base de datos
            boolean saved = userService.saveMessage(message);
            if (saved) {
                clientConnection.incrementMessagesCount();
                sendResponse("MESSAGE_SENT", "Mensaje enviado correctamente");
                loggingService.info("Mensaje enviado de " + clientConnection.getUsername() + 
                                  " a usuario " + message.getReceiverId());
            } else {
                sendResponse("MESSAGE_FAILED", "Error guardando mensaje");
            }
            
        } catch (Exception e) {
            logger.error("Error enviando mensaje: " + e.getMessage());
            sendResponse("MESSAGE_ERROR", "Error procesando mensaje");
        }
    }
    
    private void handleSendFile(String data) {
        try {
            // Verificar límite de archivos
            if (!clientConnection.canSendFile()) {
                sendResponse("FILE_LIMIT", "Límite de archivos diarios alcanzado");
                return;
            }
            
            // Procesar envío de archivo (implementación simplificada)
            clientConnection.incrementFilesSentCount();
            sendResponse("FILE_SENT", "Archivo enviado correctamente");
            loggingService.info("Archivo enviado por " + clientConnection.getUsername());
            
        } catch (Exception e) {
            logger.error("Error enviando archivo: " + e.getMessage());
            sendResponse("FILE_ERROR", "Error procesando archivo");
        }
    }
    
    private void handleGetMessages(String data) {
        try {
            // Obtener mensajes del usuario
            String messages = userService.getUserMessages(clientConnection.getUserId());
            sendResponse("MESSAGES", messages);
            
        } catch (Exception e) {
            logger.error("Error obteniendo mensajes: " + e.getMessage());
            sendResponse("MESSAGES_ERROR", "Error obteniendo mensajes");
        }
    }
    
    private void handleGetUsers() {
        try {
            String users = userService.getConnectedUsers();
            sendResponse("USERS", users);
            
        } catch (Exception e) {
            logger.error("Error obteniendo usuarios: " + e.getMessage());
            sendResponse("USERS_ERROR", "Error obteniendo usuarios");
        }
    }
    
    private void handlePing() {
        clientConnection.updateLastActivity();
        sendResponse("PONG", "OK");
    }
    
    private void sendResponse(String command, String data) {
        try {
            writer.println(command + ":" + data);
            writer.flush();
        } catch (Exception e) {
            logger.error("Error enviando respuesta: " + e.getMessage());
        }
    }
    
    private void cleanup() {
        isRunning.set(false);
        
        try {
            if (clientConnection != null) {
                // Remover del pool de conexiones
                connectionPool.removeConnection(clientConnection.getConnectionId());
                
                // Registrar desconexión en base de datos
                userService.registerDisconnection(clientConnection);
                
                loggingService.info("Cliente desconectado: " + clientConnection.getUsername());
            }
            
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            
        } catch (IOException e) {
            logger.error("Error en cleanup: " + e.getMessage());
        }
    }
}
