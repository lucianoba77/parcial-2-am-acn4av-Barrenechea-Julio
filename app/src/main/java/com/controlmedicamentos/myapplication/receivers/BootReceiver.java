package com.controlmedicamentos.myapplication.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.utils.AlarmScheduler;

import java.util.List;

/**
 * Receiver para reprogramar alarmas cuando el dispositivo se reinicia
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            Log.d(TAG, "Dispositivo reiniciado, reprogramando alarmas...");
            reprogramarAlarmas(context);
        }
    }
    
    /**
     * Reprograma todas las alarmas de medicamentos activos
     */
    private void reprogramarAlarmas(Context context) {
        FirebaseService firebaseService = new FirebaseService();
        AlarmScheduler alarmScheduler = new AlarmScheduler(context);
        
        // Obtener todos los medicamentos activos
        firebaseService.obtenerMedicamentosActivos(new FirebaseService.FirestoreListCallback() {
            @Override
            public void onSuccess(List<?> result) {
                if (result != null && !result.isEmpty()) {
                    List<Medicamento> medicamentos = (List<Medicamento>) result;
                    Log.d(TAG, "Reprogramando alarmas para " + medicamentos.size() + " medicamentos");
                    
                    for (Medicamento medicamento : medicamentos) {
                        // Solo programar para medicamentos activos con tomas programadas
                        if (medicamento.isActivo() && !medicamento.isPausado() && 
                            medicamento.getTomasDiarias() > 0) {
                            alarmScheduler.programarAlarmasMedicamento(medicamento);
                        }
                    }
                    
                    Log.d(TAG, "Alarmas reprogramadas exitosamente");
                } else {
                    Log.d(TAG, "No hay medicamentos activos para reprogramar");
                }
            }
            
            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error al reprogramar alarmas: " + 
                      (exception != null ? exception.getMessage() : "Error desconocido"));
            }
        });
    }
}

