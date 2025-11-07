-- Script de inicialización de la base de datos de mensajería

-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_connection TIMESTAMP,
    connected BOOLEAN DEFAULT FALSE,
    connection_count INTEGER DEFAULT 0,
    max_connections INTEGER DEFAULT 3,
    files_sent_count INTEGER DEFAULT 0,
    max_files_per_day INTEGER DEFAULT 10
);

-- Tabla de mensajes
CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT REFERENCES users(id),
    receiver_id BIGINT REFERENCES users(id),
    message_type VARCHAR(20) NOT NULL,
    content TEXT,
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    file_size BIGINT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE
);

-- Tabla de conexiones activas
CREATE TABLE IF NOT EXISTS active_connections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    client_ip VARCHAR(45) NOT NULL,
    connected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    messages_count INTEGER DEFAULT 0
);

-- Tabla de conexiones históricas
CREATE TABLE IF NOT EXISTS connection_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    client_ip VARCHAR(45) NOT NULL,
    connected_at TIMESTAMP NOT NULL,
    disconnected_at TIMESTAMP,
    messages_sent INTEGER DEFAULT 0
);

-- Tabla de logs del servidor
CREATE TABLE IF NOT EXISTS server_logs (
    id BIGSERIAL PRIMARY KEY,
    log_level VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,
    user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para optimización
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_receiver ON messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_messages_sent_at ON messages(sent_at);
CREATE INDEX IF NOT EXISTS idx_active_connections_user ON active_connections(user_id);
CREATE INDEX IF NOT EXISTS idx_connection_history_user ON connection_history(user_id);

-- Usuario administrador por defecto
INSERT INTO users (username, password, email, status, max_connections, max_files_per_day) 
VALUES ('admin', 'admin123', 'admin@messaging.com', 'APPROVED', 5, 50)
ON CONFLICT (username) DO NOTHING;

-- Función para actualizar estadísticas de mensajes
CREATE OR REPLACE FUNCTION update_user_message_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE users 
    SET last_connection = CURRENT_TIMESTAMP 
    WHERE id = NEW.sender_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger para actualizar estadísticas
DROP TRIGGER IF EXISTS trigger_update_message_count ON messages;
CREATE TRIGGER trigger_update_message_count
    AFTER INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION update_user_message_count();

-- Función para limpiar conexiones inactivas
CREATE OR REPLACE FUNCTION cleanup_inactive_connections()
RETURNS void AS $$
BEGIN
    -- Mover conexiones inactivas a historial
    INSERT INTO connection_history (user_id, client_ip, connected_at, disconnected_at, messages_sent)
    SELECT user_id, client_ip, connected_at, CURRENT_TIMESTAMP, messages_count
    FROM active_connections 
    WHERE connected_at < CURRENT_TIMESTAMP - INTERVAL '1 hour';
    
    -- Eliminar conexiones inactivas
    DELETE FROM active_connections 
    WHERE connected_at < CURRENT_TIMESTAMP - INTERVAL '1 hour';
    
    -- Actualizar estado de usuarios
    UPDATE users 
    SET is_connected = FALSE 
    WHERE id NOT IN (SELECT DISTINCT user_id FROM active_connections);
END;
$$ LANGUAGE plpgsql;
