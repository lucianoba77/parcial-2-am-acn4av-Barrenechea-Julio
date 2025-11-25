package com.controlmedicamentos.myapplication.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Servicio para manejar la autenticación OAuth con Google Calendar
 * Guarda y obtiene tokens de acceso desde Firestore
 */
public class GoogleCalendarAuthService {
    private static final String TAG = "GoogleCalendarAuth";
    private static final String COLLECTION_GOOGLE_TOKENS = "googleTokens";
    
    private Context context;
    private FirebaseFirestore db;
    private AuthService authService;
    
    public GoogleCalendarAuthService(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.authService = new AuthService();
    }
    
    /**
     * Guarda el token de acceso de Google en Firestore
     * Consistente con React: calendarService.js - guardarTokenGoogle()
     */
    public void guardarTokenGoogle(Map<String, Object> tokenData, FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }
        
        String userId = firebaseUser.getUid();
        Map<String, Object> tokenParaGuardar = new HashMap<>(tokenData);
        
        // Agregar metadatos
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        tokenParaGuardar.put("fechaActualizacion", isoFormat.format(new Date()));
        tokenParaGuardar.put("userId", userId);
        
        // Si no tiene fechaObtencion, agregarla
        if (!tokenParaGuardar.containsKey("fechaObtencion")) {
            tokenParaGuardar.put("fechaObtencion", isoFormat.format(new Date()));
        }
        
        db.collection(COLLECTION_GOOGLE_TOKENS)
            .document(userId)
            .set(tokenParaGuardar)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Token de Google Calendar guardado exitosamente");
                if (callback != null) {
                    callback.onSuccess(tokenParaGuardar);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al guardar token de Google Calendar", e);
                if (callback != null) {
                    callback.onError(e);
                }
            });
    }
    
    /**
     * Obtiene el token de acceso de Google del usuario
     * Verifica si el token está expirado
     * Consistente con React: calendarService.js - obtenerTokenGoogle()
     */
    public void obtenerTokenGoogle(FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }
        
        String userId = firebaseUser.getUid();
        
        db.collection(COLLECTION_GOOGLE_TOKENS)
            .document(userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        Map<String, Object> tokenData = document.getData();
                        if (tokenData != null) {
                            // Verificar si el token está expirado
                            if (esTokenExpirado(tokenData)) {
                                Log.d(TAG, "Token de Google Calendar expirado, eliminando");
                                eliminarTokenGoogle(new FirestoreCallback() {
                                    @Override
                                    public void onSuccess(Object result) {
                                        if (callback != null) {
                                            callback.onSuccess(null);
                                        }
                                    }
                                    
                                    @Override
                                    public void onError(Exception exception) {
                                        if (callback != null) {
                                            callback.onSuccess(null);
                                        }
                                    }
                                });
                            } else {
                                if (callback != null) {
                                    callback.onSuccess(tokenData);
                                }
                            }
                        } else {
                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    }
                } else {
                    Log.e(TAG, "Error al obtener token de Google Calendar", task.getException());
                    if (callback != null) {
                        callback.onSuccess(null); // No es crítico, retornar null
                    }
                }
            });
    }
    
    /**
     * Elimina el token de acceso (desconecta Google Calendar)
     * Consistente con React: calendarService.js - eliminarTokenGoogle()
     */
    public void eliminarTokenGoogle(FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }
        
        String userId = firebaseUser.getUid();
        
        db.collection(COLLECTION_GOOGLE_TOKENS)
            .document(userId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Token de Google Calendar eliminado exitosamente");
                if (callback != null) {
                    callback.onSuccess(null);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al eliminar token de Google Calendar", e);
                if (callback != null) {
                    callback.onError(e);
                }
            });
    }
    
    /**
     * Verifica si el token está expirado
     * Consistente con React: googleAuthHelper.js - esTokenExpirado()
     */
    private boolean esTokenExpirado(Map<String, Object> tokenData) {
        if (tokenData == null) {
            return true;
        }
        
        Object fechaObtencionObj = tokenData.get("fechaObtencion");
        Object expiresInObj = tokenData.get("expires_in");
        
        if (fechaObtencionObj == null || expiresInObj == null) {
            return false; // No sabemos si está expirado, asumir que no
        }
        
        try {
            String fechaObtencionStr = fechaObtencionObj.toString();
            long expiresIn = ((Number) expiresInObj).longValue();
            
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date fechaObtencion = isoFormat.parse(fechaObtencionStr);
            
            if (fechaObtencion == null) {
                return false;
            }
            
            Date fechaExpiracion = new Date(fechaObtencion.getTime() + (expiresIn * 1000));
            Date ahora = new Date();
            
            // Considerar expirado si falta menos de 5 minutos
            return ahora.after(fechaExpiracion) || 
                   (fechaExpiracion.getTime() - ahora.getTime()) < (5 * 60 * 1000);
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar expiración del token", e);
            return false;
        }
    }
    
    /**
     * Verifica si el usuario tiene Google Calendar conectado
     * Consistente con React: calendarService.js - tieneGoogleCalendarConectado()
     */
    public void tieneGoogleCalendarConectado(FirestoreCallback callback) {
        obtenerTokenGoogle(new FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                boolean conectado = result != null && 
                    result instanceof Map && 
                    ((Map<?, ?>) result).containsKey("access_token");
                
                if (callback != null) {
                    callback.onSuccess(conectado);
                }
            }
            
            @Override
            public void onError(Exception exception) {
                if (callback != null) {
                    callback.onSuccess(false);
                }
            }
        });
    }
    
    /**
     * Interfaz para callbacks
     */
    public interface FirestoreCallback {
        void onSuccess(Object result);
        void onError(Exception exception);
    }
}

