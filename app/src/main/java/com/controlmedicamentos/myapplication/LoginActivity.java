package com.controlmedicamentos.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.utils.NetworkUtils;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import android.util.Log;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnRegister;
    private TextView tvOlvidoPassword;
    private ProgressBar progressBar;
    private AuthService authService;
    private boolean isLoginMode = true; // true = login, false = registro

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar servicio de autenticación
        authService = new AuthService();

        // Verificar si ya hay un usuario autenticado
        verificarSesionExistente();

        // Inicializar vistas
        inicializarVistas();

        // Configurar listeners
        configurarListeners();
    }

    private void verificarSesionExistente() {
        if (authService.isUserLoggedIn()) {
            // Usuario ya autenticado, ir a MainActivity
            irAMainActivity();
        }
    }

    private void inicializarVistas() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvOlvidoPassword = findViewById(R.id.tvOlvidoPassword);
        
        // Intentar encontrar ProgressBar en el layout (si existe)
        progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void configurarListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoginMode = true;
                realizarLogin();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoginMode = false;
                realizarRegistro();
            }
        });

        // Listener para recuperar contraseña
        if (tvOlvidoPassword != null) {
            tvOlvidoPassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mostrarDialogoRecuperarPassword();
                }
            });
        }
    }

    private void realizarLogin() {
        // Obtener datos de los campos
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Logging detallado para debug
        Log.d(TAG, "=== Intento de Login ===");
        Log.d(TAG, "Email ingresado: " + email);
        Log.d(TAG, "Longitud de contraseña: " + password.length());
        Log.d(TAG, "Contraseña contiene espacios: " + password.contains(" "));

        // Validar campos
        if (!validarCampos(email, password)) {
            return;
        }

        // Verificar conexión a internet
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }

        // Mostrar progreso
        mostrarProgreso(true);

        // Realizar login con Firebase
        authService.loginUser(email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                mostrarProgreso(false);
                Log.d(TAG, "Login exitoso. Usuario UID: " + (user != null ? user.getUid() : "null"));
                Toast.makeText(LoginActivity.this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                irAMainActivity();
            }

            @Override
            public void onError(Exception exception) {
                mostrarProgreso(false);
                
                // Logging detallado del error
                if (exception != null) {
                    Log.e(TAG, "=== Error en Login ===");
                    Log.e(TAG, "Tipo de excepción: " + exception.getClass().getName());
                    Log.e(TAG, "Mensaje de error: " + exception.getMessage());
                    
                    if (exception instanceof com.google.firebase.auth.FirebaseAuthException) {
                        com.google.firebase.auth.FirebaseAuthException authException = 
                            (com.google.firebase.auth.FirebaseAuthException) exception;
                        String errorCode = authException.getErrorCode();
                        Log.e(TAG, "Código de error Firebase: " + errorCode);
                        
                        // Mostrar el código de error también en el mensaje para debug
                        String mensaje = obtenerMensajeErrorLogin(exception);
                        Log.e(TAG, "Mensaje traducido: " + mensaje);
                        
                        // Para debug: mostrar el código de error en el Toast también
                        Toast.makeText(LoginActivity.this, mensaje + "\n(Código: " + errorCode + ")", 
                            Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "Error completo: ", exception);
                        String mensaje = obtenerMensajeErrorLogin(exception);
                        Toast.makeText(LoginActivity.this, mensaje, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e(TAG, "Error desconocido: exception es null");
                    Toast.makeText(LoginActivity.this, "Error desconocido al iniciar sesión", 
                        Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void realizarRegistro() {
        // Obtener datos de los campos
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validar campos
        if (!validarCampos(email, password)) {
            return;
        }

        // Verificar conexión a internet
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }

        // Validar que la contraseña sea más segura para registro
        if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            return;
        }

        // Mostrar progreso
        mostrarProgreso(true);

        // Realizar registro con Firebase
        authService.registerUser(email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                mostrarProgreso(false);
                Toast.makeText(LoginActivity.this, "¡Registro exitoso! Bienvenido", Toast.LENGTH_SHORT).show();
                irAMainActivity();
            }

            @Override
            public void onError(Exception exception) {
                mostrarProgreso(false);
                String mensaje = obtenerMensajeErrorRegistro(exception);
                Log.e(TAG, "Error en registro: ", exception);
                Toast.makeText(LoginActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void irAMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Cerrar LoginActivity
    }

    private void mostrarProgreso(boolean mostrar) {
        if (progressBar != null) {
            progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        }
        btnLogin.setEnabled(!mostrar);
        btnRegister.setEnabled(!mostrar);
        etEmail.setEnabled(!mostrar);
        etPassword.setEnabled(!mostrar);
    }

    private boolean validarCampos(String email, String password) {
        boolean valido = true;

        // Validar email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingresa tu email");
            valido = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Ingresa un email válido");
            valido = false;
        } else {
            etEmail.setError(null);
        }

        // Validar contraseña
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Ingresa tu contraseña");
            valido = false;
        } else if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            valido = false;
        } else {
            etPassword.setError(null);
        }

        return valido;
    }

    /**
     * Obtiene un mensaje de error amigable para errores de login
     */
    private String obtenerMensajeErrorLogin(Exception exception) {
        if (exception == null) {
            return "Error al iniciar sesión. Intenta nuevamente.";
        }

        if (exception instanceof FirebaseAuthException) {
            FirebaseAuthException authException = (FirebaseAuthException) exception;
            String errorCode = authException.getErrorCode();
            
            Log.d(TAG, "Procesando error de Firebase. Código: " + errorCode);

            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                    return "El formato del email es incorrecto";
                case "ERROR_WRONG_PASSWORD":
                    return "Contraseña incorrecta. Verifica que no tengas espacios al inicio o final.";
                case "ERROR_USER_NOT_FOUND":
                    return "No existe una cuenta con este email. Verifica el email o crea una cuenta.";
                case "ERROR_USER_DISABLED":
                    return "Esta cuenta ha sido deshabilitada. Contacta al soporte.";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Demasiados intentos fallidos. Espera unos minutos e intenta nuevamente.";
                case "ERROR_OPERATION_NOT_ALLOWED":
                    return "Esta operación no está permitida. Contacta al soporte.";
                case "ERROR_INVALID_CREDENTIAL":
                    return "Email o contraseña incorrectos. Verifica tus credenciales.";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "Error de conexión. Verifica tu internet.";
                case "ERROR_WEAK_PASSWORD":
                    return "La contraseña es muy débil.";
                default:
                    Log.w(TAG, "Código de error no reconocido: " + errorCode);
                    return "Error al iniciar sesión. Código: " + errorCode;
            }
        }

        // Si no es un FirebaseAuthException, buscar en el mensaje
        String errorMessage = exception.getMessage();
        if (errorMessage != null) {
            errorMessage = errorMessage.toLowerCase();
            if (errorMessage.contains("wrong-password") || errorMessage.contains("invalid-credential")) {
                return "Email o contraseña incorrectos";
            } else if (errorMessage.contains("user-not-found")) {
                return "No existe una cuenta con este email";
            } else if (errorMessage.contains("invalid-email")) {
                return "El formato del email es incorrecto";
            } else if (errorMessage.contains("network")) {
                return "Error de conexión. Verifica tu internet.";
            }
        }

        return "Error al iniciar sesión. Verifica tus credenciales.";
    }

    /**
     * Obtiene un mensaje de error amigable para errores de registro
     */
    private String obtenerMensajeErrorRegistro(Exception exception) {
        if (exception == null) {
            return "Error al registrar usuario. Intenta nuevamente.";
        }

        if (exception instanceof FirebaseAuthException) {
            FirebaseAuthException authException = (FirebaseAuthException) exception;
            String errorCode = authException.getErrorCode();

            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                    return "El formato del email es incorrecto";
                case "ERROR_WEAK_PASSWORD":
                    return "La contraseña es muy débil. Debe tener al menos 6 caracteres";
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "Este email ya está registrado. Inicia sesión en su lugar.";
                case "ERROR_OPERATION_NOT_ALLOWED":
                    return "Esta operación no está permitida";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "Error de conexión. Verifica tu internet.";
                default:
                    return "Error al registrar: " + errorCode;
            }
        }

        // Si no es un FirebaseAuthException, buscar en el mensaje
        String errorMessage = exception.getMessage();
        if (errorMessage != null) {
            errorMessage = errorMessage.toLowerCase();
            if (errorMessage.contains("email-already-in-use")) {
                return "Este email ya está registrado. Inicia sesión en su lugar.";
            } else if (errorMessage.contains("invalid-email")) {
                return "El formato del email es incorrecto";
            } else if (errorMessage.contains("weak-password")) {
                return "La contraseña es muy débil. Debe tener al menos 6 caracteres";
            } else if (errorMessage.contains("network")) {
                return "Error de conexión. Verifica tu internet.";
            }
        }

        return "Error al registrar usuario. Verifica los datos e intenta nuevamente.";
    }

    /**
     * Muestra un diálogo para recuperar la contraseña
     */
    private void mostrarDialogoRecuperarPassword() {
        // Crear un diálogo personalizado con un campo de texto
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.reset_password_dialog_title);
        builder.setMessage(R.string.reset_password_dialog_message);

        // Crear un EditText para el email
        final TextInputLayout textInputLayout = new TextInputLayout(this);
        final TextInputEditText editText = new TextInputEditText(textInputLayout.getContext());
        editText.setHint(getString(R.string.login_email_hint));
        editText.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        // Pre-llenar con el email del campo de login si existe
        String emailActual = etEmail.getText().toString().trim();
        if (!TextUtils.isEmpty(emailActual)) {
            editText.setText(emailActual);
        }
        
        textInputLayout.addView(editText);
        textInputLayout.setPadding(
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            0
        );

        builder.setView(textInputLayout);

        builder.setPositiveButton("Enviar", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                String email = editText.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginActivity.this, "Por favor ingresa tu email", Toast.LENGTH_SHORT).show();
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(LoginActivity.this, "Ingresa un email válido", Toast.LENGTH_SHORT).show();
                } else {
                    enviarEmailRecuperacion(email);
                }
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    /**
     * Envía un email de recuperación de contraseña
     */
    private void enviarEmailRecuperacion(String email) {
        // Verificar conexión a internet
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }

        // Mostrar progreso (opcional, ya que es rápido)
        authService.resetPassword(email, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Toast.makeText(LoginActivity.this, 
                    getString(R.string.reset_password_success), 
                    Toast.LENGTH_LONG).show();
                Log.d(TAG, "Email de recuperación enviado a: " + email);
            }

            @Override
            public void onError(Exception exception) {
                String mensaje = getString(R.string.reset_password_error);
                if (exception != null && exception.getMessage() != null) {
                    if (exception.getMessage().contains("user-not-found")) {
                        mensaje = "No existe una cuenta con este email";
                    } else if (exception.getMessage().contains("invalid-email")) {
                        mensaje = "El formato del email es incorrecto";
                    } else {
                        mensaje = exception.getMessage();
                    }
                }
                Toast.makeText(LoginActivity.this, mensaje, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error al enviar email de recuperación", exception);
            }
        });
    }

}