package com.messaging.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Configuración de base de datos usando patrón Singleton
 * Implementa Object Pool para conexiones de base de datos
 */
public class DatabaseConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static volatile DatabaseConfig instance;
    private HikariDataSource dataSource;
    
    private DatabaseConfig() {
        initializeDataSource();
    }
    
    public static DatabaseConfig getInstance() {
        if (instance == null) {
            synchronized (DatabaseConfig.class) {
                if (instance == null) {
                    instance = new DatabaseConfig();
                }
            }
        }
        return instance;
    }
    
    private void initializeDataSource() {
        try {
            HikariConfig config = new HikariConfig();
            
            // Configuración desde variables de entorno o valores por defecto
            String dbUrl = System.getenv("DB_URL") != null ? 
                System.getenv("DB_URL") : "jdbc:postgresql://localhost:5432/messaging_app";
            String dbUser = System.getenv("DB_USER") != null ? 
                System.getenv("DB_USER") : "messaging_user";
            String dbPassword = System.getenv("DB_PASSWORD") != null ? 
                System.getenv("DB_PASSWORD") : "messaging_pass";
            
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);
            
            // Configuración del pool de conexiones
            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setLeakDetectionThreshold(60000);
            
            // Configuración específica de PostgreSQL
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            
            dataSource = new HikariDataSource(config);
            
            logger.info("Pool de conexiones de base de datos inicializado correctamente");
            
        } catch (Exception e) {
            logger.error("Error inicializando pool de conexiones: " + e.getMessage());
            throw new RuntimeException("No se pudo inicializar la base de datos", e);
        }
    }
    
    public void initialize() {
        // Verificar conexión
        try (Connection connection = getConnection()) {
            logger.info("Conexión a base de datos establecida correctamente");
        } catch (SQLException e) {
            logger.error("Error verificando conexión a base de datos: " + e.getMessage());
            throw new RuntimeException("No se pudo conectar a la base de datos", e);
        }
    }
    
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource no está disponible");
        }
        return dataSource.getConnection();
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Pool de conexiones cerrado correctamente");
        }
    }
    
    public boolean isHealthy() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5);
        } catch (SQLException e) {
            logger.error("Error verificando salud de la base de datos: " + e.getMessage());
            return false;
        }
    }
}
