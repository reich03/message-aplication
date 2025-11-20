package com.messaging.web.controller;

import com.messaging.web.model.User;
import com.messaging.web.model.Message;
import com.messaging.web.service.WebService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador principal de la aplicación web MVC
 * Maneja las vistas y funcionalidades del sistema de mensajería
 */
@Controller
@RequestMapping("/")
public class WebController {
    
    @Autowired
    private WebService webService;
    
    /**
     * Página principal - Dashboard
     */
    @GetMapping
    public String dashboard(Model model) {
        // Estadísticas generales
        Map<String, Object> stats = webService.getDashboardStats();
        model.addAttribute("stats", stats);
        
        // Usuarios conectados recientemente
        List<User> recentUsers = webService.getRecentUsers();
        model.addAttribute("recentUsers", recentUsers);
        
        return "dashboard";
    }
    
    /**
     * Lista de usuarios registrados
     */
    @GetMapping("/users")
    public String users(Model model) {
        List<User> users = webService.getAllUsers();
        model.addAttribute("users", users);
        return "users";
    }
    
    /**
     * Lista de mensajes
     */
    @GetMapping("/messages")
    public String messages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            Model model) {
        
        List<Message> messages = webService.getMessages(page, size, type);
        model.addAttribute("messages", messages);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("messageType", type);
        
        return "messages";
    }
    
    /**
     * Detalles de un mensaje específico
     */
    @GetMapping("/messages/{id}")
    public String messageDetails(@PathVariable Long id, Model model) {
        Message message = webService.getMessageById(id);
        if (message == null) {
            return "redirect:/messages?error=notfound";
        }
        
        model.addAttribute("message", message);
        return "message-details";
    }
    
    /**
     * Descargar archivo asociado a un mensaje
     */
    @GetMapping("/messages/{id}/download")
    public String downloadFile(@PathVariable Long id) {
        // Implementar descarga de archivo
        return "redirect:/messages?downloaded=true";
    }
    
    /**
     * Reportes
     */
    @GetMapping("/reports")
    public String reports(Model model) {
        // Usuario con más mensajes enviados
        Map<String, Object> topSender = webService.getTopMessageSender();
        model.addAttribute("topSender", topSender);
        
        // Archivos enviados por tamaño
        List<Map<String, Object>> filesBySize = webService.getFilesBySize();
        model.addAttribute("filesBySize", filesBySize);
        
        // Usuarios conectados
        List<Map<String, Object>> connectedUsers = webService.getConnectedUsers();
        model.addAttribute("connectedUsers", connectedUsers);
        
        // Usuarios desconectados
        List<Map<String, Object>> disconnectedUsers = webService.getDisconnectedUsers();
        model.addAttribute("disconnectedUsers", disconnectedUsers);
        
        return "reports";
    }
    
    /**
     * API REST - Información completa de usuario
     */
    @GetMapping("/api/users/{id}/complete")
    @ResponseBody
    public Map<String, Object> getUserCompleteInfo(@PathVariable Long id) {
        return webService.getUserCompleteInfo(id);
    }
    
    /**
     * API REST - Lista de mensajes enviados por usuario
     */
    @GetMapping("/api/users/{id}/messages/sent")
    @ResponseBody
    public List<Map<String, Object>> getUserSentMessages(@PathVariable Long id) {
        return webService.getUserSentMessages(id);
    }
    
    /**
     * API REST - Lista de mensajes recibidos por usuario
     */
    @GetMapping("/api/users/{id}/messages/received")
    @ResponseBody
    public List<Map<String, Object>> getUserReceivedMessages(@PathVariable Long id) {
        return webService.getUserReceivedMessages(id);
    }
    
    /**
     * API REST - Lista de todos los usuarios con información completa
     */
    @GetMapping("/api/users")
    @ResponseBody
    public List<Map<String, Object>> getAllUsersAPI() {
        return webService.getAllUsersAPI();
    }
    
    /**
     * Crear nuevo usuario
     */
    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        return "user-form";
    }
    
    /**
     * Guardar nuevo usuario
     */
    @PostMapping("/users")
    public String saveUser(@ModelAttribute User user) {
        webService.createUser(user);
        return "redirect:/users?created=true";
    }
    
    /**
     * Editar usuario
     */
    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = webService.getUserById(id);
        if (user == null) {
            return "redirect:/users?error=notfound";
        }
        model.addAttribute("user", user);
        return "user-form";
    }
    
    /**
     * Actualizar usuario
     */
    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User user) {
        webService.updateUser(id, user);
        return "redirect:/users?updated=true";
    }
    
    /**
     * Eliminar usuario
     */
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        webService.deleteUser(id);
        return "redirect:/users?deleted=true";
    }
    
    /**
     * Ver detalles de usuario
     */
    @GetMapping("/users/{id}")
    public String userDetails(@PathVariable Long id, Model model) {
        User user = webService.getUserById(id);
        if (user == null) {
            return "redirect:/users?error=notfound";
        }
        
        // Obtener información completa del usuario
        Map<String, Object> userInfo = webService.getUserCompleteInfo(id);
        model.addAttribute("user", user);
        model.addAttribute("userInfo", userInfo);
        
        return "user-details";
    }
    
    /**
     * Gestión rápida de usuarios
     */
    @GetMapping("/admin/users")
    public String manageUsers(Model model) {
        List<User> users = webService.getAllUsers();
        model.addAttribute("users", users);
        return "admin/users";
    }
    
    /**
     * Gestión rápida de mensajes con filtros avanzados
     */
    @GetMapping("/admin/messages")
    public String manageMessages(
            @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) Long receiverId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "25") int size,
            Model model) {
        
        // Obtener mensajes filtrados
        List<Message> messages = webService.getFilteredMessages(senderId, receiverId, type, dateFrom, dateTo, size);
        
        // Obtener lista de todos los usuarios para los filtros
        List<User> users = webService.getAllUsers();
        
        model.addAttribute("messages", messages);
        model.addAttribute("users", users);
        model.addAttribute("senderId", senderId);
        model.addAttribute("receiverId", receiverId);
        model.addAttribute("type", type);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("size", size);
        
        return "admin/messages";
    }
    
    /**
     * Descargar archivo de un mensaje
     */
    @GetMapping("/admin/messages/{id}/download")
    @ResponseBody
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadMessageFile(@PathVariable Long id) {
        Message message = webService.getMessageById(id);
        if (message == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        
        org.springframework.core.io.Resource resource = webService.downloadMessageFile(id);
        
        // Determinar el tipo de contenido
        String contentType = "application/octet-stream";
        if (message.getMessageType() == Message.MessageType.IMAGE) {
            if (message.getFileName() != null) {
                if (message.getFileName().toLowerCase().endsWith(".jpg") || 
                    message.getFileName().toLowerCase().endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (message.getFileName().toLowerCase().endsWith(".png")) {
                    contentType = "image/png";
                } else if (message.getFileName().toLowerCase().endsWith(".gif")) {
                    contentType = "image/gif";
                }
            }
        }
        
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                        "inline; filename=\"" + (message.getFileName() != null ? message.getFileName() : "file") + "\"")
                .body(resource);
    }
    
    /**
     * API para obtener detalles de un mensaje
     */
    @GetMapping("/api/messages/{id}")
    @ResponseBody
    public Message getMessageDetails(@PathVariable Long id) {
        return webService.getMessageById(id);
    }
    
    /**
     * Eliminar un mensaje
     */
    @PostMapping("/admin/messages/{id}/delete")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deleteMessage(@PathVariable Long id) {
        try {
            webService.deleteMessage(id);
            return org.springframework.http.ResponseEntity.ok().build();
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(500).body("Error al eliminar el mensaje");
        }
    }
    
    /**
     * Gestión rápida de reportes
     */
    @GetMapping("/admin/reports")
    public String manageReports(Model model) {
        Map<String, Object> stats = webService.getDashboardStats();
        model.addAttribute("stats", stats);
        return "admin/reports";
    }
    
    /**
     * Aprobar usuario
     */
    @PostMapping("/users/{id}/approve")
    public String approveUser(@PathVariable Long id) {
        webService.approveUser(id);
        return "redirect:/admin/users?approved=true";
    }
    
    /**
     * Rechazar usuario
     */
    @PostMapping("/users/{id}/reject")
    public String rejectUser(@PathVariable Long id) {
        webService.rejectUser(id);
        return "redirect:/admin/users?rejected=true";
    }
}
