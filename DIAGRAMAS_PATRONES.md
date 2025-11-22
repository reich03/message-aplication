# Diagramas de Patrones de Diseño

Este documento proporciona representaciones visuales de los patrones de diseño implementados en el proyecto.

---

## 1. Diagrama UML - Patrón Builder (Message)

```
┌────────────────────────────────────────────────────────────────┐
│                           Message                               │
├────────────────────────────────────────────────────────────────┤
│ - id: Long                                                      │
│ - senderId: Long                                                │
│ - receiverId: Long                                              │
│ - messageType: String                                           │
│ - content: String                                               │
│ - fileName: String                                              │
│ - filePath: String                                              │
│ - fileSize: Long                                                │
│ - sentAt: LocalDateTime                                         │
│ - createdAt: LocalDateTime                                      │
│ - isRead: boolean                                               │
│ - senderUsername: String                                        │
│ - receiverUsername: String                                      │
├────────────────────────────────────────────────────────────────┤
│ - Message(builder: Builder)        [Constructor Privado]       │
│ + static builder(): Builder         [Factory Method]           │
│ + getters/setters...                                            │
│                                                                  │
│ ┌────────────────────────────────────────────────────────────┐ │
│ │                    Inner Class: Builder                     │ │
│ ├────────────────────────────────────────────────────────────┤ │
│ │ - id: Long                                                  │ │
│ │ - senderId: Long                                            │ │
│ │ - receiverId: Long                                          │ │
│ │ - messageType: String                                       │ │
│ │ - content: String                                           │ │
│ │ - ... (todos los campos)                                    │ │
│ ├────────────────────────────────────────────────────────────┤ │
│ │ + id(Long): Builder                                         │ │
│ │ + senderId(Long): Builder                                   │ │
│ │ + receiverId(Long): Builder                                 │ │
│ │ + messageType(String): Builder                              │ │
│ │ + content(String): Builder                                  │ │
│ │ + ... (métodos fluent para todos los campos)                │ │
│ │ + build(): Message              [Validación + Construcción] │ │
│ └────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
```

### Flujo de Construcción

```
Cliente
   │
   │ 1. Llama a factory method
   ├────────────────────────────────────────────────────────────┐
   │                                                             │
   │  Message.builder()                                          │
   │       │                                                     │
   │       ├──> Retorna nueva instancia de Builder              │
   │       │                                                     │
   │  2. Configuración fluida                                    │
   │       │                                                     │
   │  .senderId(1L)                                              │
   │       │                                                     │
   │  .receiverId(2L)                ┌──────────────────────┐   │
   │       │                         │      Builder         │   │
   │  .messageType("TEXT")           │  - Valida            │   │
   │       │                         │  - Acumula valores   │   │
   │  .content("Hola")               │  - Retorna this      │   │
   │       │                         └──────────────────────┘   │
   │  3. Construcción final                                      │
   │       │                                                     │
   │  .build()                                                   │
   │       │                                                     │
   │       ├──> Valida campos requeridos                        │
   │       │                                                     │
   │       ├──> Llama constructor privado Message(Builder)      │
   │       │                                                     │
   │       └──> Retorna objeto Message inmutable                │
   │                                                             │
   └─────────────────────────────────────────────────────────────┘
```

---

## 2. Diagrama - Patrón Object Pool (ConnectionPool)

```
┌─────────────────────────────────────────────────────────────────┐
│                        ConnectionPool                            │
│                         (Singleton)                              │
├─────────────────────────────────────────────────────────────────┤
│ - static volatile instance: ConnectionPool                      │
│ - activeConnections: ConcurrentHashMap<String,ClientConnection> │
│ - userConnectionCounts: ConcurrentHashMap<Long, AtomicInteger>  │
│ - totalConnections: AtomicInteger                               │
├─────────────────────────────────────────────────────────────────┤
│ - ConnectionPool()                      [Constructor Privado]   │
│ + static getInstance(): ConnectionPool  [Singleton]             │
│ + addConnection(ClientConnection): boolean                      │
│ + removeConnection(String): void                                │
│ + getConnection(String): ClientConnection                       │
│ + getUserConnections(Long): Set<ClientConnection>               │
│ + getActiveConnections(): int                                   │
│ + getUserConnectionCount(Long): int                             │
│ + cleanupInactiveConnections(): void                            │
│ + getStatistics(): Map<String, Object>                          │
│ + shutdown(): void                                              │
└─────────────────────────────────────────────────────────────────┘
```

