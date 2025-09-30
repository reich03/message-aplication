package com.messaging.server.pool;

import com.messaging.server.model.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pool de conexiones de clientes usando patrón Object Pool
 * Gestiona las conexiones activas y aplica restricciones
 */
public class ConnectionPool {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);
    private static volatile ConnectionPool instance;
    
    private final Map<String, ClientConnection> activeConnections;
    private final Map<Integer, AtomicInteger> userConnectionCounts;
    private final AtomicInteger totalConnections;
    
    private ConnectionPool() {
        this.activeConnections = new ConcurrentHashMap<>();
        this.userConnectionCounts = new ConcurrentHashMap<>();
        this.totalConnections = new AtomicInteger(0);
    }
    
    public static ConnectionPool getInstance() {
        if (instance == null) {
            synchronized (ConnectionPool.class) {
                if (instance == null) {
                    instance = new ConnectionPool();
                }
            }
        }
        return instance;
    }
    
    /**
     * Agregar nueva conexión al pool
     */
    public boolean addConnection(ClientConnection connection) {
        String connectionId = connection.getConnectionId();
        int userId = connection.getUserId();
        
        // Verificar si el usuario ya tiene el máximo de conexiones
        int maxConnections = connection.getMaxConnections();
        int currentConnections = getUserConnectionCount(userId);
        
        if (currentConnections >= maxConnections) {
            logger.warn("Usuario {} ha alcanzado el límite de conexiones: {}/{}", 
                userId, currentConnections, maxConnections);
            return false;
        }
        
        // Agregar conexión
        activeConnections.put(connectionId, connection);
        userConnectionCounts.computeIfAbsent(userId, k -> new AtomicInteger(0)).incrementAndGet();
        totalConnections.incrementAndGet();
        
        logger.info("Conexión agregada: {} para usuario {}. Total: {}", 
            connectionId, userId, totalConnections.get());
        
        return true;
    }
    
    /**
     * Remover conexión del pool
     */
    public void removeConnection(String connectionId) {
        ClientConnection connection = activeConnections.remove(connectionId);
        if (connection != null) {
            int userId = connection.getUserId();
            AtomicInteger userCount = userConnectionCounts.get(userId);
            if (userCount != null) {
                userCount.decrementAndGet();
                if (userCount.get() <= 0) {
                    userConnectionCounts.remove(userId);
                }
            }
            totalConnections.decrementAndGet();
            
            logger.info("Conexión removida: {} para usuario {}. Total: {}", 
                connectionId, userId, totalConnections.get());
        }
    }
    
    /**
     * Obtener conexión por ID
     */
    public ClientConnection getConnection(String connectionId) {
        return activeConnections.get(connectionId);
    }
    
    /**
     * Obtener todas las conexiones de un usuario
     */
    public Set<ClientConnection> getUserConnections(int userId) {
        return activeConnections.values().stream()
            .filter(conn -> conn.getUserId() == userId)
            .collect(Collectors.toSet());
    }
    
    /**
     * Obtener número de conexiones activas
     */
    public int getActiveConnections() {
        return totalConnections.get();
    }
    
    /**
     * Obtener número de conexiones de un usuario específico
     */
    public int getUserConnectionCount(int userId) {
        AtomicInteger count = userConnectionCounts.get(userId);
        return count != null ? count.get() : 0;
    }
    
    /**
     * Obtener todas las conexiones activas
     */
    public Map<String, ClientConnection> getAllConnections() {
        return new ConcurrentHashMap<>(activeConnections);
    }
    
    /**
     * Limpiar conexiones inactivas
     */
    public void cleanupInactiveConnections() {
        long currentTime = System.currentTimeMillis();
        long timeout = 300000; // 5 minutos
        
        activeConnections.entrySet().removeIf(entry -> {
            ClientConnection connection = entry.getValue();
            if (currentTime - connection.getLastActivity() > timeout) {
                logger.info("Removiendo conexión inactiva: {} para usuario {}", 
                    entry.getKey(), connection.getUserId());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Verificar si un usuario puede conectarse
     */
    public boolean canUserConnect(int userId, int maxConnections) {
        return getUserConnectionCount(userId) < maxConnections;
    }
    
    /**
     * Obtener estadísticas del pool
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalConnections", totalConnections.get());
        stats.put("uniqueUsers", userConnectionCounts.size());
        stats.put("averageConnectionsPerUser", 
            userConnectionCounts.size() > 0 ? 
            (double) totalConnections.get() / userConnectionCounts.size() : 0.0);
        
        return stats;
    }
    
    /**
     * Cerrar todas las conexiones
     */
    public void shutdown() {
        logger.info("Cerrando pool de conexiones...");
        
        activeConnections.values().forEach(connection -> {
            try {
                connection.close();
            } catch (Exception e) {
                logger.error("Error cerrando conexión: " + e.getMessage());
            }
        });
        
        activeConnections.clear();
        userConnectionCounts.clear();
        totalConnections.set(0);
        
        logger.info("Pool de conexiones cerrado correctamente");
    }
}
