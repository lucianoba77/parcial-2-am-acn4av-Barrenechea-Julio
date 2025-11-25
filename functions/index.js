/**
 * Cloud Functions for Firebase - Control Medicamentos
 *
 * Función: Intercambiar auth code de Google Calendar por access token
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

// Inicializar Firebase Admin
admin.initializeApp();

/**
 * Intercambia el server auth code de Google por un access token
 * Esta función se ejecuta cuando se crea o actualiza un documento en googleTokens
 * con pendiente_intercambio = true
 */
exports.intercambiarGoogleCalendarToken = functions.firestore
    .document("googleTokens/{userId}")
    .onWrite(async (change, context) => {
      const userId = context.params.userId;
      const data = change.after.exists ? change.after.data() : null;

      // Si el documento fue eliminado o no existe, no hacer nada
      if (!data) {
        console.log(`Documento ${userId} eliminado, no se procesa`);
        return null;
      }

      // Solo procesar si está pendiente de intercambio
      if (!data.pendiente_intercambio) {
        console.log(`Documento ${userId} no está pendiente de intercambio`);
        return null;
      }

      // Verificar que tenga el server_auth_code
      if (!data.server_auth_code) {
        console.error(`Documento ${userId} no tiene server_auth_code`);
        return null;
      }

      console.log(`Procesando intercambio de token para usuario: ${userId}`);

      try {
        // Obtener las credenciales de configuración
        // Prioridad: environment variables > functions.config (deprecated)
        const config = functions.config();
        const googleConfig = config && config.google;
        const clientId = process.env.GOOGLE_CLIENT_ID ||
            (googleConfig && googleConfig.client_id);
        const clientSecret = process.env.GOOGLE_CLIENT_SECRET ||
            (googleConfig && googleConfig.client_secret);

        if (!clientId || !clientSecret) {
          console.error("Client ID o Client Secret no configurados");
          throw new Error("Client ID o Client Secret no configurados. " +
          "Configura usando: firebase functions:secrets:set GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET");
        }

        // Intercambiar el auth code por access token
        // Usar URLSearchParams para enviar los datos como form-urlencoded
        const params = new URLSearchParams();
        params.append("code", data.server_auth_code);
        params.append("client_id", clientId);
        params.append("client_secret", clientSecret);
        params.append("grant_type", "authorization_code");
        // Para aplicaciones móviles
        params.append("redirect_uri", "urn:ietf:wg:oauth:2.0:oob");

        const tokenUrl = "https://oauth2.googleapis.com/token";
        const tokenResponse = await axios.post(tokenUrl,
            params.toString(),
            {
              headers: {
                "Content-Type": "application/x-www-form-urlencoded",
              },
            },
        );

        const tokenData = tokenResponse.data;

        // Preparar los datos para guardar en Firestore
        const tokenParaGuardar = {
          access_token: tokenData.access_token,
          refresh_token: tokenData.refresh_token,
          token_type: tokenData.token_type || "Bearer",
          expires_in: tokenData.expires_in,
          scope: tokenData.scope,
          fechaObtencion: admin.firestore.FieldValue.serverTimestamp(),
          fechaExpiracion: admin.firestore.Timestamp.fromMillis(
              Date.now() + (tokenData.expires_in * 1000),
          ),
          pendiente_intercambio: false,
          email: data.email || null,
        };

        // Actualizar el documento en Firestore
        await admin.firestore()
            .collection("googleTokens")
            .doc(userId)
            .update(tokenParaGuardar);

        console.log(`Token intercambiado exitosamente para usuario: ${userId}`);

        // Limpiar el server_auth_code por seguridad (ya no es necesario)
        await admin.firestore()
            .collection("googleTokens")
            .doc(userId)
            .update({
              server_auth_code: admin.firestore.FieldValue.delete(),
            });

        return null;
      } catch (error) {
        console.error(`Error al intercambiar token para usuario ${userId}:`, error);

        // Guardar el error en el documento para debugging
        await admin.firestore()
            .collection("googleTokens")
            .doc(userId)
            .update({
              error_intercambio: error.message ||
                  "Error desconocido al intercambiar token",
              fecha_error: admin.firestore.FieldValue.serverTimestamp(),
              pendiente_intercambio: false, // Marcar como procesado para evitar loops
            });

        return null;
      }
    });

/**
 * Refresca el access token cuando está próximo a expirar
 * Esta función puede ser llamada desde la app cuando detecta
 * que el token está expirado
 */
exports.refrescarGoogleCalendarToken = functions.https.onCall(async (data, context) => {
  // Verificar autenticación
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "El usuario debe estar autenticado",
    );
  }

  const userId = context.auth.uid;

  try {
    // Obtener el documento del token
    const tokenDoc = await admin.firestore()
        .collection("googleTokens")
        .doc(userId)
        .get();

    if (!tokenDoc.exists) {
      throw new functions.https.HttpsError(
          "not-found",
          "No se encontró token de Google Calendar para este usuario",
      );
    }

    const tokenData = tokenDoc.data();

    if (!tokenData.refresh_token) {
      throw new functions.https.HttpsError(
          "failed-precondition",
          "No hay refresh token disponible. Necesitas reconectar Google Calendar.",
      );
    }

    // Obtener las credenciales
    // Prioridad: environment variables > functions.config (deprecated)
    const config = functions.config();
    const clientId = process.env.GOOGLE_CLIENT_ID ||
        (config && config.google && config.google.client_id);
    const clientSecret = process.env.GOOGLE_CLIENT_SECRET ||
        (config && config.google && config.google.client_secret);

    if (!clientId || !clientSecret) {
      throw new functions.https.HttpsError(
          "internal",
          "Error de configuración del servidor. " +
          "Client ID o Client Secret no configurados. " +
          "Configura usando: firebase functions:config:set",
      );
    }

    // Intercambiar refresh token por nuevo access token
    // Usar URLSearchParams para enviar los datos como form-urlencoded
    const params = new URLSearchParams();
    params.append("refresh_token", tokenData.refresh_token);
    params.append("client_id", clientId);
    params.append("client_secret", clientSecret);
    params.append("grant_type", "refresh_token");

    const tokenUrl = "https://oauth2.googleapis.com/token";
    const tokenResponse = await axios.post(tokenUrl,
        params.toString(),
        {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
        },
    );

    const newTokenData = tokenResponse.data;

    // Actualizar el documento
    const tokenParaGuardar = {
      access_token: newTokenData.access_token,
      token_type: newTokenData.token_type || "Bearer",
      expires_in: newTokenData.expires_in,
      fechaObtencion: admin.firestore.FieldValue.serverTimestamp(),
      fechaExpiracion: admin.firestore.Timestamp.fromMillis(
          Date.now() + (newTokenData.expires_in * 1000),
      ),
    };

    // Si viene un nuevo refresh_token (raro, pero puede pasar), actualizarlo
    if (newTokenData.refresh_token) {
      tokenParaGuardar.refresh_token = newTokenData.refresh_token;
    }

    await admin.firestore()
        .collection("googleTokens")
        .doc(userId)
        .update(tokenParaGuardar);

    return {
      success: true,
      message: "Token refrescado exitosamente",
    };
  } catch (error) {
    console.error(`Error al refrescar token para usuario ${userId}:`, error);

    if (error instanceof functions.https.HttpsError) {
      throw error;
    }

    throw new functions.https.HttpsError(
        "internal",
        `Error al refrescar token: ${error.message}`,
    );
  }
});

