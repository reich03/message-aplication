-- Script para verificar y aprobar usuarios
-- Conectar a la base de datos y ejecutar estos comandos

-- Ver usuarios existentes
SELECT id, username, email, status, created_at FROM users ORDER BY created_at DESC;

-- Aprobar todos los usuarios pendientes
UPDATE users SET status = 'APPROVED' WHERE status = 'PENDING';

-- Verificar usuarios aprobados
SELECT id, username, email, status FROM users WHERE status = 'APPROVED';

-- Ver conexiones activas
SELECT ac.id, u.username, ac.client_ip, ac.connected_at 
FROM active_connections ac 
JOIN users u ON ac.user_id = u.id 
ORDER BY ac.connected_at DESC;

-- Ver mensajes recientes
SELECT m.id, s.username as sender, r.username as receiver, m.content, m.sent_at
FROM messages m
JOIN users s ON m.sender_id = s.id
JOIN users r ON m.receiver_id = r.id
ORDER BY m.sent_at DESC
LIMIT 10;
