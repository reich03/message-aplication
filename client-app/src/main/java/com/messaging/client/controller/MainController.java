package com.messaging.client.controller;

import com.messaging.client.model.Message;
import com.messaging.client.model.User;
import com.messaging.client.service.NetworkService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    private final NetworkService networkService;
    private final ScheduledExecutorService scheduler;
    
    private Stage primaryStage;
    
    @FXML private VBox loginPane;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label loginStatusLabel;
    
    // Componentes del chat
    @FXML private VBox chatPane;
    @FXML private Label welcomeLabel;
    @FXML private Label connectionStatusLabel;
    
    // Nuevos componentes estilo WhatsApp
    @FXML private TextField searchUserField;
    @FXML private ListView<User> usersListView;
    @FXML private HBox chatHeader;
    @FXML private Label selectedUserLabel;
    @FXML private Label userStatusLabel;
    @FXML private Label onlineIndicator;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesContainer;
    @FXML private VBox messageInputPanel;
    @FXML private TextArea messageTextArea;
    @FXML private Button sendMessageButton;
    @FXML private Button sendFileButton;
    @FXML private VBox noSelectionPane;
    
    // Datos observables
    private final ObservableList<User> connectedUsers;
    private final ObservableList<Message> messages;
    private User currentUser;
    private User selectedUser;
    
    public MainController() {
        this.networkService = NetworkService.getInstance();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.connectedUsers = FXCollections.observableArrayList();
        this.messages = FXCollections.observableArrayList();
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupEventHandlers();
        startPeriodicUpdates();
    }
    
    private void setupUI() {
        // Configurar lista de usuarios con formato personalizado
        usersListView.setItems(connectedUsers);
        usersListView.setCellFactory(listView -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox vbox = new VBox(5);
                    Label nameLabel = new Label(user.getUsername());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    
                    String status = user.isConnected() ? "● En línea" : "○ Desconectado";
                    Label statusLabel = new Label(status);
                    statusLabel.setStyle(user.isConnected() ? 
                        "-fx-text-fill: #00A884; -fx-font-size: 12px;" : 
                        "-fx-text-fill: #8696A0; -fx-font-size: 12px;");
                    
                    vbox.getChildren().addAll(nameLabel, statusLabel);
                    setGraphic(vbox);
                }
            }
        });
        
        // Configurar área de mensajes
        messageTextArea.setWrapText(true);
        
        // Estado inicial
        showLoginPane();
        showNoSelectionPane();
    }
    
    private void setupEventHandlers() {
        // Autenticación
        loginButton.setOnAction(e -> handleLogin());
        registerButton.setOnAction(e -> handleRegister());
        
        // Enter para enviar mensaje
        messageTextArea.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                handleSendMessage();
            }
        });
        
        // Botones de mensajería
        sendMessageButton.setOnAction(e -> handleSendMessage());
        sendFileButton.setOnAction(e -> handleSendFile());
        
        // Selección de usuario en la lista
        usersListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedUser = newVal;
                showChatWithUser(newVal);
                loadMessagesWithUser(newVal.getId());
            }
        });
        
        // Filtro de búsqueda
        searchUserField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterUsers(newVal);
        });
    }
    
    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showLoginStatus("Por favor complete todos los campos", false);
            return;
        }
        
        Task<User> loginTask = new Task<User>() {
            @Override
            protected User call() throws Exception {
                return networkService.authenticateAndGetUser(username, password);
            }
            
            @Override
            protected void succeeded() {
                User user = getValue();
                if (user != null) {
                    currentUser = user;
                    logger.info("Usuario autenticado: {} con ID: {}", currentUser.getUsername(), currentUser.getId());
                    showChatPane();
                    loadInitialData();
                } else {
                    showLoginStatus("Credenciales inválidas", false);
                }
            }
            
            @Override
            protected void failed() {
                showLoginStatus("Error de conexión", false);
            }
        };
        
        new Thread(loginTask).start();
    }
    
    @FXML
    public void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showLoginStatus("Por favor complete todos los campos", false);
            return;
        }
        
        Task<Boolean> registerTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return networkService.register(username, password, username + "@example.com");
            }
            
            @Override
            protected void succeeded() {
                if (getValue()) {
                    showLoginStatus("Usuario registrado. Esperando aprobación del administrador.", true);
                } else {
                    showLoginStatus("Error registrando usuario", false);
                }
            }
            
            @Override
            protected void failed() {
                showLoginStatus("Error de conexión", false);
            }
        };
        
        new Thread(registerTask).start();
    }
    
    @FXML
    public void handleSendMessage() {
        if (selectedUser == null) {
            showStatus("Seleccione un usuario para enviar el mensaje", false);
            return;
        }
        
        String content = messageTextArea.getText().trim();
        if (content.isEmpty()) {
            return;
        }
        
        // Limpiar el campo inmediatamente para mejor UX
        messageTextArea.clear();
        
        Task<Boolean> sendTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return networkService.sendMessage(selectedUser.getId(), content);
            }
            
            @Override
            protected void succeeded() {
                if (getValue()) {
                    // Recargar mensajes para mostrar el enviado
                    loadMessagesWithUser(selectedUser.getId());
                } else {
                    showStatus("Error enviando mensaje", false);
                    // Restaurar el texto si falló
                    Platform.runLater(() -> messageTextArea.setText(content));
                }
            }
            
            @Override
            protected void failed() {
                showStatus("Error de conexión", false);
                Platform.runLater(() -> messageTextArea.setText(content));
            }
        };
        
        new Thread(sendTask).start();
    }
    
    @FXML
    public void handleSendFile() {
        if (selectedUser == null) {
            showStatus("Seleccione un usuario para enviar el archivo", false);
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo para enviar");
        
        // Configurar directorio inicial a Descargas del usuario
        String userHome = System.getProperty("user.home");
        File downloadsDir = new File(userHome, "Downloads");
        if (downloadsDir.exists() && downloadsDir.isDirectory()) {
            fileChooser.setInitialDirectory(downloadsDir);
        } else {
            fileChooser.setInitialDirectory(new File(userHome));
        }
        
        // Limitar tipos de archivo para evitar cargar todo
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"),
            new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.doc", "*.docx", "*.txt"),
            new FileChooser.ExtensionFilter("Archivos comprimidos", "*.zip", "*.rar", "*.7z"),
            new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        
        if (selectedFile != null) {
            Task<Boolean> sendFileTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    return networkService.sendFile(selectedUser.getId(), selectedFile);
                }
                
                @Override
                protected void succeeded() {
                    if (getValue()) {
                        showStatus("Archivo enviado correctamente", true);
                        loadMessagesWithUser(selectedUser.getId());
                    } else {
                        showStatus("Error enviando archivo", false);
                    }
                }
                
                @Override
                protected void failed() {
                    showStatus("Error de conexión", false);
                }
            };
            
            new Thread(sendFileTask).start();
        }
    }
    
    private void handleDownloadFile(Message message) {
        if (message.getContent() == null || message.getFileName() == null) {
            showStatus("No hay archivo para descargar", false);
            return;
        }
        
        // Pedir al usuario dónde guardar
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar archivo");
        fileChooser.setInitialFileName(message.getFileName());
        
        // Configurar directorio inicial a Descargas
        String userHome = System.getProperty("user.home");
        File downloadsDir = new File(userHome, "Downloads");
        if (downloadsDir.exists() && downloadsDir.isDirectory()) {
            fileChooser.setInitialDirectory(downloadsDir);
        }
        
        File destinationFile = fileChooser.showSaveDialog(primaryStage);
        
        if (destinationFile != null) {
            Task<Boolean> downloadTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    return networkService.downloadFile(message.getContent(), destinationFile);
                }
                
                @Override
                protected void succeeded() {
                    if (getValue()) {
                        showStatus("Archivo descargado: " + destinationFile.getName(), true);
                    } else {
                        showStatus("Error descargando archivo", false);
                    }
                }
                
                @Override
                protected void failed() {
                    showStatus("Error de conexión al descargar", false);
                }
            };
            
            new Thread(downloadTask).start();
        }
    }
    
    private void loadInitialData() {
        loadConnectedUsers();
        loadUserMessages();
    }
    
    private void loadConnectedUsers() {
        Task<List<User>> loadUsersTask = new Task<List<User>>() {
            @Override
            protected List<User> call() throws Exception {
                return networkService.getConnectedUsers();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    connectedUsers.clear();
                    connectedUsers.addAll(getValue());
                });
            }
        };
        
        new Thread(loadUsersTask).start();
    }
    
    private void loadUserMessages() {
        Task<List<Message>> loadMessagesTask = new Task<List<Message>>() {
            @Override
            protected List<Message> call() throws Exception {
                return networkService.getUserMessages();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    messages.clear();
                    messages.addAll(getValue());
                });
            }
        };
        
        new Thread(loadMessagesTask).start();
    }
    
    private void loadMessagesWithUser(Long userId) {
        Task<List<Message>> loadMessagesTask = new Task<List<Message>>() {
            @Override
            protected List<Message> call() throws Exception {
                return networkService.getMessagesWithUser(userId);
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    messages.clear();
                    messages.addAll(getValue());
                    displayMessages(getValue());
                });
            }
        };
        
        new Thread(loadMessagesTask).start();
    }
    
    private void displayMessages(List<Message> messagesList) {
        messagesContainer.getChildren().clear();
        
        for (Message msg : messagesList) {
            // Determinar si el mensaje fue enviado por el usuario actual
            boolean isSent = msg.getSenderId() != null && 
                           currentUser != null && 
                           currentUser.getId() != null && 
                           msg.getSenderId().equals(currentUser.getId());
            
            // Crear la burbuja del mensaje
            VBox messageBox = new VBox(5);
            messageBox.setMaxWidth(450);
            messageBox.setStyle(isSent ? 
                "-fx-background-color: #D9FDD3; -fx-background-radius: 8px; -fx-padding: 10px;" :
                "-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-padding: 10px;");
            
            // Si es un archivo o imagen, mostrar información del archivo
            if (("FILE".equals(msg.getMessageType()) || "IMAGE".equals(msg.getMessageType())) 
                && msg.getFileName() != null) {
                
                // Verificar si es una imagen
                String fileName = msg.getFileName().toLowerCase();
                boolean isImage = fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                                fileName.endsWith(".png") || fileName.endsWith(".gif") || 
                                fileName.endsWith(".bmp");
                
                // Información del archivo
                HBox fileInfo = new HBox(8);
                fileInfo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                fileInfo.setStyle("-fx-cursor: hand; -fx-padding: 5px; -fx-background-color: rgba(0,0,0,0.05); -fx-background-radius: 5px;");
                
                Label fileIcon = new Label(isImage ? "🖼️" : "📎");
                fileIcon.setStyle("-fx-font-size: 24px;");
                
                VBox fileDetails = new VBox(2);
                Label fileNameLabel = new Label(msg.getFileName());
                fileNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #111B21;");
                
                Label fileInfoLabel = new Label(isImage ? "Imagen enviada - Click para descargar" : "Archivo enviado - Click para descargar");
                fileInfoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #667781;");
                
                fileDetails.getChildren().addAll(fileNameLabel, fileInfoLabel);
                fileInfo.getChildren().addAll(fileIcon, fileDetails);
                
                // Click para descargar
                fileInfo.setOnMouseClicked(e -> handleDownloadFile(msg));
                
                // Tooltip informativo
                javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(
                    "Click para descargar: " + msg.getFileName());
                javafx.scene.control.Tooltip.install(fileInfo, tooltip);
                
                messageBox.getChildren().add(fileInfo);
                
                // Contenido adicional si existe
                if (msg.getContent() != null && !msg.getContent().trim().isEmpty() 
                    && !msg.getContent().startsWith("uploads/")) {
                    Label contentLabel = new Label(msg.getContent());
                    contentLabel.setWrapText(true);
                    contentLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #111B21; -fx-padding: 5 0 0 0;");
                    messageBox.getChildren().add(contentLabel);
                }
            } else {
                // Mensaje de texto normal
                Label contentLabel = new Label(msg.getContent() != null ? msg.getContent() : "");
                contentLabel.setWrapText(true);
                contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #111B21;");
                messageBox.getChildren().add(contentLabel);
            }
            
            // Hora del mensaje
            Label timeLabel = new Label(msg.getFormattedTimestamp());
            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #667781;");
            messageBox.getChildren().add(timeLabel);
            
            // Contenedor para alinear el mensaje (derecha si es enviado, izquierda si es recibido)
            HBox container = new HBox();
            container.setPadding(new Insets(5));
            
            if (isSent) {
                // Mensajes enviados: alineados a la DERECHA
                container.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            } else {
                // Mensajes recibidos: alineados a la IZQUIERDA
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }
            
            container.getChildren().add(messageBox);
            messagesContainer.getChildren().add(container);
        }
        
        // Auto-scroll al final
        Platform.runLater(() -> {
            messagesScrollPane.setVvalue(1.0);
        });
    }
    
    private void showImageFullScreen(String imagePath) {
        try {
            javafx.stage.Stage imageStage = new javafx.stage.Stage();
            imageStage.setTitle("Imagen");
            
            java.io.File imageFile = new java.io.File(imagePath);
            javafx.scene.image.Image image = new javafx.scene.image.Image(imageFile.toURI().toString());
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
            imageView.setPreserveRatio(true);
            
            ScrollPane scrollPane = new ScrollPane(imageView);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            
            javafx.scene.Scene scene = new javafx.scene.Scene(scrollPane, 800, 600);
            imageStage.setScene(scene);
            imageStage.show();
        } catch (Exception e) {
            logger.error("Error mostrando imagen: " + e.getMessage());
        }
    }
    
    private void showMessageDetails(Message message) {
        // Implementar vista de detalles del mensaje
        String details = String.format(
            "Mensaje ID: %d\n" +
            "De: %s\n" +
            "Para: %s\n" +
            "Tipo: %s\n" +
            "Contenido: %s\n" +
            "Enviado: %s",
            message.getId(),
            message.getSenderUsername(),
            message.getReceiverUsername(),
            message.getMessageType(),
            message.getContent(),
            message.getFormattedTimestamp()
        );
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalles del Mensaje");
        alert.setHeaderText(null);
        alert.setContentText(details);
        alert.showAndWait();
    }
    
    private void startPeriodicUpdates() {
        // Actualizar usuarios conectados cada 30 segundos
        scheduler.scheduleAtFixedRate(() -> {
            if (currentUser != null) {
                Platform.runLater(this::loadConnectedUsers);
            }
        }, 30, 30, TimeUnit.SECONDS);
        
        // Enviar ping cada 60 segundos
        scheduler.scheduleAtFixedRate(() -> {
            if (currentUser != null) {
                networkService.sendPing();
            }
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    private void showLoginPane() {
        loginPane.setVisible(true);
        chatPane.setVisible(false);
    }
    
    private void showChatPane() {
        loginPane.setVisible(false);
        chatPane.setVisible(true);
        welcomeLabel.setText("Bienvenido, " + currentUser.getUsername());
        connectionStatusLabel.setText("Conectado");
    }
    
    private void showNoSelectionPane() {
        noSelectionPane.setVisible(true);
        noSelectionPane.setManaged(true);
        chatHeader.setVisible(false);
        messageInputPanel.setVisible(false);
        messagesScrollPane.setVisible(false);
    }
    
    private void showChatWithUser(User user) {
        noSelectionPane.setVisible(false);
        noSelectionPane.setManaged(false);
        chatHeader.setVisible(true);
        messageInputPanel.setVisible(true);
        messagesScrollPane.setVisible(true);
        
        selectedUserLabel.setText(user.getUsername());
        userStatusLabel.setText("Haz clic aquí para ver información");
        
        if (user.isConnected()) {
            onlineIndicator.setVisible(true);
            onlineIndicator.setText("● En línea");
        } else {
            onlineIndicator.setVisible(true);
            onlineIndicator.setText("○ Desconectado");
            onlineIndicator.setStyle("-fx-text-fill: #8696A0;");
        }
        
        messagesContainer.getChildren().clear();
    }
    
    private void filterUsers(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            usersListView.setItems(connectedUsers);
        } else {
            ObservableList<User> filtered = FXCollections.observableArrayList();
            for (User user : connectedUsers) {
                if (user.getUsername().toLowerCase().contains(searchText.toLowerCase())) {
                    filtered.add(user);
                }
            }
            usersListView.setItems(filtered);
        }
    }
    
    private void showLoginStatus(String message, boolean success) {
        loginStatusLabel.setText(message);
        loginStatusLabel.setStyle(success ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
    }
    
    private void showStatus(String message, boolean success) {
        // Implementar notificación de estado
        System.out.println(message);
    }
    
    @FXML
    private void handleRefreshUsers() {
        loadConnectedUsers();
    }
    
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    
    public void shutdown() {
        scheduler.shutdown();
        networkService.disconnect();
        logger.info("Controlador principal cerrado");
    }
    
    // Métodos adicionales para el menú
    @FXML
    private void handleExit() {
        shutdown();
        Platform.exit();
    }
    
    @FXML
    private void handleSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Configuración");
        alert.setHeaderText("Configuración del Cliente");
        alert.setContentText("Configuración del servidor:\nHost: localhost\nPuerto: 9999");
        alert.showAndWait();
    }
    
    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Acerca de");
        alert.setHeaderText("Sistema de Mensajería");
        alert.setContentText("Versión 1.0\nCliente JavaFX\nDesarrollado con Java 17");
        alert.showAndWait();
    }
}
