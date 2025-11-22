# Ejemplos Prácticos de Uso de Patrones de Diseño

Este documento complementa el archivo `PATRONES_DE_DISEÑO.md` con ejemplos prácticos de cómo se utilizan los patrones implementados en el código real del proyecto.

---

## 1. Patrón Builder - Ejemplos de Uso

### Ejemplo 1: Creación de mensaje de texto simple

**Ubicación**: `UserService.java` - Línea 201

```java
// Construcción de mensaje usando Builder con validaciones automáticas
Message message = Message.builder()
    .senderId(1L)
    .receiverId(2L)
    .messageType("TEXT")
    .content("Hola, ¿cómo estás?")
    .isRead(false)
    .build();
```

**Ventajas demostradas**:
- Código legible y autoexplicativo
- No es necesario recordar el orden de los parámetros
- Validación automática en el método `build()`

---

### Ejemplo 2: Creación de mensaje con archivo

```java
Message fileMessage = Message.builder()
    .senderId(userId)
    .receiverId(receiverId)
    .messageType("FILE")
    .fileName("documento.pdf")
    .filePath("/uploads/1/documento.pdf")
    .fileSize(1024000L) // 1MB
    .content("Archivo adjunto")
    .sentAt(LocalDateTime.now())
    .isRead(false)
    .build();
```

**Ventajas demostradas**:
- Claridad en qué representa cada valor
- Fácil omitir campos opcionales
- Construcción paso a paso

---

### Ejemplo 3: Reconstrucción desde base de datos

**Ubicación**: `UserService.java` - Método `getMessagesBetweenUsers()`

```java
Message message = Message.builder()
    .id(rs.getLong("id"))
    .senderId(rs.getLong("sender_id"))
    .receiverId(rs.getLong("receiver_id"))
    .content(rs.getString("content"))
    .messageType(rs.getString("message_type"))
    .fileName(rs.getString("file_name"))
    .createdAt(rs.getTimestamp("sent_at").toLocalDateTime())
    .senderUsername(rs.getString("sender_username"))
    .receiverUsername(rs.getString("receiver_username"))
    .isRead(false)
    .build();
```

**Ventajas demostradas**:
- Mapeo claro desde ResultSet
- Fácil de leer y mantener
- Validación automática de campos requeridos

---

### Ejemplo 4: Construcción parcial con valores por defecto

```java
Message notification = Message.builder()
    .senderId(0L) // Sistema
    .receiverId(userId)
    .messageType("NOTIFICATION")
    .content("Tu sesión expirará en 5 minutos")
    // sentAt se establece automáticamente a LocalDateTime.now()
    // isRead se establece automáticamente a false
    .build();
```

**Ventajas demostradas**:
- Valores por defecto inteligentes
- Solo se especifican campos necesarios

---

## 2. Patrón Object Pool - Ejemplos de Uso

### Ejemplo 1: Agregar conexión al pool

**Ubicación**: `ClientHandlerService.java` - Método `run()`

```java
ConnectionPool connectionPool = ConnectionPool.getInstance();
ClientConnection clientConnection = new ClientConnection(
    socket, 
    userId, 
    username, 
    maxConnections
);

// Intenta agregar la conexión al pool
if (!connectionPool.addConnection(clientConnection)) {
    // El pool rechaza la conexión si el usuario alcanzó su límite
    sendResponse("CONNECTION_LIMIT", 
        "Límite de conexiones alcanzado para este usuario");
    socket.close();
    return;
}

// Conexión agregada exitosamente
logger.info("Conexión agregada al pool para usuario: " + username);
```

**Ventajas demostradas**:
- Control automático de límites
- Validación antes de agregar
- Thread-safe

---

### Ejemplo 2: Obtener conexiones de usuario

```java
ConnectionPool pool = ConnectionPool.getInstance();
Long userId = 1L;

// Obtener todas las conexiones activas de un usuario
Set<ClientConnection> userConnections = pool.getUserConnections(userId);

// Enviar mensaje a todas las conexiones del usuario
for (ClientConnection conn : userConnections) {
    conn.sendMessage("NOTIFICATION", "Nuevo mensaje recibido");
}
```

