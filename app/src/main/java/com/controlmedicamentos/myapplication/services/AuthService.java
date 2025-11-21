package com.controlmedicamentos.myapplication.services;

import android.util.Log;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.annotation.NonNull;

/**
 * Servicio para manejar la autenticación con Firebase
 */
public class AuthService {
    private static final String TAG = "AuthService";
    private FirebaseAuth mAuth;

    public AuthService() {
        mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Obtiene el usuario actual autenticado
     * @return FirebaseUser si hay usuario autenticado, null en caso contrario
     */
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    /**
     * Verifica si hay un usuario autenticado
     * @return true si hay usuario autenticado, false en caso contrario
     */
    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    /**
     * Registra un nuevo usuario con email y contraseña
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param callback Callback para manejar el resultado
     */
    public void registerUser(String email, String password, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Usuario registrado exitosamente");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    } else {
                        Log.e(TAG, "Error al registrar usuario", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }

    /**
     * Inicia sesión con email y contraseña
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param callback Callback para manejar el resultado
     */
    public void loginUser(String email, String password, AuthCallback callback) {
        // Logging detallado para debug
        Log.d(TAG, "Intentando login con email: " + email);
        Log.d(TAG, "Longitud de contraseña: " + (password != null ? password.length() : 0));
        
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "Usuario autenticado exitosamente. UID: " + 
                            (user != null ? user.getUid() : "null"));
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    } else {
                        Exception exception = task.getException();
                        if (exception != null) {
                            Log.e(TAG, "Error al autenticar usuario", exception);
                            String errorMessage = exception.getMessage();
                            Log.e(TAG, "Mensaje de error: " + errorMessage);
                            
                            // Si es FirebaseAuthException, obtener el código de error
                            if (exception instanceof com.google.firebase.auth.FirebaseAuthException) {
                                com.google.firebase.auth.FirebaseAuthException authException = 
                                    (com.google.firebase.auth.FirebaseAuthException) exception;
                                String errorCode = authException.getErrorCode();
                                Log.e(TAG, "Código de error Firebase: " + errorCode);
                            }
                        }
                        if (callback != null) {
                            callback.onError(exception);
                        }
                    }
                }
            });
    }

    /**
     * Envía un email para recuperar la contraseña
     * @param email Email del usuario
     * @param callback Callback para manejar el resultado
     */
    public void resetPassword(String email, AuthCallback callback) {
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email de recuperación enviado");
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    } else {
                        Log.e(TAG, "Error al enviar email de recuperación", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }

    /**
     * Cierra la sesión del usuario actual
     */
    public void logout() {
        mAuth.signOut();
        Log.d(TAG, "Usuario cerró sesión");
    }

    /**
     * Interfaz para callbacks de autenticación
     */
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(Exception exception);
    }
}

