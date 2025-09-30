package com.messaging.client.controller;

import com.messaging.client.model.Message;
import com.messaging.client.model.User;
import com.messaging.client.service.NetworkService;
import com.messaging.client.service.UserService;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controlador principal de la aplicación
 * Implementa patrón MVC y maneja la interfaz master-detail
 */
public class MainController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    // Servicios
    private final NetworkService networkService;
    private final UserService userService;
    private final ScheduledExecutorService scheduler;
    
    // Referencias a la ventana
    private Stage primaryStage;
    
    // Componentes de autenticación
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
    
    // Lista de usuarios conectados
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TableColumn<User, String> lastSeenColumn;
    
    // Área de mensajes
    @FXML private ListView<String> messagesList;
    @FXML private TextArea messageTextArea;
    @FXML private Button sendMessageButton;
    @FXML private Button sendFileButton;
    @FXML private Label selectedUserLabel;
    
    // Lista de mensajes (master-detail)
    @FXML private TableView<Message> messagesTable;
    @FXML private TableColumn<Message, String> senderColumn;
    @FXML private TableColumn<Message, String> receiverColumn;
    @FXML private TableColumn<Message, String> contentColumn;
    @FXML private TableColumn<Message, String> timestampColumn;
    @FXML private TableColumn<Message, String> typeColumn;
    
    // Datos observables
    private final ObservableList<User> connectedUsers;
    private final ObservableList<Message> messages;
    private User currentUser;
    private User selectedUser;
    
    public MainController() {
        this.networkService = NetworkService.getInstance();
        this.userService = new UserService();
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
        // Configurar tabla de usuarios
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        lastSeenColumn.setCellValueFactory(new PropertyValueFactory<>("lastConnection"));
        usersTable.setItems(connectedUsers);
        
        // Configurar tabla de mensajes (master-detail)
        senderColumn.setCellValueFactory(new PropertyValueFactory<>("senderUsername"));
        receiverColumn.setCellValueFactory(new PropertyValueFactory<>("receiverUsername"));
        contentColumn.setCellValueFactory(new PropertyValueFactory<>("content"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("formattedTimestamp"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("messageType"));
        messagesTable.setItems(messages);
        
        // Configurar área de mensajes
        messageTextArea.setWrapText(true);
        messageTextArea.setPrefRowCount(3);
        
        // Estado inicial
        showLoginPane();
    }
    
    private void setupEventHandlers() {
        // Autenticación
        loginButton.setOnAction(e -> handleLogin());
        registerButton.setOnAction(e -> handleRegister());
        
        // Enter para enviar mensaje
        messageTextArea.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && e.isControlDown()) {
                handleSendMessage();
            }
        });
        
        // Botones de mensajería
        sendMessageButton.setOnAction(e -> handleSendMessage());
        sendFileButton.setOnAction(e -> handleSendFile());
        
        // Selección de usuario en tabla
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedUser = newVal;
                selectedUserLabel.setText("Enviando a: " + newVal.getUsername());
                loadMessagesWithUser(newVal.getId());
            }
        });
        
        // Selección de mensaje en tabla (master-detail)
        messagesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showMessageDetails(newVal);
            }
        });
    }
    
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showLoginStatus("Por favor complete todos los campos", false);
            return;
        }
        
        Task<Boolean> loginTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return networkService.authenticate(username, password);
            }
            
            @Override
            protected void succeeded() {
                if (getValue()) {
                    currentUser = new User();
                    currentUser.setUsername(username);
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
    
    private void handleRegister() {
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
    
    private void handleSendMessage() {
        if (selectedUser == null) {
            showStatus("Seleccione un usuario para enviar el mensaje", false);
            return;
        }
        
        String content = messageTextArea.getText().trim();
        if (content.isEmpty()) {
            return;
        }
        
        Task<Boolean> sendTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return networkService.sendMessage(selectedUser.getId(), content);
            }
            
            @Override
            protected void succeeded() {
                if (getValue()) {
                    messageTextArea.clear();
                    loadMessagesWithUser(selectedUser.getId());
                } else {
                    showStatus("Error enviando mensaje", false);
                }
            }
            
            @Override
            protected void failed() {
                showStatus("Error de conexión", false);
            }
        };
        
        new Thread(sendTask).start();
    }
    
    private void handleSendFile() {
        if (selectedUser == null) {
            showStatus("Seleccione un usuario para enviar el archivo", false);
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo para enviar");
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
    
    private void loadMessagesWithUser(int userId) {
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
                });
            }
        };
        
        new Thread(loadMessagesTask).start();
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
    
    private void showLoginStatus(String message, boolean success) {
        loginStatusLabel.setText(message);
        loginStatusLabel.setStyle(success ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
    }
    
    private void showStatus(String message, boolean success) {
        // Implementar notificación de estado
        System.out.println(message);
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
