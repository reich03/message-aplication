-- Script para reinicializar la base de datos con la estructura correcta
-- Ejecutar este script en PostgreSQL para limpiar y recrear las tablas

-- Eliminar tablas existentes (en orden correcto por dependencias)
DROP TABLE IF EXISTS server_logs CASCADE;
DROP TABLE IF EXISTS connection_history CASCADE;
DROP TABLE IF EXISTS active_connections CASCADE;
DROP TABLE IF EXISTS messages CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Eliminar funciones y triggers
DROP FUNCTION IF EXISTS update_user_message_count() CASCADE;
DROP FUNCTION IF EXISTS cleanup_inactive_connections() CASCADE;

-- Recrear tablas con la estructura correcta
CREATE TABLE users (
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

CREATE TABLE messages (
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

CREATE TABLE active_connections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    client_ip VARCHAR(45) NOT NULL,
    connected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    messages_count INTEGER DEFAULT 0
);

CREATE TABLE connection_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    client_ip VARCHAR(45) NOT NULL,
    connected_at TIMESTAMP NOT NULL,
    disconnected_at TIMESTAMP,
    messages_sent INTEGER DEFAULT 0
);

CREATE TABLE server_logs (
    id BIGSERIAL PRIMARY KEY,
    log_level VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,
    user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Crear índices
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_receiver ON messages(receiver_id);
CREATE INDEX idx_messages_sent_at ON messages(sent_at);
CREATE INDEX idx_active_connections_user ON active_connections(user_id);
CREATE INDEX idx_connection_history_user ON connection_history(user_id);

-- Insertar usuario administrador
INSERT INTO users (username, password, email, status, max_connections, max_files_per_day) 
VALUES ('admin', 'admin123', 'admin@messaging.com', 'APPROVED', 5, 50);

-- Insertar algunos usuarios de prueba
INSERT INTO users (username, password, email, status, max_connections, max_files_per_day) 
VALUES 
    ('jhojan', 'jhojan123', 'jhojan@test.com', 'APPROVED', 3, 10),
    ('andres', 'andres123', 'andres@test.com', 'APPROVED', 3, 10),
    ('testuser', 'test123', 'test@test.com', 'APPROVED', 3, 10);

-- Verificar datos insertados
SELECT 'Usuarios creados:' as info;
SELECT id, username, email, status, connected FROM users ORDER BY id;

SELECT 'Base de datos reinicializada correctamente' as status;
