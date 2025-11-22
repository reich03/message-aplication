# Patrones de Diseño Implementados en el Proyecto

## Resumen Ejecutivo

Este documento describe los patrones de diseño implementados en la aplicación de mensajería distribuida. El proyecto implementa exitosamente los patrones **Object Pool** y **Builder** como requisitos principales, además de otros patrones complementarios que mejoran la arquitectura y mantenibilidad del sistema.

---

## 1. Patrón Object Pool ⭐ (REQUERIDO)

### Implementación Principal: ConnectionPool

**Ubicación**: `server-app/src/main/java/com/messaging/server/pool/ConnectionPool.java`

**Descripción**: 
Implementa un pool de conexiones de clientes para gestionar eficientemente las conexiones activas al servidor de mensajería. Este patrón permite reutilizar objetos costosos de crear y mantener, optimizando el uso de recursos del sistema.

**Características clave**:
- Gestión centralizada de conexiones de clientes activas
- Control de límites de conexiones por usuario
- Thread-safe mediante el uso de `ConcurrentHashMap` y `AtomicInteger`
- Monitoreo de estadísticas de uso
- Limpieza automática de conexiones inactivas
- Prevención de sobrecarga del servidor

**Métodos principales**:
```java
- addConnection(ClientConnection): Agrega conexión al pool con validación de límites
- removeConnection(String): Remueve conexión del pool
- getConnection(String): Obtiene conexión específica
- getUserConnections(Long): Obtiene todas las conexiones de un usuario
- getUserConnectionCount(Long): Cuenta conexiones activas por usuario
- cleanupInactiveConnections(): Limpia conexiones inactivas
- getStatistics(): Retorna estadísticas del pool
- shutdown(): Cierra todas las conexiones limpiamente
```

**Beneficios**:
- Previene desbordamiento de conexiones
- Mejora el rendimiento al reutilizar recursos
- Facilita el monitoreo del sistema
- Implementa límites configurables por usuario

---

### Implementación Secundaria: Database Connection Pool (HikariCP)

**Ubicación**: `server-app/src/main/java/com/messaging/server/config/DatabaseConfig.java`

**Descripción**:
Implementa un pool de conexiones de base de datos usando HikariCP, que es una implementación eficiente del patrón Object Pool para conexiones JDBC.

**Configuración del pool**:
```java
- Maximum Pool Size: 20 conexiones
- Minimum Idle: 5 conexiones
- Connection Timeout: 30 segundos
- Idle Timeout: 10 minutos
- Max Lifetime: 30 minutos
- Leak Detection Threshold: 60 segundos
```

**Beneficios**:
- Reutilización de conexiones costosas a la base de datos
- Mejor rendimiento en operaciones concurrentes
- Detección de fugas de conexiones
- Configuración optimizada para PostgreSQL

---

## 2. Patrón Builder ⭐ (REQUERIDO)

### Implementación: Message.Builder

**Ubicación**: `server-app/src/main/java/com/messaging/server/model/Message.java`

**Descripción**:
Implementa el patrón Builder para la construcción flexible de objetos Message complejos. Este patrón separa la construcción de un objeto complejo de su representación, permitiendo crear diferentes representaciones usando el mismo proceso de construcción.

**Estructura de la clase Message**:
```java
public class Message {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String messageType;
    private String content;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private boolean isRead;
    private String senderUsername;
    private String receiverUsername;
    
    // Constructor privado para el Builder
    private Message(Builder builder) { ... }
    
    // Método factory para obtener Builder
    public static Builder builder() {
        return new Builder();
    }
    
    // Clase interna Builder
    public static class Builder {
        // Campos privados idénticos a Message
        
        // Métodos fluent para cada campo
        public Builder id(Long id) { ... return this; }
        public Builder senderId(Long senderId) { ... return this; }
        public Builder receiverId(Long receiverId) { ... return this; }
        // ... más métodos fluent
        
        // Método build con validaciones
        public Message build() {
            // Validaciones de campos requeridos
            if (senderId == null) {
                throw new IllegalStateException("senderId es requerido");
            }
            if (receiverId == null) {
                throw new IllegalStateException("receiverId es requerido");
            }
            if (messageType == null || messageType.isEmpty()) {
                throw new IllegalStateException("messageType es requerido");
            }
            
            return new Message(this);
        }
    }
}
```

