package com.messaging.web.repository;

import com.messaging.web.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para la entidad User
 * Proporciona métodos de acceso a datos para usuarios
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Buscar usuario por nombre de usuario
     */
    User findByUsername(String username);
    
    /**
     * Buscar usuario por email
     */
    User findByEmail(String email);
    
    /**
     * Contar usuarios por estado
     */
    long countByStatus(User.UserStatus status);
    
    /**
     * Contar usuarios conectados
     */
    long countByConnectedTrue();
    
    /**
     * Buscar usuarios por estado
     */
    List<User> findByStatus(User.UserStatus status);
    
    /**
     * Buscar usuarios conectados
     */
    List<User> findByConnectedTrue();
    
    /**
     * Buscar usuarios recientes
     */
    List<User> findTop10ByOrderByCreatedAtDesc();
    
    /**
     * Buscar usuarios por nombre de usuario (búsqueda parcial)
     */
    List<User> findByUsernameContainingIgnoreCase(String username);
    
    /**
     * Verificar si existe un usuario con el nombre de usuario dado
     */
    boolean existsByUsername(String username);
    
    /**
     * Verificar si existe un usuario con el email dado
     */
    boolean existsByEmail(String email);
    
    /**
     * Buscar usuarios con más conexiones
     */
    @Query("SELECT u FROM User u ORDER BY u.connectionCount DESC")
    List<User> findUsersByConnectionCountDesc();
    
    /**
     * Buscar usuarios que han enviado archivos
     */
    @Query("SELECT u FROM User u WHERE u.filesSentCount > 0 ORDER BY u.filesSentCount DESC")
    List<User> findUsersWithFilesSent();
    
    /**
     * Buscar usuarios activos (conectados recientemente)
     */
    @Query("SELECT u FROM User u WHERE u.lastConnection IS NOT NULL AND u.lastConnection > :oneDayAgo ORDER BY u.lastConnection DESC")
    List<User> findActiveUsers(@Param("oneDayAgo") LocalDateTime oneDayAgo);
}