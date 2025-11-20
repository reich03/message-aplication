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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
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
            if (!authenticateClient()) {
                sendResponse("AUTH_FAILED", "Autenticación fallida");
                return;
            }
            
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
            
            userService.registerConnection(clientConnection);
            
            // Enviar datos del usuario autenticado como JSON
            User currentUser = userService.getCurrentUser();
            String userJson = objectMapper.writeValueAsString(currentUser);
            sendResponse("AUTH_SUCCESS", userJson);
            loggingService.info("Cliente autenticado: " + currentUser.getUsername() + 
                              " desde " + clientIp);
            
            handleMessages();
            
        } catch (Exception e) {
            logger.error("Error en handler: " + e.getMessage());
        }
    }
    
    private boolean authenticateClient() {
        try {
            String authRequest = reader.readLine();
            if (authRequest == null) return false;
            
            String[] parts = authRequest.split(":");
            if (parts.length != 3 || !"AUTH".equals(parts[0])) {
                return false;
            }
            
            String username = parts[1];
            String password = parts[2];
            
            User user = userService.authenticateUser(username, password);
            if (user == null) {
                loggingService.warn("Intento de autenticación fallido para usuario: " + username);
                return false;
            }
            
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
                case "DOWNLOAD_FILE":
                    handleDownloadFile(data);
                    break;
                case "GET_MESSAGES":
                    handleGetMessages(data);
                    break;
                case "GET_MESSAGES_WITH_USER":
                    handleGetMessagesWithUser(data);
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
            if (!clientConnection.canSendFile()) {
                sendResponse("FILE_LIMIT", "Límite de archivos diarios alcanzado");
                return;
            }
            
            // Parsear información del archivo: receiverId:fileName:fileSize
            String[] parts = data.split(":", 3);
            if (parts.length < 3) {
                sendResponse("FILE_ERROR", "Información de archivo inválida");
                return;
            }
            
            Long receiverId = Long.parseLong(parts[0]);
            String fileName = parts[1];
            long fileSize = Long.parseLong(parts[2]);
            
            logger.info("Recibiendo archivo: {} ({} bytes) para usuario {}", fileName, fileSize, receiverId);
            
            // Aceptar el archivo
            sendResponse("FILE_ACCEPTED", "OK");
            
            // Recibir los datos del archivo
            StringBuilder fileData = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("FILE_END")) {
                    break;
                } else if (line.startsWith("FILE_DATA:")) {
                    fileData.append(line.substring(10)); // Remover "FILE_DATA:"
                }
            }
            
            logger.info("Archivo recibido completo: {} caracteres de Base64", fileData.length());
            
            // Crear directorio del usuario si no existe
            Path userUploadDir = Paths.get("uploads", String.valueOf(clientConnection.getUserId()));
            Files.createDirectories(userUploadDir);
            
            // Generar nombre único con timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueFileName = timestamp + "_" + fileName;
            Path filePath = userUploadDir.resolve(uniqueFileName);
            
            // Decodificar Base64 y guardar archivo
            byte[] fileBytes = Base64.getDecoder().decode(fileData.toString());
            Files.write(filePath, fileBytes, StandardOpenOption.CREATE);
            
            logger.info("Archivo guardado en: {}", filePath.toString());
            
            // Determinar tipo de mensaje (IMAGE o FILE)
            String messageType = isImageFile(fileName) ? "IMAGE" : "FILE";
            
            // Guardar en base de datos
            String relativeFilePath = "uploads/" + clientConnection.getUserId() + "/" + uniqueFileName;
            boolean saved = userService.saveFileMessage(
                clientConnection.getUserId(),
                receiverId,
                messageType,
                relativeFilePath,
                fileName
            );
            
            if (saved) {
                clientConnection.incrementFilesSentCount();
                sendResponse("FILE_SENT", "Archivo enviado correctamente");
                loggingService.info("Archivo enviado por " + clientConnection.getUsername() + 
                                  " a usuario " + receiverId + ": " + fileName);
            } else {
                sendResponse("FILE_ERROR", "Error guardando información del archivo");
            }
            
        } catch (Exception e) {
            logger.error("Error enviando archivo: " + e.getMessage(), e);
            sendResponse("FILE_ERROR", "Error procesando archivo: " + e.getMessage());
        }
    }
    
    private boolean isImageFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
               lower.endsWith(".png") || lower.endsWith(".gif") || 
               lower.endsWith(".bmp") || lower.endsWith(".webp");
    }
    
    private void handleDownloadFile(String filePath) {
        try {
            // El filePath viene como "uploads/1/20251118_081120_perrito.jpeg"
            Path fileToDownload = Paths.get(filePath);
            
            if (!Files.exists(fileToDownload)) {
                sendResponse("FILE_NOT_FOUND", "Archivo no encontrado");
                logger.warn("Archivo no encontrado: {}", filePath);
                return;
            }
            
            // Leer archivo y convertir a Base64
            byte[] fileBytes = Files.readAllBytes(fileToDownload);
            String base64Content = Base64.getEncoder().encodeToString(fileBytes);
            
            // Obtener nombre del archivo
            String fileName = fileToDownload.getFileName().toString();
            
            logger.info("Enviando archivo: {} ({} bytes)", fileName, fileBytes.length);
            
            // Enviar información del archivo
            sendResponse("FILE_INFO", fileName + ":" + fileBytes.length);
            
            // Esperar confirmación
            String response = reader.readLine();
            if (response != null && response.startsWith("FILE_READY")) {
                // Enviar contenido en Base64
                writer.println("FILE_DATA:" + base64Content);
                writer.flush();
                
                // Confirmar fin
                writer.println("FILE_COMPLETE");
                writer.flush();
                
                logger.info("Archivo descargado exitosamente: {}", fileName);
            }
            
        } catch (Exception e) {
            logger.error("Error descargando archivo: " + e.getMessage(), e);
            sendResponse("DOWNLOAD_ERROR", "Error descargando archivo: " + e.getMessage());
        }
    }
    
    private void handleGetMessages(String data) {
        try {
            String messages = userService.getUserMessages(clientConnection.getUserId());
            sendResponse("MESSAGES", messages);
            
        } catch (Exception e) {
            logger.error("Error obteniendo mensajes: " + e.getMessage());
            sendResponse("MESSAGES_ERROR", "Error obteniendo mensajes");
        }
    }
    
    private void handleGetMessagesWithUser(String data) {
        try {
            Long otherUserId = Long.parseLong(data);
            String messages = userService.getMessagesWithUser(clientConnection.getUserId(), otherUserId);
            sendResponse("MESSAGES", messages);
            
        } catch (Exception e) {
            logger.error("Error obteniendo mensajes con usuario: " + e.getMessage());
            sendResponse("MESSAGES_ERROR", "Error obteniendo mensajes con usuario");
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
                connectionPool.removeConnection(clientConnection.getConnectionId());
                
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