**Ejemplo de uso**:
```java
Message message = Message.builder()
    .senderId(1L)
    .receiverId(2L)
    .messageType("TEXT")
    .content("Hola mundo")
    .sentAt(LocalDateTime.now())
    .isRead(false)
    .build();
```

**Beneficios**:
- Construcción de objetos paso a paso con interfaz fluida
- Validación centralizada en el método `build()`
- Inmutabilidad opcional del objeto construido
- Código más legible y mantenible
- Facilita la creación de objetos con muchos parámetros opcionales
- Previene el "constructor telescópico" (múltiples constructores sobrecargados)

---

## 3. Patrón Singleton (ADICIONAL)

El patrón Singleton asegura que una clase tenga solo una instancia y proporciona un punto de acceso global a ella. Implementado en múltiples clases críticas del sistema.

### 3.1 ConnectionPool Singleton

**Ubicación**: `server-app/src/main/java/com/messaging/server/pool/ConnectionPool.java`

```java
public class ConnectionPool {
    private static volatile ConnectionPool instance;
    
    private ConnectionPool() {
        // Constructor privado
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
}
```

**Tipo**: Double-Checked Locking Singleton (thread-safe)

---

### 3.2 DatabaseConfig Singleton

**Ubicación**: `server-app/src/main/java/com/messaging/server/config/DatabaseConfig.java`

```java
public class DatabaseConfig {
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
}
```

**Beneficios**:
- Única instancia del pool de conexiones de base de datos
- Configuración centralizada
- Evita múltiples inicializaciones costosas

---

### 3.3 ServerConfig Singleton

**Ubicación**: `server-app/src/main/java/com/messaging/server/config/ServerConfig.java`

```java
public class ServerConfig {
    private static volatile ServerConfig instance;
    private final Properties properties;
    
    private ServerConfig() {
        this.properties = new Properties();
        loadConfiguration();
    }
    
    public static ServerConfig getInstance() {
        if (instance == null) {
            synchronized (ServerConfig.class) {
                if (instance == null) {
                    instance = new ServerConfig();
                }
            }
        }
        return instance;
    }
}
```

**Beneficios**:
- Configuración única y global del servidor
- Carga única del archivo de propiedades
- Acceso consistente a configuraciones

---

### 3.4 LoggingService Singleton

**Ubicación**: `server-app/src/main/java/com/messaging/server/service/LoggingService.java`

```java
public class LoggingService {
    private static volatile LoggingService instance;
    
    private LoggingService() {
        // Constructor privado
    }
    
    public static LoggingService getInstance() {
        if (instance == null) {
            synchronized (LoggingService.class) {
                if (instance == null) {
                    instance = new LoggingService();
                }
            }
        }
        return instance;
    }
}
```

**Beneficios**:
- Punto único de logging en toda la aplicación
- Sincronización de escritura en archivos
- Gestión centralizada de logs

---

## 4. Patrón Repository (ADICIONAL)

**Ubicación**: `web-app/src/main/java/com/messaging/web/repository/`

Implementado mediante Spring Data JPA en la aplicación web.

**Clases**:
- `UserRepository.java` - Gestión de usuarios
- `MessageRepository.java` - Gestión de mensajes
- `ConnectionHistoryRepository.java` - Historial de conexiones
- `ConnectionRepository.java` - Conexiones activas

**Beneficios**:
- Abstracción de la capa de persistencia
- Separación de lógica de negocio y acceso a datos
- Facilita el testing mediante mocks
- Implementación automática de CRUD

---

## 5. Patrón Dependency Injection (ADICIONAL)

**Ubicación**: Aplicación `web-app` (Spring Framework)

