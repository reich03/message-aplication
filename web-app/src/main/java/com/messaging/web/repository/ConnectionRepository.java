package com.messaging.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para conexiones de usuarios
 * Proporciona métodos de acceso a datos para conexiones activas e históricas
 */
@Repository
public interface ConnectionRepository extends JpaRepository<Object, Long> {
    
    /**
     * Buscar usuarios conectados con información detallada
     */
    @Query("SELECT u.username, ac.clientIp, ac.connectedAt, ac.messagesCount " +
           "FROM active_connections ac " +
           "JOIN users u ON ac.user_id = u.id " +
           "ORDER BY ac.connectedAt DESC")
    List<Object[]> findConnectedUsers();
    
    /**
     * Buscar usuarios desconectados con información detallada
     */
    @Query("SELECT u.username, ch.clientIp, ch.connectedAt, ch.disconnectedAt, ch.messagesSent " +
           "FROM connection_history ch " +
           "JOIN users u ON ch.user_id = u.id " +
           "ORDER BY ch.disconnectedAt DESC")
    List<Object[]> findDisconnectedUsers();
    
    /**
     * Buscar conexiones activas por usuario
     */
    @Query("SELECT ac FROM active_connections ac WHERE ac.user_id = :userId")
    List<Object[]> findActiveConnectionsByUserId(Long userId);
    
    /**
     * Buscar historial de conexiones por usuario
     */
    @Query("SELECT ch FROM connection_history ch WHERE ch.user_id = :userId ORDER BY ch.connectedAt DESC")
    List<Object[]> findConnectionHistoryByUserId(Long userId);
    
    /**
     * Contar conexiones activas
     */
    @Query("SELECT COUNT(*) FROM active_connections")
    long countActiveConnections();
    
    /**
     * Contar conexiones por IP
     */
    @Query("SELECT ac.clientIp, COUNT(*) as connectionCount FROM active_connections ac GROUP BY ac.clientIp ORDER BY connectionCount DESC")
    List<Object[]> findConnectionsByIp();
    
    /**
     * Buscar conexiones más largas
     */
    @Query("SELECT u.username, ch.clientIp, ch.connectedAt, ch.disconnectedAt, " +
           "EXTRACT(EPOCH FROM (ch.disconnectedAt - ch.connectedAt))/60 as durationMinutes " +
           "FROM connection_history ch " +
           "JOIN users u ON ch.user_id = u.id " +
           "WHERE ch.disconnectedAt IS NOT NULL " +
           "ORDER BY durationMinutes DESC")
    List<Object[]> findLongestConnections();
    
    /**
     * Buscar conexiones por rango de tiempo
     */
    @Query("SELECT u.username, ch.clientIp, ch.connectedAt, ch.disconnectedAt " +
           "FROM connection_history ch " +
           "JOIN users u ON ch.user_id = u.id " +
           "WHERE ch.connectedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ch.connectedAt DESC")
    List<Object[]> findConnectionsByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
    
    /**
     * Estadísticas de conexiones por día
     */
    @Query("SELECT CAST(ch.connectedAt AS date) as date, COUNT(*) as connectionCount " +
           "FROM connection_history ch " +
           "GROUP BY CAST(ch.connectedAt AS date) " +
           "ORDER BY date DESC")
    List<Object[]> findConnectionStatsByDay();
    
    /**
     * Buscar usuarios con más conexiones
     */
    @Query("SELECT u.username, COUNT(ch) as connectionCount " +
           "FROM connection_history ch " +
           "JOIN users u ON ch.user_id = u.id " +
           "GROUP BY u.id, u.username " +
           "ORDER BY connectionCount DESC")
    List<Object[]> findUsersWithMostConnections();
    
    /**
     * Buscar conexiones inactivas (más de 1 hora sin actividad)
     */
    @Query("SELECT ac FROM active_connections ac WHERE ac.connectedAt < CURRENT_TIMESTAMP - INTERVAL '1 HOUR'")
    List<Object[]> findInactiveConnections();
}
