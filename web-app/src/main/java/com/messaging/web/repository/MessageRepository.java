package com.messaging.web.repository;

import com.messaging.web.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para la entidad Message
 * Proporciona métodos de acceso a datos para mensajes
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    /**
     * Buscar mensajes por tipo
     */
    List<Message> findByMessageTypeOrderBySentAtDesc(Message.MessageType messageType, Pageable pageable);
    
    /**
     * Contar mensajes por remitente
     */
    long countBySenderId(Long senderId);
    
    /**
     * Contar mensajes por destinatario
     */
    long countByReceiverId(Long receiverId);
    
    /**
     * Contar mensajes por tipo
     */
    long countByMessageType(Message.MessageType messageType);
    
    /**
     * Contar mensajes enviados después de una fecha
     */
    long countBySentAtAfter(LocalDateTime dateTime);
    
    /**
     * Buscar mensajes entre dos usuarios
     */
    @Query("SELECT m FROM Message m WHERE (m.senderId = :userId1 AND m.receiverId = :userId2) OR (m.senderId = :userId2 AND m.receiverId = :userId1) ORDER BY m.sentAt DESC")
    List<Message> findMessagesBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    /**
     * Buscar mensajes de un usuario específico
     */
    @Query("SELECT m FROM Message m WHERE m.senderId = :userId OR m.receiverId = :userId ORDER BY m.sentAt DESC")
    List<Message> findMessagesByUserId(@Param("userId") Long userId);
    
    /**
     * Buscar mensajes enviados por un usuario
     */
    @Query("SELECT m FROM Message m WHERE m.senderId = :userId ORDER BY m.sentAt DESC")
    List<Message> findSentMessagesByUserId(@Param("userId") Long userId);
    
    /**
     * Buscar mensajes recibidos por un usuario
     */
    @Query("SELECT m FROM Message m WHERE m.receiverId = :userId ORDER BY m.sentAt DESC")
    List<Message> findReceivedMessagesByUserId(@Param("userId") Long userId);
    
    /**
     * Buscar mensajes no leídos de un usuario
     */
    @Query("SELECT m FROM Message m WHERE m.receiverId = :userId AND m.isRead = false ORDER BY m.sentAt DESC")
    List<Message> findUnreadMessagesByUserId(@Param("userId") Long userId);
    
    /**
     * Buscar mensajes de archivos
     */
    @Query("SELECT m FROM Message m WHERE m.messageType = 'FILE' ORDER BY m.sentAt DESC")
    List<Message> findFileMessages();
    
    /**
     * Buscar mensajes recientes
     */
    @Query("SELECT m FROM Message m ORDER BY m.sentAt DESC")
    Page<Message> findRecentMessages(Pageable pageable);
    
    /**
     * Buscar usuario con más mensajes enviados
     */
    @Query("SELECT u.username, COUNT(m) as messageCount FROM Message m JOIN User u ON m.senderId = u.id GROUP BY u.id, u.username ORDER BY messageCount DESC")
    List<Object[]> findTopMessageSender();
    
    /**
     * Buscar archivos por tamaño
     */
    @Query("SELECT m.fileName, m.fileSize, u.username, m.sentAt FROM Message m JOIN User u ON m.senderId = u.id WHERE m.messageType = 'FILE' ORDER BY m.fileSize DESC")
    List<Object[]> findFilesBySize();
    
    /**
     * Buscar mensajes enviados por usuario (para API REST)
     */
    @Query("SELECT '127.0.0.1' as userIp, '127.0.0.1' as remoteUserIp, CAST(m.sentAt AS date) as date, CAST(m.sentAt AS time) as time FROM Message m WHERE m.senderId = :userId ORDER BY m.sentAt DESC")
    List<Object[]> findSentMessagesForAPI(@Param("userId") Long userId);
    
    /**
     * Buscar mensajes recibidos por usuario (para API REST)
     */
    @Query("SELECT '127.0.0.1' as userIp, '127.0.0.1' as remoteUserIp, CAST(m.sentAt AS date) as date, CAST(m.sentAt AS time) as time FROM Message m WHERE m.receiverId = :userId ORDER BY m.sentAt DESC")
    List<Object[]> findReceivedMessagesForAPI(@Param("userId") Long userId);
    
    /**
     * Buscar mensajes por rango de fechas
     */
    @Query("SELECT m FROM Message m WHERE m.sentAt BETWEEN :startDate AND :endDate ORDER BY m.sentAt DESC")
    List<Message> findMessagesByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Buscar mensajes con contenido específico
     */
    @Query("SELECT m FROM Message m WHERE LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY m.sentAt DESC")
    List<Message> findMessagesByContent(@Param("searchTerm") String searchTerm);
    
    /**
     * Estadísticas de mensajes por día
     */
    @Query("SELECT CAST(m.sentAt AS date) as date, COUNT(m) as messageCount FROM Message m GROUP BY CAST(m.sentAt AS date) ORDER BY date DESC")
    List<Object[]> findMessageStatsByDay();
    
    /**
     * Estadísticas de mensajes por tipo
     */
    @Query("SELECT m.messageType, COUNT(m) as messageCount FROM Message m GROUP BY m.messageType")
    List<Object[]> findMessageStatsByType();
    
    /**
     * Buscar todos los mensajes ordenados por fecha descendente
     */
    @Query("SELECT m FROM Message m ORDER BY m.sentAt DESC")
    List<Message> findAllByOrderBySentAtDesc(Pageable pageable);
}