Implementado mediante las anotaciones de Spring Framework:
- `@Autowired` - Inyección de dependencias
- `@Service` - Servicios de negocio
- `@Repository` - Capa de persistencia
- `@Controller` - Controladores web

**Ejemplo en WebService**:
```java
@Service
public class WebService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private ConnectionHistoryRepository connectionHistoryRepository;
    
    @Autowired
    private ConnectionRepository connectionRepository;
}
```

**Beneficios**:
- Bajo acoplamiento entre componentes
- Facilita el testing con mocks
- Gestión automática del ciclo de vida de objetos
- Configuración centralizada

---

## 6. Patrón MVC (Model-View-Controller) (ADICIONAL)

### 6.1 En la Aplicación Web (Spring MVC)

**Ubicación**: `web-app/src/main/java/com/messaging/web/`

- **Model**: Paquete `model/` - Entidades JPA (User, Message, etc.)
- **View**: `resources/templates/` - Plantillas Thymeleaf (HTML)
- **Controller**: `controller/WebController.java` - Lógica de presentación

### 6.2 En la Aplicación Cliente (JavaFX)

**Ubicación**: `client-app/src/main/java/com/messaging/client/`

- **Model**: Paquete `model/` - User, Message
- **View**: `resources/fxml/main.fxml` - Interfaz gráfica FXML
- **Controller**: `controller/MainController.java` - Lógica de UI

**Beneficios**:
- Separación clara de responsabilidades
- Facilita el mantenimiento
- Reutilización de componentes
- Testing independiente de capas

---

## 7. Patrón Service Layer (ADICIONAL)

**Ubicación**: 
- `server-app/src/main/java/com/messaging/server/service/`
- `web-app/src/main/java/com/messaging/web/service/`
- `client-app/src/main/java/com/messaging/client/service/`

**Clases principales**:
- `ClientHandlerService` - Manejo de clientes en el servidor
- `UserService` - Lógica de negocio de usuarios
- `WebService` - Servicios web para administración
- `NetworkService` - Comunicación cliente-servidor

**Beneficios**:
- Encapsulación de lógica de negocio
- Reutilización de operaciones comunes
- Transaccionalidad en operaciones
- Punto único de entrada para operaciones

---

## 8. Patrón DAO (Data Access Object) Implícito (ADICIONAL)

Implementado implícitamente en:
- `UserService.java` (server-app) - Acceso directo a base de datos
- Spring Data JPA Repositories (web-app)

**Beneficios**:
- Abstracción del acceso a datos
- Facilita cambios en la capa de persistencia
- Código más limpio y mantenible

---

## Resumen de Patrones Implementados

| Patrón | Ubicación Principal | Estado | Propósito |
|--------|-------------------|--------|-----------|
| **Object Pool** ⭐ | `ConnectionPool.java`, `DatabaseConfig.java` | ✅ IMPLEMENTADO | Gestión eficiente de recursos reutilizables |
| **Builder** ⭐ | `Message.java` | ✅ IMPLEMENTADO | Construcción flexible de objetos complejos |
| **Singleton** | `ConnectionPool.java`, `DatabaseConfig.java`, `ServerConfig.java`, `LoggingService.java` | ✅ IMPLEMENTADO | Instancia única global |
| **Repository** | `web-app/repository/` | ✅ IMPLEMENTADO | Abstracción de persistencia |
| **Dependency Injection** | `web-app` (Spring) | ✅ IMPLEMENTADO | Inversión de control |
| **MVC** | `web-app`, `client-app` | ✅ IMPLEMENTADO | Separación de capas |
| **Service Layer** | Todas las aplicaciones | ✅ IMPLEMENTADO | Lógica de negocio |
| **DAO** | `UserService`, Repositories | ✅ IMPLEMENTADO | Acceso a datos |

---

## Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENT APPLICATION                      │
│  ┌─────────┐  ┌──────────┐  ┌────────────┐                 │
│  │  View   │──│Controller│──│  Service   │                 │
│  │ (FXML)  │  │  (MVC)   │  │  Layer     │                 │
│  └─────────┘  └──────────┘  └────────────┘                 │
└─────────────────────────┬───────────────────────────────────┘
                          │
                    (TCP Socket)
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                      SERVER APPLICATION                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         ConnectionPool (Object Pool + Singleton)     │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌────────────┐  ┌────────────┐  ┌───────────────────┐     │
│  │  Service   │  │   Config   │  │     Logging       │     │
│  │   Layer    │  │ (Singleton)│  │   (Singleton)     │     │
│  └────────────┘  └────────────┘  └───────────────────┘     │
│  ┌──────────────────────────────────────────────────────┐   │
│  │    DatabaseConfig (Pool + Singleton) - HikariCP      │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                          │
                    (JDBC/HikariCP)
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    POSTGRESQL DATABASE                       │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                      WEB APPLICATION (Admin)                  │
│  ┌─────────┐  ┌──────────┐  ┌─────────┐  ┌──────────────┐   │
│  │  View   │──│Controller│──│ Service │──│ Repository   │   │
│  │(Thymeleaf)││  (MVC)   │  │  Layer  │  │    (DAO)     │   │
│  └─────────┘  └──────────┘  └─────────┘  └──────────────┘   │
│                                              │                │
│                    (Spring Framework)        │                │
│              (Dependency Injection)          │                │
└──────────────────────────────────────────────┼────────────────┘
                                               │
                                          (JPA/JDBC)
                                               │
┌──────────────────────────────────────────────▼────────────────┐
│                    POSTGRESQL DATABASE                         │
└────────────────────────────────────────────────────────────────┘
```

---

## Justificación de Patrones

### ¿Por qué Object Pool?
El patrón Object Pool es crítico en aplicaciones con múltiples conexiones simultáneas:
- Evita crear/destruir objetos costosos repetidamente
- Limita el uso de recursos del sistema
- Mejora el rendimiento significativamente
- Facilita el control de concurrencia

### ¿Por qué Builder?
El patrón Builder es ideal para objetos con muchos campos:
- La clase Message tiene 13 campos
- Algunos campos son opcionales (fileName, filePath, etc.)
- Evita constructores con muchos parámetros
- Facilita validaciones antes de construcción
- Mejora la legibilidad del código

### ¿Por qué Singleton?
Necesario para componentes que deben tener una sola instancia:
- ConnectionPool: Una única instancia del pool de conexiones
- DatabaseConfig: Una única configuración de BD
- ServerConfig: Una única configuración del servidor
- LoggingService: Un único punto de logging

---

## Conclusión

El proyecto implementa exitosamente los patrones de diseño requeridos (**Object Pool** y **Builder**) además de múltiples patrones adicionales que mejoran la arquitectura general del sistema:

✅ **Patrones Requeridos Implementados**:
- Object Pool (ConnectionPool + HikariCP Database Pool)
- Builder (Message.Builder)

✅ **Patrones Adicionales para Mejorar Arquitectura**:
- Singleton (4 implementaciones)
- Repository (Spring Data JPA)
- Dependency Injection (Spring Framework)
- MVC (Web y Cliente)
- Service Layer (Todas las apps)
- DAO (Implícito)

El sistema demuestra un diseño robusto, escalable y mantenible siguiendo las mejores prácticas de ingeniería de software.

---

## Referencias

- **Object Pool Pattern**: Gang of Four - Design Patterns
- **Builder Pattern**: Effective Java by Joshua Bloch
- **Singleton Pattern**: Gang of Four - Design Patterns
- **Repository Pattern**: Martin Fowler - Patterns of Enterprise Application Architecture
- **Dependency Injection**: Spring Framework Documentation
- **MVC Pattern**: Model-View-Controller - Sun Microsystems

---

**Fecha de documentación**: Noviembre 22, 2025  
**Versión del proyecto**: 1.0  
**Autor**: Sistema de Mensajería Distribuida - Equipo de Desarrollo
