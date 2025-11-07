FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copiar archivos de configuración
COPY pom.xml .
COPY src ./src

# Instalar Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Compilar la aplicación
RUN mvn clean package -DskipTests

# Crear directorio para logs
RUN mkdir -p /app/logs

# Exponer puerto
EXPOSE 9999

# Comando para ejecutar la aplicación
CMD ["java", "-jar", "target/messaging-server-1.0.jar"]
