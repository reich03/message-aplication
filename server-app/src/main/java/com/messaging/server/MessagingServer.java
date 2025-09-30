package com.messaging.server;

import com.messaging.server.config.DatabaseConfig;
import com.messaging.server.config.ServerConfig;
import com.messaging.server.pool.ConnectionPool;
import com.messaging.server.service.ClientHandlerService;
import com.messaging.server.service.LoggingService;
import com.messaging.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servidor principal de la aplicación de mensajería
 * Implementa concurrencia usando hilos y patrones de diseño
 */
public class MessagingServer {
    
    private static final Logger logger = LoggerFactory.getLogger(MessagingServer.class);
    
    private final ServerConfig serverConfig;
    private final DatabaseConfig databaseConfig;
    private final LoggingService loggingService;
    private final UserService userService;
    private final ConnectionPool connectionPool;
    private final ExecutorService threadPool;
    private final AtomicBoolean isRunning;
    
    private ServerSocket serverSocket;

    public MessagingServer() {
        this.serverConfig = ServerConfig.getInstance();
        this.databaseConfig = DatabaseConfig.getInstance();
        this.loggingService = LoggingService.getInstance();
        this.userService = new UserService(databaseConfig);
        this.connectionPool = ConnectionPool.getInstance();
        this.threadPool = Executors.newFixedThreadPool(serverConfig.getMaxThreads());
        this.isRunning = new AtomicBoolean(false);
    }

    public void start() {
        try {
            // Inicializar base de datos
            databaseConfig.initialize();
            loggingService.info("Base de datos inicializada correctamente");
            
            // Crear socket del servidor
            serverSocket = new ServerSocket(serverConfig.getPort());
            isRunning.set(true);
            
            loggingService.info("Servidor iniciado en puerto: " + serverConfig.getPort());
            System.out.println("=== SERVIDOR DE MENSAJERÍA INICIADO ===");
            System.out.println("Puerto: " + serverConfig.getPort());
            System.out.println("Max conexiones: " + serverConfig.getMaxConnections());
            System.out.println("Max hilos: " + serverConfig.getMaxThreads());
            System.out.println("=====================================");
            
            // Iniciar hilo de limpieza de conexiones
            startConnectionCleanupTask();
            
            // Bucle principal para aceptar conexiones
            while (isRunning.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientAddress = clientSocket.getInetAddress().getHostAddress();
                    
                    loggingService.info("Nueva conexión desde: " + clientAddress);
                    
                    // Verificar límite de conexiones
                    if (connectionPool.getActiveConnections() >= serverConfig.getMaxConnections()) {
                        loggingService.warn("Límite de conexiones alcanzado. Rechazando conexión de: " + clientAddress);
                        clientSocket.close();
                        continue;
                    }
                    
                    // Crear y ejecutar handler para el cliente
                    ClientHandlerService clientHandler = new ClientHandlerService(
                        clientSocket, 
                        userService, 
                        loggingService,
                        connectionPool
                    );
                    
                    threadPool.submit(clientHandler);
                    
                } catch (IOException e) {
                    if (isRunning.get()) {
                        loggingService.error("Error aceptando conexión: " + e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            loggingService.error("Error iniciando servidor: " + e.getMessage());
            System.err.println("Error iniciando servidor: " + e.getMessage());
        }
    }

    public void stop() {
        isRunning.set(false);
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            loggingService.error("Error cerrando servidor: " + e.getMessage());
        }
        
        threadPool.shutdown();
        connectionPool.shutdown();
        databaseConfig.close();
        
        loggingService.info("Servidor detenido correctamente");
        System.out.println("Servidor detenido correctamente");
    }

    private void startConnectionCleanupTask() {
        Thread cleanupThread = new Thread(() -> {
            while (isRunning.get()) {
                try {
                    Thread.sleep(60000); // Limpiar cada minuto
                    connectionPool.cleanupInactiveConnections();
                    userService.cleanupInactiveUsers();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        cleanupThread.setDaemon(true);
        cleanupThread.setName("ConnectionCleanup");
        cleanupThread.start();
    }

    public static void main(String[] args) {
        MessagingServer server = new MessagingServer();
        
        // Agregar shutdown hook para cerrar correctamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nDeteniendo servidor...");
            server.stop();
        }));
        
        server.start();
    }
}
