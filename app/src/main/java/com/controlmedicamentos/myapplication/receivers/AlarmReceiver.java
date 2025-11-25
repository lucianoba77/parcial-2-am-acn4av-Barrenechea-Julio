package com.controlmedicamentos.myapplication.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.controlmedicamentos.myapplication.services.NotificationService;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.models.Medicamento;

/**
 * Receiver para manejar las alarmas programadas de medicamentos
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    private static final String EXTRA_MEDICAMENTO_ID = "medicamento_id";
    private static final String EXTRA_HORARIO = "horario";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarma recibida");
        
        String medicamentoId = intent.getStringExtra(EXTRA_MEDICAMENTO_ID);
        String horario = intent.getStringExtra(EXTRA_HORARIO);
        
        if (medicamentoId == null || horario == null) {
            Log.e(TAG, "Datos de medicamento incompletos en la alarma");
            return;
        }
        
        // Obtener el medicamento desde Firebase
        FirebaseService firebaseService = new FirebaseService();
        firebaseService.obtenerMedicamento(medicamentoId, new FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof Medicamento) {
                    Medicamento medicamento = (Medicamento) result;
                    
                    // Verificar que el medicamento esté activo
                    if (medicamento.isActivo() && !medicamento.isPausado()) {
                        // Enviar notificación
                        NotificationService notificationService = new NotificationService(context);
                        notificationService.enviarNotificacionMedicamento(medicamento, horario);
                        Log.d(TAG, "Notificación enviada para: " + medicamento.getNombre());
                    } else {
                        Log.d(TAG, "Medicamento no activo, no se envía notificación: " + medicamento.getNombre());
                    }
                } else {
                    Log.e(TAG, "No se pudo obtener el medicamento desde Firebase");
                }
            }
            
            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error al obtener medicamento desde Firebase: " + 
                      (exception != null ? exception.getMessage() : "Error desconocido"));
            }
        });
    }
    
    /**
     * Crea un Intent para la alarma de un medicamento
     */
    public static Intent createIntent(Context context, String medicamentoId, String horario) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(EXTRA_MEDICAMENTO_ID, medicamentoId);
        intent.putExtra(EXTRA_HORARIO, horario);
        return intent;
    }
}

