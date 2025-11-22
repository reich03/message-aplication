# Aplicación de Mensajería

Sistema de mensajería cliente-servidor con aplicación web MVC y API REST.

## Arquitectura

- **Servidor TCP**: Aplicación Java con concurrencia usando hilos
- **Cliente Desktop**: Aplicación Java Swing
- **Web MVC**: Aplicación Spring Boot
- **API REST**: Servicios REST para consulta de datos
- **Base de Datos**: PostgreSQL

## Requisitos

- Docker
- Docker Compose
- Java 17+ (para desarrollo local)

## Instalación y Ejecución

1. Clonar el repositorio
2. Ejecutar con Docker Compose:

```bash
docker-compose up --build
```

## Servicios

- **Base de Datos**: PostgreSQL en puerto 5432
- **Servidor TCP**: Puerto 9999
- **Aplicación Web**: http://localhost:8082
- **API REST**: http://localhost:8082/api

## Estructura del Proyecto

```
├── server-app/          # Servidor TCP Java
├── client-app/          # Cliente Desktop Java
├── web-app/             # Aplicación Web Spring Boot
├── database/            # Scripts de base de datos
└── docker-compose.yml   # Configuración Docker
```

## Características

- ✅ Arquitectura cliente-servidor
- ✅ Comunicación TCP-IP
- ✅ Concurrencia con hilos
- ✅ Base de datos relacional
- ✅ Patrones de diseño implementados (Object Pool, Builder, Singleton)
- ✅ Panel de administración web
- ✅ API REST

## Patrones de Diseño

Este proyecto implementa los siguientes patrones de diseño:

### Patrones Requeridos ⭐
- **Object Pool**: Gestión eficiente de conexiones de clientes y base de datos
  - `ConnectionPool.java` - Pool de conexiones de clientes
  - `DatabaseConfig.java` - Pool de conexiones de BD (HikariCP)
- **Builder**: Construcción flexible de objetos complejos
  - `Message.Builder` - Constructor fluido para mensajes

### Patrones Adicionales
- **Singleton**: Instancia única para componentes críticos
- **Repository**: Abstracción de persistencia (Spring Data JPA)
- **MVC**: Separación de responsabilidades
- **Service Layer**: Lógica de negocio encapsulada


## Funcionalidades

- ✅ Gestión de usuarios con aprobación
- ✅ Envío de mensajes y archivos
- ✅ Logging en consola y archivo
- ✅ Restricciones de conexiones y archivos
- ✅ Panel de administración web
- ✅ Consultas y reportes
- ✅ Pool de conexiones optimizado
- ✅ Construcción de objetos con validación

## Principios y Buenas Prácticas

- ✅ Principios SOLID
- ✅ Clean Code
- ✅ Thread-Safety
- ✅ Documentación completa
- ✅ Patrones de diseño GoF