**Ventajas demostradas**:
- Búsqueda eficiente de conexiones
- Acceso centralizado
- Operaciones en todas las conexiones de un usuario

---

### Ejemplo 3: Monitoreo del pool

```java
ConnectionPool pool = ConnectionPool.getInstance();

// Obtener estadísticas del pool
Map<String, Object> stats = pool.getStatistics();

System.out.println("Conexiones totales: " + stats.get("totalConnections"));
System.out.println("Usuarios únicos: " + stats.get("uniqueUsers"));
System.out.println("Promedio conexiones/usuario: " + 
    stats.get("averageConnectionsPerUser"));

// Verificar si un usuario puede conectarse
Long userId = 5L;
int maxConnections = 3;
boolean canConnect = pool.canUserConnect(userId, maxConnections);

if (canConnect) {
    System.out.println("Usuario puede establecer nueva conexión");
} else {
    System.out.println("Usuario ha alcanzado el límite de conexiones");
}
```

**Ventajas demostradas**:
- Métricas en tiempo real
- Validación de límites
- Información para administración

---

### Ejemplo 4: Limpieza de conexiones inactivas

```java
// En un thread de mantenimiento
ConnectionPool pool = ConnectionPool.getInstance();

// Ejecutar periódicamente (ej: cada 5 minutos)
pool.cleanupInactiveConnections();
```

**Ventajas demostradas**:
- Liberación automática de recursos
- Prevención de fugas de memoria
- Mantenimiento del pool

---

### Ejemplo 5: Database Connection Pool (HikariCP)

**Ubicación**: `DatabaseConfig.java`

```java
// Obtener instancia única de configuración
DatabaseConfig dbConfig = DatabaseConfig.getInstance();

// Obtener conexión del pool
try (Connection conn = dbConfig.getConnection()) {
    // La conexión es automáticamente devuelta al pool al salir del try
    
    PreparedStatement stmt = conn.prepareStatement(
        "SELECT * FROM users WHERE id = ?"
    );
    stmt.setLong(1, userId);
    
    ResultSet rs = stmt.executeQuery();
    // Procesar resultados...
    
} catch (SQLException e) {
    logger.error("Error de base de datos: " + e.getMessage());
}
// La conexión se devuelve automáticamente al pool aquí
```

**Ventajas demostradas**:
- Reutilización de conexiones
- Try-with-resources automático
- Pool gestionado por HikariCP

---

## 3. Patrón Singleton - Ejemplos de Uso

### Ejemplo 1: Singleton thread-safe con double-checked locking

**Ubicación**: `ConnectionPool.java`, `DatabaseConfig.java`, `ServerConfig.java`

```java
public class ConnectionPool {
    private static volatile ConnectionPool instance;
    
    private ConnectionPool() {
        // Constructor privado impide instanciación externa
    }
    
    public static ConnectionPool getInstance() {
        if (instance == null) {                          // Primera verificación
            synchronized (ConnectionPool.class) {         // Sincronización
                if (instance == null) {                  // Segunda verificación
                    instance = new ConnectionPool();     // Creación única
                }
            }
        }
        return instance;
    }
}
```

**Uso en código**:
```java
// Siempre devuelve la misma instancia
ConnectionPool pool1 = ConnectionPool.getInstance();
ConnectionPool pool2 = ConnectionPool.getInstance();

// pool1 == pool2 es true
assert pool1 == pool2;
```

**Ventajas demostradas**:
- Thread-safe
- Lazy initialization
- Una sola instancia garantizada

---

### Ejemplo 2: Uso de ServerConfig Singleton

**Ubicación**: `MessagingServer.java`

