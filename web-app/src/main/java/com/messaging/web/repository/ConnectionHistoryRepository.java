package com.messaging.web.repository;

import com.messaging.web.model.ConnectionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para historial de conexiones
 */
@Repository
public interface ConnectionHistoryRepository extends JpaRepository<ConnectionHistory, Long> {
    
    /**
     * Buscar historial por usuario
     */
    List<ConnectionHistory> findByUserIdOrderByConnectedAtDesc(Long userId);
    
    /**
     * Buscar conexiones por rango de tiempo
     */
    @Query("SELECT ch FROM ConnectionHistory ch WHERE ch.connectedAt BETWEEN :startDate AND :endDate ORDER BY ch.connectedAt DESC")
    List<ConnectionHistory> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * Buscar conexiones más largas
     */
    @Query(value = "SELECT * FROM connection_history ch " +
                   "WHERE ch.disconnected_at IS NOT NULL " +
                   "ORDER BY (EXTRACT(EPOCH FROM (ch.disconnected_at - ch.connected_at))) DESC " +
                   "LIMIT 10", 
           nativeQuery = true)
    List<ConnectionHistory> findLongestConnections();
    
    /**
     * Estadísticas por día
     */
    @Query("SELECT CAST(ch.connectedAt AS date), COUNT(ch) FROM ConnectionHistory ch GROUP BY CAST(ch.connectedAt AS date) ORDER BY CAST(ch.connectedAt AS date) DESC")
    List<Object[]> findStatsByDay();
    
    /**
     * Usuarios con más conexiones
     */
    @Query("SELECT ch.userId, COUNT(ch) FROM ConnectionHistory ch GROUP BY ch.userId ORDER BY COUNT(ch) DESC")
    List<Object[]> findUsersWithMostConnections();
}