### Flujo de Gestión de Conexiones

```
                         ConnectionPool (Singleton)
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
        ▼                        ▼                        ▼
  addConnection()         getConnection()         removeConnection()
        │                        │                        │
        │                        │                        │
        ▼                        ▼                        ▼
┌───────────────────┐  ┌──────────────────┐   ┌──────────────────┐
│ 1. Verificar      │  │ 1. Buscar en     │   │ 1. Remover de    │
│    límites        │  │    HashMap       │   │    HashMap       │
│                   │  │                  │   │                  │
│ 2. Agregar a      │  │ 2. Retornar      │   │ 2. Actualizar    │
│    HashMap        │  │    conexión      │   │    contadores    │
│                   │  │                  │   │                  │
│ 3. Incrementar    │  │                  │   │ 3. Decrementar   │
│    contador       │  │                  │   │    contador      │
│                   │  │                  │   │                  │
│ 4. Retornar       │  │                  │   │ 4. Cerrar        │
│    true/false     │  │                  │   │    recursos      │
└───────────────────┘  └──────────────────┘   └──────────────────┘

         Thread-Safe usando ConcurrentHashMap y AtomicInteger
```

### Estado del Pool

```
┌──────────────────────────────────────────────────────────────┐
│                    ConnectionPool State                       │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  activeConnections (ConcurrentHashMap)                        │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ "conn-001" → ClientConnection{userId=1, socket=...}     │ │
│  │ "conn-002" → ClientConnection{userId=1, socket=...}     │ │
│  │ "conn-003" → ClientConnection{userId=2, socket=...}     │ │
│  │ "conn-004" → ClientConnection{userId=3, socket=...}     │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  userConnectionCounts (ConcurrentHashMap)                     │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ userId=1 → AtomicInteger(2)    [2 conexiones activas]  │ │
│  │ userId=2 → AtomicInteger(1)    [1 conexión activa]     │ │
│  │ userId=3 → AtomicInteger(1)    [1 conexión activa]     │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  totalConnections = AtomicInteger(4)                          │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

---

## 3. Diagrama - Patrón Singleton

### Double-Checked Locking Implementation

```
                         getInstance()
                               │
                               ▼
                    ┌──────────────────────┐
                    │ instance == null?    │
                    └──────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
                ▼                             ▼
              YES                            NO
                │                             │
                ▼                             │
    ┌───────────────────────┐                │
    │ synchronized(Class)   │                │
    └───────────────────────┘                │
                │                             │
                ▼                             │
    ┌──────────────────────┐                 │
    │ instance == null?    │                 │
    │ (Double Check)       │                 │
    └──────────────────────┘                 │
                │                             │
     ┌──────────┴──────────┐                 │
     │                     │                 │
     ▼                     ▼                 │
    YES                   NO                 │
     │                     │                 │
     ▼                     │                 │
┌─────────────┐            │                 │
│ Create new  │            │                 │
│ instance    │            │                 │
└─────────────┘            │                 │
     │                     │                 │
     └──────────┬──────────┘                 │
                │                             │
                └─────────────────────────────┘
                               │
                               ▼
                      Return instance
```

### Thread Safety Visualization

```
Thread 1                    Thread 2                    Thread 3
   │                           │                           │
   │ getInstance()             │                           │
   │                           │ getInstance()             │
   ├─ Check: null? YES         │                           │
   │                           ├─ Check: null? YES         │
   ├─ Wait for lock            │                           │ getInstance()
   │                           ├─ Wait for lock            │
   │                           │                           ├─ Check: null? YES
   ├─ Acquire lock             │                           │
   │                           │                           ├─ Wait for lock
   ├─ Double-check: null? YES  │                           │
   │                           │                           │
   ├─ CREATE INSTANCE ✓        │                           │
   │                           │                           │
   ├─ Release lock             │                           │
   │                           │                           │
   │                           ├─ Acquire lock             │
   │                           │                           │
   ├─ Return instance          ├─ Double-check: null? NO   │
   │                           │                           │
   │                           ├─ Release lock             │
   │                           │                           │
   │                           ├─ Return instance          ├─ Acquire lock
   │                           │                           │
   │                           │                           ├─ Double-check: null? NO
   │                           │                           │
   │                           │                           ├─ Release lock
   │                           │                           │
   │                           │                           ├─ Return instance
   ▼                           ▼                           ▼
 Same                        Same                        Same
 Instance                    Instance                    Instance
