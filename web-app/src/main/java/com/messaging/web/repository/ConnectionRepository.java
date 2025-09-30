package com.messaging.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.messaging.web.model.ActiveConnection;
import com.messaging.web.model.ConnectionHistory;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para conexiones de usuarios
 * Proporciona métodos de acceso a datos para conexiones activas e históricas
 */
@Repository
public interface ConnectionRepository extends JpaRepository<ActiveConnection, Long> {

       /**
        * Buscar usuarios conectados con información detallada
        */
       @Query(value = "SELECT u.username, ac.client_ip, ac.connected_at, ac.messages_count " +
                     "FROM active_connections ac " +
                     "JOIN users u ON ac.user_id = u.id " +
                     "ORDER BY ac.connected_at DESC", nativeQuery = true)
       List<Object[]> findConnectedUsers();

       /**
        * Buscar usuarios desconectados con información detallada
        */
       @Query(value = "SELECT u.username, ch.client_ip, ch.connected_at, ch.disconnected_at, ch.messages_sent " +
                     "FROM connection_history ch " +
                     "JOIN users u ON ch.user_id = u.id " +
                     "ORDER BY ch.disconnected_at DESC", nativeQuery = true)
       List<Object[]> findDisconnectedUsers();

       /**
        * Buscar conexiones activas por usuario
        */
       @Query(value = "SELECT * FROM active_connections ac WHERE ac.user_id = :userId", nativeQuery = true)
       List<Object[]> findActiveConnectionsByUserId(@Param("userId") Long userId);

       /**
        * Buscar historial de conexiones por usuario
        */
       @Query(value = "SELECT * FROM connection_history ch WHERE ch.user_id = :userId ORDER BY ch.connected_at DESC", nativeQuery = true)
       List<Object[]> findConnectionHistoryByUserId(@Param("userId") Long userId);

       /**
        * Contar conexiones activas
        */
       @Query(value = "SELECT COUNT(*) FROM active_connections", nativeQuery = true)
       long countActiveConnections();

       /**
        * Contar conexiones por IP
        */
       @Query(value = "SELECT ac.client_ip, COUNT(*) as connection_count " +
                     "FROM active_connections ac " +
                     "GROUP BY ac.client_ip " +
                     "ORDER BY connection_count DESC", nativeQuery = true)
       List<Object[]> findConnectionsByIp();

       /**
        * Buscar conexiones más largas
        */
       @Query(value = "SELECT u.username, ch.client_ip, ch.connected_at, ch.disconnected_at, " +
                     "EXTRACT(EPOCH FROM (ch.disconnected_at - ch.connected_at))/60 as duration_minutes " +
                     "FROM connection_history ch " +
                     "JOIN users u ON ch.user_id = u.id " +
                     "WHERE ch.disconnected_at IS NOT NULL " +
                     "ORDER BY duration_minutes DESC", nativeQuery = true)
       List<Object[]> findLongestConnections();

       /**
        * Buscar conexiones por rango de tiempo
        */
       @Query(value = "SELECT u.username, ch.client_ip, ch.connected_at, ch.disconnected_at " +
                     "FROM connection_history ch " +
                     "JOIN users u ON ch.user_id = u.id " +
                     "WHERE ch.connected_at BETWEEN :startDate AND :endDate " +
                     "ORDER BY ch.connected_at DESC", nativeQuery = true)
       List<Object[]> findConnectionsByDateRange(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       /**
        * Estadísticas de conexiones por día
        */
       @Query(value = "SELECT CAST(ch.connected_at AS date) as date, COUNT(*) as connection_count " +
                     "FROM connection_history ch " +
                     "GROUP BY CAST(ch.connected_at AS date) " +
                     "ORDER BY date DESC", nativeQuery = true)
       List<Object[]> findConnectionStatsByDay();

       /**
        * Buscar usuarios con más conexiones
        */
       @Query(value = "SELECT u.username, COUNT(ch.id) as connection_count " +
                     "FROM connection_history ch " +
                     "JOIN users u ON ch.user_id = u.id " +
                     "GROUP BY u.id, u.username " +
                     "ORDER BY connection_count DESC", nativeQuery = true)
       List<Object[]> findUsersWithMostConnections();

       /**
        * Buscar todas las conexiones activas
        */
       List<ActiveConnection> findAll();

       /**
        * Buscar conexiones activas por usuario
        */
       List<ActiveConnection> findByUserId(Long userId);

       /**
        * Buscar conexiones inactivas (más de 1 hora sin actividad)
        */
       @Query("SELECT ac FROM ActiveConnection ac WHERE ac.connectedAt < :oneHourAgo")
       List<ActiveConnection> findInactiveConnections(@Param("oneHourAgo") LocalDateTime oneHourAgo);

       /**
        * Contar conexiones activas
        */
       long count();

       /**
        * Contar conexiones por IP
        */
       @Query("SELECT ac.clientIp, COUNT(ac) FROM ActiveConnection ac GROUP BY ac.clientIp ORDER BY COUNT(ac) DESC")
       List<Object[]> countConnectionsByIp();
}