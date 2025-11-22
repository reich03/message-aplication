# 🎤 Guía de Presentación - Patrones de Diseño

## ⏱️ Presentación de 10-15 Minutos

---

## 📋 Agenda Sugerida

| Sección | Tiempo | Contenido |
|---------|--------|-----------|
| Introducción | 1-2 min | Presentación del proyecto |
| Object Pool | 4-5 min | Demostración del patrón |
| Builder | 4-5 min | Demostración del patrón |
| Bonus | 2-3 min | Patrones adicionales |
| Conclusión | 1 min | Resumen y preguntas |

---

## 🎯 PARTE 1: Introducción (1-2 min)

### Qué decir:

> "Buenos días/tardes. Voy a presentar el proyecto de aplicación de mensajería, donde implementamos los patrones de diseño **Object Pool** y **Builder** como requisitos principales, además de varios patrones adicionales.
>
> El proyecto es una aplicación de mensajería cliente-servidor que incluye:
> - Servidor TCP en Java
> - Cliente desktop con JavaFX
> - Aplicación web de administración con Spring Boot
> - Base de datos PostgreSQL
>
> Los patrones están implementados en código real y funcional, no son ejemplos aislados."

### Material de apoyo:
- Mostrar README.md brevemente
- Mencionar que hay documentación completa de 275,000+ caracteres

---

## 🎯 PARTE 2: Patrón Object Pool (4-5 min)

### Slide 1: ¿Qué es Object Pool? (30 seg)

> "El patrón Object Pool gestiona un conjunto de objetos reutilizables para evitar la creación y destrucción costosa de recursos. Es especialmente útil para conexiones de red o bases de datos."

### Slide 2: Nuestra Implementación (1 min)

> "Implementamos Object Pool en DOS lugares:
>
> **1. ConnectionPool** - Para conexiones de clientes TCP
> - Ubicación: `server-app/pool/ConnectionPool.java`
> - 151 líneas de código
> - Gestiona conexiones activas de usuarios
> - Limita conexiones por usuario
> - Thread-safe con ConcurrentHashMap
>
> **2. DatabaseConfig** - Para conexiones de base de datos
> - Usa HikariCP, una implementación profesional de Object Pool
> - Pool de 20 conexiones máximo, 5 mínimo
> - Reutiliza conexiones costosas a PostgreSQL"

### Slide 3: Código - ConnectionPool (2 min)

**ABRIR ARCHIVO**: `server-app/src/main/java/com/messaging/server/pool/ConnectionPool.java`

**MOSTRAR** (líneas 28-37):
```java
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
```

> "Este es el patrón Singleton que garantiza una única instancia del pool."

**MOSTRAR** (líneas 39-58):
```java
public boolean addConnection(ClientConnection connection) {
    String connectionId = connection.getConnectionId();
    Long userId = connection.getUserId();
    
    int maxConnections = connection.getMaxConnections();
    int currentConnections = getUserConnectionCount(userId);
    
    if (currentConnections >= maxConnections) {
        logger.warn("Usuario {} ha alcanzado el límite...", userId);
        return false;
    }
    
    activeConnections.put(connectionId, connection);
    userConnectionCounts.computeIfAbsent(userId, k -> new AtomicInteger(0))
        .incrementAndGet();
    totalConnections.incrementAndGet();
    
    return true;
}
```

> "Aquí vemos:
> - Validación de límites por usuario
> - Agregado al pool usando ConcurrentHashMap
> - Contadores thread-safe con AtomicInteger
> - Control de acceso concurrente"

### Slide 4: Uso Real del Pool (1 min)

**ABRIR ARCHIVO**: `server-app/src/main/java/com/messaging/server/service/ClientHandlerService.java`

**MOSTRAR** (línea 87):
```java
if (!connectionPool.addConnection(clientConnection)) {
    sendResponse("CONNECTION_LIMIT", "Límite alcanzado");
    socket.close();
    return;
}
```

> "El pool se usa realmente en la aplicación para gestionar conexiones de clientes."

**Mostrar diagrama** del `DIAGRAMAS_PATRONES.md` (opcional)

---

## 🎯 PARTE 3: Patrón Builder (4-5 min)

### Slide 1: ¿Qué es Builder? (30 seg)

> "El patrón Builder facilita la construcción de objetos complejos paso a paso, especialmente cuando tienen muchos parámetros. Proporciona una interfaz fluida y validaciones automáticas."

### Slide 2: Nuestra Implementación (1 min)

> "Implementamos Builder en la clase **Message** que tiene 13 campos diferentes:
> - Ubicación: `server-app/model/Message.java`
> - 270 líneas de código
> - Constructor privado
> - Clase interna Builder
> - Validación automática
> - Interfaz fluida"

### Slide 3: Código - Message.Builder (2 min)

**ABRIR ARCHIVO**: `server-app/src/main/java/com/messaging/server/model/Message.java`

**MOSTRAR** (líneas 40-53):
```java
// Constructor privado - solo Builder puede crear instancias
private Message(Builder builder) {
    this.id = builder.id;
    this.senderId = builder.senderId;
    this.receiverId = builder.receiverId;
    this.messageType = builder.messageType;
    this.content = builder.content;
    // ... más campos
}

// Método factory
public static Builder builder() {
    return new Builder();
}
```