```

---

## 4. Database Connection Pool (HikariCP)

```
┌─────────────────────────────────────────────────────────────────┐
│                       DatabaseConfig                             │
│                        (Singleton)                               │
├─────────────────────────────────────────────────────────────────┤
│ - static volatile instance: DatabaseConfig                      │
│ - dataSource: HikariDataSource                                  │
├─────────────────────────────────────────────────────────────────┤
│ - DatabaseConfig()                      [Constructor Privado]   │
│ - initializeDataSource(): void                                  │
│ + static getInstance(): DatabaseConfig                          │
│ + getConnection(): Connection                                   │
│ + close(): void                                                 │
└─────────────────────────────────────────────────────────────────┘
                               │
                               │ contains
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                      HikariDataSource                            │
│                    (Connection Pool)                             │
├─────────────────────────────────────────────────────────────────┤
│ Configuration:                                                   │
│ - Maximum Pool Size: 20                                          │
│ - Minimum Idle: 5                                                │
│ - Connection Timeout: 30s                                        │
│ - Idle Timeout: 10min                                            │
│ - Max Lifetime: 30min                                            │
├─────────────────────────────────────────────────────────────────┤
│                    Connection Pool State                         │
│                                                                  │
│  Active Connections: [████████░░] 8/20                           │
│  Idle Connections:   [████░░░░░░] 4/20                           │
│  Available:          12 connections                              │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Connection 1 [IN USE]  ← Thread 1 (Query running)          │ │
│  │ Connection 2 [IN USE]  ← Thread 2 (Transaction active)     │ │
│  │ Connection 3 [IN USE]  ← Thread 3 (Reading data)           │ │
│  │ Connection 4 [IDLE]    (Available for reuse)               │ │
│  │ Connection 5 [IDLE]    (Available for reuse)               │ │
│  │ ...                                                         │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                               │
                               │ JDBC
                               ▼
                      ┌─────────────────┐
                      │   PostgreSQL    │
                      │    Database     │
                      └─────────────────┘
```

### Connection Lifecycle

```
Request Connection
       │
       ▼
┌──────────────┐
│ Pool has     │ YES
│ idle conn?   ├────────► Return existing connection
└──────────────┘              │
       │ NO                   │
       ▼                      │
┌──────────────┐              │
│ Pool < max?  │ YES          │
│              ├──────► Create new connection
└──────────────┘              │
       │ NO                   │
       ▼                      │
┌──────────────┐              │
│ Wait for     │              │
│ available    │              │
│ (timeout)    │              │
└──────────────┘              │
       │                      │
       └──────────────────────┘
                │
                ▼
         Use Connection
                │
                ▼
         Close Connection
         (Return to pool)
                │
                ▼
┌───────────────────────────┐
│ Connection marked as IDLE │
│ Available for reuse       │
└───────────────────────────┘
```

---

## 5. Arquitectura General del Sistema

```
┌───────────────────────────────────────────────────────────────────┐
│                          CLIENT LAYER                              │
│                                                                    │
│  ┌──────────────────┐              ┌──────────────────┐           │
│  │  Desktop Client  │              │   Web Browser    │           │
│  │   (JavaFX)       │              │   (HTML/CSS/JS)  │           │
│  └────────┬─────────┘              └────────┬─────────┘           │
│           │                                 │                     │
│           │ TCP Socket                      │ HTTP/REST           │
└───────────┼─────────────────────────────────┼─────────────────────┘
            │                                 │
            │                                 │
