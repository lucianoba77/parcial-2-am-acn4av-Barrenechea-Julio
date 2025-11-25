# Firebase Cloud Functions - Control Medicamentos

Este directorio contiene las Cloud Functions de Firebase para la aplicación Control Medicamentos.

## Funciones Implementadas

### 1. `intercambiarGoogleCalendarToken`
Intercambia automáticamente el `server_auth_code` de Google por un `access_token` cuando se guarda un nuevo token en Firestore con `pendiente_intercambio = true`.

**Trigger:** Se ejecuta cuando se crea o actualiza un documento en la colección `googleTokens`.

**Funcionalidad:**
- Intercambia el `server_auth_code` por `access_token` y `refresh_token` usando la API de Google OAuth
- Guarda el token en Firestore
- Marca `pendiente_intercambio` como `false`
- Elimina el `server_auth_code` por seguridad

### 2. `refrescarGoogleCalendarToken`
Refresca el `access_token` cuando está próximo a expirar usando el `refresh_token`.

**Tipo:** Callable Function (se puede llamar desde la app)

**Funcionalidad:**
- Verifica autenticación del usuario
- Usa el `refresh_token` para obtener un nuevo `access_token`
- Actualiza el documento en Firestore con el nuevo token

## Configuración

### 1. Instalar dependencias

```bash
cd functions
npm install
```

### 2. Configurar credenciales de Google OAuth

Antes de desplegar, necesitas configurar el Client ID y Client Secret de Google OAuth en Firebase Functions Config:

```bash
firebase functions:config:set google.client_id="TU_CLIENT_ID"
firebase functions:config:set google.client_secret="TU_CLIENT_SECRET"
```

**Nota:** El Client ID debe ser el mismo `default_web_client_id` que usas en la app Android.
El Client Secret lo obtienes de Google Cloud Console en la sección de credenciales OAuth 2.0.

### 3. Desplegar las funciones

```bash
firebase deploy --only functions
```

## Estructura de Datos en Firestore

### Colección: `googleTokens`
- **Documento ID:** `{userId}` (UID del usuario de Firebase Auth)

**Campos:**
- `server_auth_code` (string, temporal): Código de autorización recibido de Google Sign-In
- `access_token` (string): Token de acceso para usar la API de Google Calendar
- `refresh_token` (string): Token para refrescar el access_token
- `token_type` (string): Tipo de token (normalmente "Bearer")
- `expires_in` (number): Segundos hasta que expire el token
- `scope` (string): Scopes autorizados
- `fechaObtencion` (Timestamp): Fecha en que se obtuvo el token
- `fechaExpiracion` (Timestamp): Fecha en que expira el token
- `pendiente_intercambio` (boolean): Indica si está pendiente de intercambio
- `email` (string): Email del usuario de Google
- `error_intercambio` (string, opcional): Mensaje de error si falló el intercambio

## Desarrollo Local

Para probar las funciones localmente:

```bash
# Instalar Firebase CLI si no lo tienes
npm install -g firebase-tools

# Iniciar emulador de Firebase
firebase emulators:start --only functions
```

## Logs

Para ver los logs de las funciones:

```bash
firebase functions:log
```

Para ver logs en tiempo real:

```bash
firebase functions:log --follow
```

## Notas Importantes

1. **Seguridad:** El Client Secret NUNCA debe estar en la app móvil. Solo debe estar en las Cloud Functions.

2. **Refresh Token:** El refresh token solo se obtiene la primera vez. Si se pierde, el usuario necesitará reconectar Google Calendar.

3. **Límites:** Las funciones tienen límites de ejecución. Si el intercambio falla, se guarda el error en el documento para debugging.