**MOSTRAR** (líneas 178-210):
```java
public static class Builder {
    private Long senderId;
    private Long receiverId;
    // ... más campos
    
    public Builder senderId(Long senderId) {
        this.senderId = senderId;
        return this;  // ← Interfaz fluida
    }
    
    public Builder receiverId(Long receiverId) {
        this.receiverId = receiverId;
        return this;  // ← Interfaz fluida
    }
    // ... más métodos
}
```

**MOSTRAR** (líneas 252-262):
```java
public Message build() {
    // Validaciones
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
```

> "El método build() valida campos requeridos antes de crear el objeto."

### Slide 4: Uso Real del Builder (1 min)

**ABRIR ARCHIVO**: `server-app/src/main/java/com/messaging/server/service/UserService.java`

**MOSTRAR** (líneas 201-212):
```java
Message message = Message.builder()
    .id(rs.getLong("id"))
    .senderId(rs.getLong("sender_id"))
    .receiverId(rs.getLong("receiver_id"))
    .messageType(rs.getString("message_type"))
    .content(rs.getString("content"))
    .sentAt(rs.getTimestamp("sent_at").toLocalDateTime())
    .isRead(false)
    .build();
```

> "Comparado con el código anterior:
> - Más legible y expresivo
> - Interfaz fluida clara
> - Validación automática
> - Fácil de mantener"

### Slide 5: Comparación (30 seg)

**Mostrar lado a lado**:

```java
// ❌ ANTES (sin Builder)
Message message = new Message();
message.setId(rs.getLong("id"));
message.setSenderId(rs.getLong("sender_id"));
message.setReceiverId(rs.getLong("receiver_id"));
message.setMessageType(rs.getString("message_type"));
message.setContent(rs.getString("content"));
// ... más líneas

// ✅ DESPUÉS (con Builder)
Message message = Message.builder()
    .id(rs.getLong("id"))
    .senderId(rs.getLong("sender_id"))
    .receiverId(rs.getLong("receiver_id"))
    .messageType(rs.getString("message_type"))
    .content(rs.getString("content"))
    .build(); // ← Validación automática
```

---

## 🎯 PARTE 4: Patrones Adicionales (2-3 min)

### Qué decir:

> "Además de los dos patrones requeridos, implementamos SEIS patrones adicionales:
>
> **1. Singleton** - 4 implementaciones
> - ConnectionPool (ya visto)
> - DatabaseConfig
> - ServerConfig  
> - LoggingService
> - Todos con double-checked locking thread-safe
>
> **2. Repository Pattern**
> - 4 repositorios en web-app
> - Spring Data JPA
> - UserRepository, MessageRepository, ConnectionRepository, etc.
>
> **3. MVC Pattern**
> - Web-app: Spring MVC completo
> - Client-app: JavaFX MVC
>
> **4. Dependency Injection**
> - Spring Framework en toda la web-app
> - @Autowired, @Service, @Repository, @Controller
>
> **5. Service Layer**
> - Lógica de negocio encapsulada
> - UserService, ClientHandlerService, WebService, etc.
>
> **6. DAO Pattern**
> - Implícito en repositorios y servicios"

**Mostrar brevemente** un archivo de ejemplo si hay tiempo.

---

## 🎯 PARTE 5: Conclusión (1 min)

### Qué decir:

> "En resumen:
>
> ✅ **Requisitos cumplidos al 100%**
> - Object Pool: Implementado en 2 lugares (ConnectionPool + HikariCP)
> - Builder: Implementado y usado en código real
>
> ✅ **Valor agregado**
> - 6 patrones adicionales
> - Thread-safety garantizado
> - Código refactorizado y mejorado
> - Documentación exhaustiva de 275,000+ caracteres
>
> ✅ **Documentación incluida**
> - 6 archivos markdown
> - 50+ ejemplos de código
> - 15+ diagramas
> - Guía completa de navegación
>
> El proyecto no solo cumple con los requisitos, sino que demuestra una arquitectura sólida siguiendo las mejores prácticas de la industria."

---

## 📊 Material de Apoyo

### Archivos para Tener Abiertos

1. **`RESUMEN_PATRONES.md`** - Para referencia rápida
2. **`ConnectionPool.java`** - Para mostrar Object Pool
3. **`Message.java`** - Para mostrar Builder
4. **`UserService.java`** - Para mostrar uso real
5. **`DIAGRAMAS_PATRONES.md`** - Para diagramas visuales

### Navegación Rápida

**Object Pool**:
- Implementación: `server-app/pool/ConnectionPool.java` (líneas 13-151)
- Singleton: Líneas 28-37
- Add/Remove: Líneas 39-77
- Uso: `ClientHandlerService.java` línea 87

**Builder**:
- Implementación: `server-app/model/Message.java` (líneas 40-269)
- Constructor: Líneas 40-53
- Builder class: Líneas 178-269
- Validación: Líneas 252-262
- Uso: `UserService.java` líneas 201-212, 385-396

---

