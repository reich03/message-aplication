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
     * Aprobar usuario
     */
    @PostMapping("/users/{id}/approve")
    public String approveUser(@PathVariable Long id) {
        webService.approveUser(id);
        return "redirect:/users?approved=true";
    }
    
    /**
     * Rechazar usuario
     */
    @PostMapping("/users/{id}/reject")
    public String rejectUser(@PathVariable Long id) {
        webService.rejectUser(id);
        return "redirect:/users?rejected=true";
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
     * Gestión rápida de mensajes
     */
    @GetMapping("/admin/messages")
    public String manageMessages(Model model) {
        List<Message> messages = webService.getMessages(0, 50, null);
        model.addAttribute("messages", messages);
        return "admin/messages";
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
}
