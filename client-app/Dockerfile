FROM eclipse-temurin:17-jdk

WORKDIR /app

# Instalar dependencias necesarias para JavaFX
RUN apt-get update && apt-get install -y \
    libx11-6 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libgtk-3-0 \
    libcanberra-gtk-module \
    libcanberra-gtk3-module \
    && rm -rf /var/lib/apt/lists/*

# Copiar archivos de configuración
COPY pom.xml .
COPY src ./src

# Instalar Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Compilar la aplicación
RUN mvn clean package -DskipTests

# Crear directorio para archivos
RUN mkdir -p /app/files

# Comando para ejecutar la aplicación
CMD ["java", "-jar", "target/messaging-client-1.0.jar"]
