package com.messaging.client.service;

import com.messaging.client.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * Servicio para gestión de usuarios en el cliente
 * Maneja preferencias y datos locales del usuario
 */
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String PREFS_NODE = "com.messaging.client";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_REMEMBER_LOGIN = "rememberLogin";
    
    private final Preferences preferences;
    private User currentUser;
    
    public UserService() {
        this.preferences = Preferences.userRoot().node(PREFS_NODE);
    }
    
    /**
     * Guardar credenciales del usuario
     */
    public void saveUserCredentials(String username, boolean rememberLogin) {
        preferences.put(KEY_USERNAME, username);
        preferences.putBoolean(KEY_REMEMBER_LOGIN, rememberLogin);
        logger.debug("Credenciales guardadas para usuario: {}", username);
    }
    
    /**
     * Obtener nombre de usuario guardado
     */
    public String getSavedUsername() {
        return preferences.get(KEY_USERNAME, "");
    }
    
    /**
     * Verificar si debe recordar el login
     */
    public boolean shouldRememberLogin() {
        return preferences.getBoolean(KEY_REMEMBER_LOGIN, false);
    }
    
    /**
     * Limpiar credenciales guardadas
     */
    public void clearSavedCredentials() {
        preferences.remove(KEY_USERNAME);
        preferences.remove(KEY_REMEMBER_LOGIN);
        logger.debug("Credenciales limpiadas");
    }
    
    /**
     * Establecer usuario actual
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        logger.debug("Usuario actual establecido: {}", user.getUsername());
    }
    
    /**
     * Obtener usuario actual
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Verificar si hay un usuario logueado
     */
    public boolean isUserLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Cerrar sesión
     */
    public void logout() {
        if (currentUser != null) {
            logger.info("Usuario {} cerrando sesión", currentUser.getUsername());
            currentUser = null;
        }
    }
}