```java
public class MessagingServer {
    private final ServerConfig serverConfig;
    
    public MessagingServer() {
        // Obtener instancia única de configuración
        this.serverConfig = ServerConfig.getInstance();
        
        // Usar configuración en toda la aplicación
        int port = serverConfig.getPort();
        int maxConnections = serverConfig.getMaxConnections();
        
        logger.info("Servidor iniciando en puerto: " + port);
        logger.info("Máximo de conexiones: " + maxConnections);
    }
}
```

**Ventajas demostradas**:
- Configuración centralizada
- Acceso consistente
- Carga única del archivo de propiedades

---

### Ejemplo 3: LoggingService Singleton

**Ubicación**: Usado en múltiples clases

```java
// En cualquier clase del sistema
LoggingService logger = LoggingService.getInstance();

logger.info("Servidor iniciado correctamente");
logger.warn("Conexión lenta detectada");
logger.error("Error conectando a base de datos");
logger.debug("Procesando mensaje ID: 123");
```

**Ventajas demostradas**:
- Punto único de logging
- Configuración centralizada
- Sincronización automática de escrituras

---

## 4. Comparación: Antes y Después del Refactoring

### Antes (Sin Builder)

```java
// Código difícil de leer y mantener
Message message = new Message();
message.setId(rs.getLong("id"));
message.setSenderId(rs.getLong("sender_id"));
message.setReceiverId(rs.getLong("receiver_id"));
message.setContent(rs.getString("content"));
message.setMessageType(rs.getString("message_type"));
message.setFileName(rs.getString("file_name"));
message.setCreatedAt(rs.getTimestamp("sent_at").toLocalDateTime());
message.setSenderUsername(rs.getString("sender_username"));
message.setReceiverUsername(rs.getString("receiver_username"));
```

**Problemas**:
- Múltiples líneas de código
- Difícil de leer
- No hay validación hasta después de setear todos los campos
- Fácil olvidar setear un campo importante

---

### Después (Con Builder)

```java
// Código limpio y expresivo
Message message = Message.builder()
    .id(rs.getLong("id"))
    .senderId(rs.getLong("sender_id"))
    .receiverId(rs.getLong("receiver_id"))
    .content(rs.getString("content"))
    .messageType(rs.getString("message_type"))
    .fileName(rs.getString("file_name"))
    .createdAt(rs.getTimestamp("sent_at").toLocalDateTime())
    .senderUsername(rs.getString("sender_username"))
    .receiverUsername(rs.getString("receiver_username"))
    .isRead(false)
    .build(); // Validación automática aquí
```

**Mejoras**:
- Más legible y expresivo
- Interfaz fluida (method chaining)
- Validación automática en `build()`
- Inmutable después de construcción
- Fácil identificar qué campo es cuál

---

## 5. Casos de Uso Completos

### Caso de Uso 1: Usuario enviando mensaje de texto

```java
// 1. Obtener instancias Singleton
ConnectionPool pool = ConnectionPool.getInstance();
DatabaseConfig dbConfig = DatabaseConfig.getInstance();

// 2. Construir mensaje con Builder
Message textMessage = Message.builder()
    .senderId(currentUserId)
    .receiverId(recipientId)
    .messageType("TEXT")
    .content("Hola! ¿Cómo estás?")
    .sentAt(LocalDateTime.now())
    .isRead(false)
    .build();

// 3. Guardar en BD usando pool de conexiones
try (Connection conn = dbConfig.getConnection()) {
    String sql = "INSERT INTO messages (sender_id, receiver_id, message_type, " +
                "content, sent_at) VALUES (?, ?, ?, ?, ?)";
    
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setLong(1, textMessage.getSenderId());
        stmt.setLong(2, textMessage.getReceiverId());
        stmt.setString(3, textMessage.getMessageType());
        stmt.setString(4, textMessage.getContent());
        stmt.setTimestamp(5, Timestamp.valueOf(textMessage.getSentAt()));
        
        stmt.executeUpdate();
    }
}

// 4. Notificar al receptor si está conectado
Set<ClientConnection> recipientConnections = pool.getUserConnections(recipientId);
for (ClientConnection conn : recipientConnections) {
    conn.sendMessage("NEW_MESSAGE", "Tienes un nuevo mensaje");
}
```

