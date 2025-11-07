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
- ✅ Gestión de usuarios con aprobación
- ✅ Envío de mensajes y archivos
- ✅ Logging en consola y archivo
- ✅ Restricciones de conexiones y archivos
- ✅ Aplicación web MVC
- ✅ API REST
- ✅ Patrones de diseño (Builder, Object Pool)
- ✅ Principios SOLID