┌───────────▼─────────────────────────────────▼─────────────────────┐
│                       SERVER LAYER                                 │
│                                                                    │
│  ┌──────────────────────────────────────┐  ┌──────────────────┐  │
│  │     MessagingServer (TCP)            │  │   Web-App        │  │
│  │                                      │  │  (Spring Boot)   │  │
│  │  ┌─────────────────────────────────┐│  │                  │  │
│  │  │    ConnectionPool (Singleton)   ││  │  ┌─────────────┐ │  │
│  │  │      (Object Pool Pattern)      ││  │  │Controllers  │ │  │
│  │  └─────────────────────────────────┘│  │  │   (MVC)     │ │  │
│  │                                      │  │  └─────────────┘ │  │
│  │  ┌─────────────────────────────────┐│  │  ┌─────────────┐ │  │
│  │  │  ServerConfig (Singleton)       ││  │  │  Services   │ │  │
│  │  └─────────────────────────────────┘│  │  │  (Business) │ │  │
│  │                                      │  │  └─────────────┘ │  │
│  │  ┌─────────────────────────────────┐│  │  ┌─────────────┐ │  │
│  │  │  ClientHandlerService           ││  │  │Repositories │ │  │
│  │  │    (Thread per client)          ││  │  │   (DAO)     │ │  │
│  │  └─────────────────────────────────┘│  │  └─────────────┘ │  │
│  │                                      │  │                  │  │
│  │  ┌─────────────────────────────────┐│  │                  │  │
│  │  │     UserService                 ││  │                  │  │
│  │  │  (Business Logic)               ││  │                  │  │
│  │  │  - Uses Message.Builder         ││  │                  │  │
│  │  └─────────────────────────────────┘│  │                  │  │
│  └──────────────┬───────────────────────┘  └────────┬─────────┘  │
│                 │                                   │             │
│                 │                                   │             │
└─────────────────┼───────────────────────────────────┼─────────────┘
                  │                                   │
                  │                                   │
┌─────────────────▼───────────────────────────────────▼─────────────┐
│                    PERSISTENCE LAYER                               │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │           DatabaseConfig (Singleton)                         │ │
│  │              HikariCP (Connection Pool)                      │ │
│  │                 (Object Pool Pattern)                        │ │
│  └──────────────────────┬───────────────────────────────────────┘ │
│                         │ JDBC                                    │
│                         ▼                                         │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                    PostgreSQL Database                        │ │
│  │  Tables: users, messages, active_connections,                │ │
│  │          connection_history                                  │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

---

## 6. Flujo de Envío de Mensaje (Usando Builder)

```
┌─────────┐
│ Client  │
└────┬────┘
     │
     │ 1. User inputs message
     ▼
┌──────────────────────────┐
│  NetworkService          │
│                          │
│  Message msg =           │
│    Message.builder()     │ ◄─── Builder Pattern
│      .senderId(1L)       │
│      .receiverId(2L)     │
│      .messageType("TEXT")│
│      .content("Hello")   │
│      .build();           │
└────┬─────────────────────┘
     │
     │ 2. Send via TCP
     ▼
┌──────────────────────────┐
│  MessagingServer         │
│                          │
│  ClientHandlerService    │
└────┬─────────────────────┘
     │
     │ 3. Get connection from pool
     ▼
┌──────────────────────────┐
│  ConnectionPool          │ ◄─── Object Pool Pattern
│  .getInstance()          │      + Singleton Pattern
│  .getConnection(id)      │
└────┬─────────────────────┘
     │
     │ 4. Save to database
     ▼
┌──────────────────────────┐
│  UserService             │
│                          │
│  Message dbMsg =         │
│    Message.builder()     │ ◄─── Builder Pattern
│      .id(rs.getLong())   │
│      .senderId(...)      │
│      .receiverId(...)    │
│      .content(...)       │
│      .build();           │
└────┬─────────────────────┘
     │
     │ 5. Get DB connection from pool
     ▼
┌──────────────────────────┐
│  DatabaseConfig          │ ◄─── Object Pool Pattern
│  .getInstance()          │      (HikariCP)
│  .getConnection()        │      + Singleton Pattern
└────┬─────────────────────┘
     │
     │ 6. Execute INSERT
     ▼
┌──────────────────────────┐
│  PostgreSQL              │
└──────────────────────────┘
```