**Patrones utilizados**:
- ✅ Singleton (ConnectionPool, DatabaseConfig)
- ✅ Builder (Message construction)
- ✅ Object Pool (Database connections, Client connections)

---

### Caso de Uso 2: Servidor procesando múltiples conexiones

```java
public class MessagingServer {
    public void start() {
        // Singleton: Configuración única
        ServerConfig config = ServerConfig.getInstance();
        DatabaseConfig dbConfig = DatabaseConfig.getInstance();
        ConnectionPool connectionPool = ConnectionPool.getInstance();
        
        // Inicializar servidor
        int port = config.getPort();
        ServerSocket serverSocket = new ServerSocket(port);
        
        while (running) {
            Socket clientSocket = serverSocket.accept();
            
            // Verificar si se puede aceptar la conexión
            int activeConnections = connectionPool.getActiveConnections();
            int maxConnections = config.getMaxConnections();
            
            if (activeConnections >= maxConnections) {
                // Rechazar conexión
                clientSocket.close();
                continue;
            }
            
            // Crear handler en nuevo thread
            ClientHandlerService handler = new ClientHandlerService(
                clientSocket,
                userService,
                loggingService,
                connectionPool
            );
            
            executorService.submit(handler);
        }
    }
}
```

**Patrones utilizados**:
- ✅ Singleton (3 instancias)
- ✅ Object Pool (ConnectionPool)
- ✅ Service Layer (ClientHandlerService)

---

## 6. Mejores Prácticas Implementadas

### Builder Pattern
✅ Constructor privado  
✅ Método factory estático `builder()`  
✅ Interfaz fluida (method chaining)  
✅ Validación en método `build()`  
✅ Campos inmutables después de construcción  

### Object Pool Pattern
✅ Control de límites  
✅ Thread-safe (ConcurrentHashMap)  
✅ Estadísticas y monitoreo  
✅ Limpieza automática  
✅ Gestión eficiente de recursos  

### Singleton Pattern
✅ Constructor privado  
✅ Double-checked locking  
✅ Palabra clave `volatile`  
✅ Thread-safe  
✅ Lazy initialization  

---

## 7. Testing de Patrones

### Test de Builder

```java
@Test
public void testMessageBuilder() {
    // Test construcción exitosa
    Message message = Message.builder()
        .senderId(1L)
        .receiverId(2L)
        .messageType("TEXT")
        .content("Test")
        .build();
    
    assertNotNull(message);
    assertEquals(1L, message.getSenderId());
    assertEquals(2L, message.getReceiverId());
}

@Test(expected = IllegalStateException.class)
public void testBuilderValidation() {
    // Test validación de campos requeridos
    Message.builder()
        .content("Test")
        .build(); // Debería lanzar excepción
}
```

### Test de Object Pool

```java
@Test
public void testConnectionPool() {
    ConnectionPool pool = ConnectionPool.getInstance();
    
    ClientConnection conn1 = createTestConnection(1L);
    boolean added = pool.addConnection(conn1);
    
    assertTrue(added);
    assertEquals(1, pool.getActiveConnections());
    
    pool.removeConnection(conn1.getConnectionId());
    assertEquals(0, pool.getActiveConnections());
}
```

### Test de Singleton

```java
@Test
public void testSingletonInstance() {
    ConnectionPool pool1 = ConnectionPool.getInstance();
    ConnectionPool pool2 = ConnectionPool.getInstance();
    
    assertSame(pool1, pool2);
}
```

---

## Conclusión

Este documento demuestra que los patrones de diseño no solo están implementados teóricamente, sino que son utilizados activamente en el código del proyecto. Cada patrón resuelve problemas reales y mejora la calidad del software:

- **Builder**: Facilita construcción de objetos complejos
- **Object Pool**: Optimiza uso de recursos
- **Singleton**: Garantiza instancia única de componentes críticos

El código refactorizado es más limpio, mantenible y sigue las mejores prácticas de la industria.
