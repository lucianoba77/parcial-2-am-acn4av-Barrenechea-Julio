package com.controlmedicamentos.myapplication.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.receivers.AlarmReceiver;

import java.util.Calendar;
import java.util.List;

/**
 * Utilidad para programar y cancelar alarmas de medicamentos
 */
public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";
    private Context context;
    private AlarmManager alarmManager;
    
    public AlarmScheduler(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    
    /**
     * Programa todas las alarmas para un medicamento
     */
    public void programarAlarmasMedicamento(Medicamento medicamento) {
        if (medicamento == null || medicamento.getId() == null) {
            Log.e(TAG, "Medicamento inválido para programar alarmas");
            return;
        }
        
        // Solo programar alarmas para medicamentos activos con tomas diarias > 0
        if (!medicamento.isActivo() || medicamento.isPausado() || 
            medicamento.getTomasDiarias() <= 0 || 
            medicamento.getHorarioPrimeraToma() == null || 
            medicamento.getHorarioPrimeraToma().isEmpty()) {
            Log.d(TAG, "Medicamento no requiere alarmas: " + medicamento.getNombre());
            return;
        }
        
        // Cancelar alarmas existentes primero
        cancelarAlarmasMedicamento(medicamento);
        
        List<String> horarios = medicamento.getHorariosTomas();
        if (horarios == null || horarios.isEmpty()) {
            Log.e(TAG, "No hay horarios para el medicamento: " + medicamento.getNombre());
            return;
        }
        
        Calendar ahora = Calendar.getInstance();
        
        // Programar alarmas para cada horario
        for (int i = 0; i < horarios.size(); i++) {
            String horario = horarios.get(i);
            
            // Parsear horario (formato "HH:mm")
            String[] partes = horario.split(":");
            if (partes.length != 2) {
                Log.e(TAG, "Formato de horario inválido: " + horario);
                continue;
            }
            
            try {
                int hora = Integer.parseInt(partes[0]);
                int minuto = Integer.parseInt(partes[1]);
                
                // Crear Calendar para la hora de la toma hoy
                Calendar horarioToma = Calendar.getInstance();
                horarioToma.set(Calendar.HOUR_OF_DAY, hora);
                horarioToma.set(Calendar.MINUTE, minuto);
                horarioToma.set(Calendar.SECOND, 0);
                horarioToma.set(Calendar.MILLISECOND, 0);
                
                // Si la hora ya pasó hoy, programar para mañana
                if (horarioToma.before(ahora)) {
                    horarioToma.add(Calendar.DAY_OF_YEAR, 1);
                }
                
                // Crear Intent para la alarma
                Intent intent = AlarmReceiver.createIntent(context, medicamento.getId(), horario);
                int requestCode = generarRequestCode(medicamento.getId(), i);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                // Programar alarma
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        horarioToma.getTimeInMillis(),
                        pendingIntent
                    );
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        horarioToma.getTimeInMillis(),
                        pendingIntent
                    );
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        horarioToma.getTimeInMillis(),
                        pendingIntent
                    );
                }
                
                // Programar alarmas recurrentes para los próximos 365 días
                programarAlarmasRecurrentes(medicamento, horario, i, horarioToma);
                
                Log.d(TAG, "Alarma programada para: " + medicamento.getNombre() + 
                      " a las " + horario + " (requestCode: " + requestCode + ")");
                
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error al parsear horario: " + horario, e);
            }
        }
    }
    
    /**
     * Programa alarmas recurrentes para los próximos días
     * OPTIMIZADO: Solo programa alarmas para los próximos 30 días para evitar exceder el límite de 500 alarmas
     */
    private void programarAlarmasRecurrentes(Medicamento medicamento, String horario, 
                                            int indiceHorario, Calendar primeraAlarma) {
        // LIMITAR: Solo programar alarmas para los próximos 30 días
        // Cuando se ejecute una alarma, se programará la siguiente en el ciclo
        // Esto evita exceder el límite de 500 alarmas concurrentes de Android
        final int DIAS_PROGRAMADOS = 30;
        
        // Si el medicamento tiene días de tratamiento definidos y no es crónico
        if (medicamento.getDiasTratamiento() > 0) {
            // Calcular cuántos días programar (el mínimo entre los días restantes y el límite)
            int diasRestantes = medicamento.getDiasTratamiento();
            
            // Calcular días desde el inicio del tratamiento hasta hoy
            if (medicamento.getFechaInicioTratamiento() != null) {
                Calendar hoy = Calendar.getInstance();
                Calendar inicio = Calendar.getInstance();
                inicio.setTime(medicamento.getFechaInicioTratamiento());
                long diffMillis = hoy.getTimeInMillis() - inicio.getTimeInMillis();
                int diasTranscurridos = (int) (diffMillis / (1000 * 60 * 60 * 24));
                diasRestantes = Math.max(0, medicamento.getDiasTratamiento() - diasTranscurridos);
            }
            
            int diasProgramar = Math.min(diasRestantes, DIAS_PROGRAMADOS);
            
            // Solo programar si quedan días de tratamiento
            if (diasProgramar <= 0) {
                Log.d(TAG, "No quedan días de tratamiento para programar alarmas: " + medicamento.getNombre());
                return;
            }
            
            for (int dia = 1; dia <= diasProgramar; dia++) {
                Calendar horarioToma = (Calendar) primeraAlarma.clone();
                horarioToma.add(Calendar.DAY_OF_YEAR, dia);
                
                Intent intent = AlarmReceiver.createIntent(context, medicamento.getId(), horario);
                int requestCode = generarRequestCode(medicamento.getId(), indiceHorario, dia);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                // Programar alarma
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            horarioToma.getTimeInMillis(),
                            pendingIntent
                        );
                    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            horarioToma.getTimeInMillis(),
                            pendingIntent
                        );
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            horarioToma.getTimeInMillis(),
                            pendingIntent
                        );
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error al programar alarma para día " + dia + ": " + medicamento.getNombre(), e);
                    // Si hay error (por ejemplo, límite alcanzado), detener la programación
                    if (e instanceof IllegalStateException && e.getMessage() != null && 
                        e.getMessage().contains("Maximum limit")) {
                        Log.w(TAG, "Límite de alarmas alcanzado. Deteniendo programación de alarmas adicionales.");
                        break;
                    }
                }
            }
        } else {
            // Medicamento crónico: programar solo para los próximos 30 días
            // Cuando se ejecute una alarma, se programará automáticamente la siguiente
            for (int dia = 1; dia <= DIAS_PROGRAMADOS; dia++) {
                Calendar horarioToma = (Calendar) primeraAlarma.clone();
                horarioToma.add(Calendar.DAY_OF_YEAR, dia);
                
                Intent intent = AlarmReceiver.createIntent(context, medicamento.getId(), horario);
                int requestCode = generarRequestCode(medicamento.getId(), indiceHorario, dia);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                // Programar alarma
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            horarioToma.getTimeInMillis(),
                            pendingIntent
                        );
                    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            horarioToma.getTimeInMillis(),
                            pendingIntent
                        );
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            horarioToma.getTimeInMillis(),
                            pendingIntent
                        );
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error al programar alarma para día " + dia + ": " + medicamento.getNombre(), e);
                    // Si hay error (por ejemplo, límite alcanzado), detener la programación
                    if (e instanceof IllegalStateException && e.getMessage() != null && 
                        e.getMessage().contains("Maximum limit")) {
                        Log.w(TAG, "Límite de alarmas alcanzado. Deteniendo programación de alarmas adicionales.");
                        break;
                    }
                }
            }
        }
        
        Log.d(TAG, "Alarmas recurrentes programadas (30 días) para: " + medicamento.getNombre() + 
              " horario " + horario);
    }
    
    /**
     * Cancela todas las alarmas de un medicamento
     */
    public void cancelarAlarmasMedicamento(Medicamento medicamento) {
        if (medicamento == null || medicamento.getId() == null) {
            return;
        }
        
        List<String> horarios = medicamento.getHorariosTomas();
        if (horarios == null) {
            horarios = new java.util.ArrayList<>();
        }
        
        // Cancelar alarmas para todos los horarios
        // OPTIMIZADO: Solo cancelar las alarmas que podrían estar programadas (30 días + 1 para el día 0)
        final int MAX_DIAS_CANCELAR = 31;
        
        for (int i = 0; i < horarios.size(); i++) {
            // Cancelar alarmas para los próximos días programados
            for (int dia = 0; dia <= MAX_DIAS_CANCELAR; dia++) {
                int requestCode = generarRequestCode(medicamento.getId(), i, dia);
                Intent intent = AlarmReceiver.createIntent(context, medicamento.getId(), horarios.get(i));
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
        
        Log.d(TAG, "Alarmas canceladas para: " + medicamento.getNombre());
        
        // También cancelar notificaciones pendientes
        com.controlmedicamentos.myapplication.services.NotificationService notificationService = 
            new com.controlmedicamentos.myapplication.services.NotificationService(context);
        notificationService.cancelarNotificacionesMedicamento(medicamento);
    }
    
    /**
     * Genera un código único para el requestCode de la alarma
     */
    private int generarRequestCode(String medicamentoId, int indiceHorario) {
        return generarRequestCode(medicamentoId, indiceHorario, 0);
    }
    
    /**
     * Genera un código único para el requestCode de la alarma (con día)
     */
    private int generarRequestCode(String medicamentoId, int indiceHorario, int dia) {
        // Usar hash del ID del medicamento + índice del horario + día
        int hash = medicamentoId.hashCode();
        return Math.abs(hash) + (indiceHorario * 1000) + dia;
    }
}

