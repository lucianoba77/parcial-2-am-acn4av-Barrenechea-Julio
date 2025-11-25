package com.controlmedicamentos.myapplication.services;

import android.util.Log;

import com.controlmedicamentos.myapplication.models.Medicamento;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Servicio para interactuar con la API de Google Calendar
 * Crea, actualiza y elimina eventos en Google Calendar
 */
public class GoogleCalendarService {
    private static final String TAG = "GoogleCalendarService";
    private static final String CALENDAR_API_BASE_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private OkHttpClient httpClient;
    
    public GoogleCalendarService() {
        this.httpClient = new OkHttpClient();
    }
    
    /**
     * Crea un evento en Google Calendar para una toma de medicamento
     * Consistente con React: calendarService.js - crearEventoToma()
     */
    public void crearEventoToma(String accessToken, Medicamento medicamento, String fecha, String hora, 
                                CalendarCallback callback) {
        try {
            Calendar fechaCompleta = Calendar.getInstance();
            String[] partesFecha = fecha.split("-");
            String[] partesHora = hora.split(":");
            
            if (partesFecha.length == 3 && partesHora.length == 2) {
                fechaCompleta.set(
                    Integer.parseInt(partesFecha[0]),
                    Integer.parseInt(partesFecha[1]) - 1,
                    Integer.parseInt(partesFecha[2]),
                    Integer.parseInt(partesHora[0]),
                    Integer.parseInt(partesHora[1]),
                    0
                );
            } else {
                if (callback != null) {
                    callback.onError(new Exception("Formato de fecha u hora inválido"));
                }
                return;
            }
            
            Calendar fechaFin = (Calendar) fechaCompleta.clone();
            fechaFin.add(Calendar.MINUTE, 15); // Evento de 15 minutos
            
            // Obtener zona horaria del dispositivo
            String timeZone = java.util.TimeZone.getDefault().getID();
            
            // Crear objeto JSON del evento
            JSONObject evento = new JSONObject();
            evento.put("summary", "💊 " + medicamento.getNombre());
            evento.put("description", "Toma de " + medicamento.getNombre() + "\n" +
                       "Presentación: " + medicamento.getPresentacion() + "\n" +
                       "Condición: " + (medicamento.getAfeccion() != null ? medicamento.getAfeccion() : "N/A") + "\n" +
                       "Stock: " + medicamento.getStockActual() + "/" + 
                       (medicamento.getDiasTratamiento() > 0 ? medicamento.getDiasTratamiento() : medicamento.getStockInicial()));
            
            // Fecha inicio
            JSONObject start = new JSONObject();
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            start.put("dateTime", isoFormat.format(fechaCompleta.getTime()));
            start.put("timeZone", timeZone);
            evento.put("start", start);
            
            // Fecha fin
            JSONObject end = new JSONObject();
            end.put("dateTime", isoFormat.format(fechaFin.getTime()));
            end.put("timeZone", timeZone);
            evento.put("end", end);
            
            // Recordatorios
            JSONObject reminders = new JSONObject();
            reminders.put("useDefault", false);
            JSONArray overrides = new JSONArray();
            JSONObject reminder1 = new JSONObject();
            reminder1.put("method", "popup");
            reminder1.put("minutes", 15); // Recordatorio 15 min antes
            overrides.put(reminder1);
            JSONObject reminder2 = new JSONObject();
            reminder2.put("method", "popup");
            reminder2.put("minutes", 5); // Recordatorio 5 min antes
            overrides.put(reminder2);
            reminders.put("overrides", overrides);
            evento.put("reminders", reminders);
            
            // Color
            evento.put("colorId", obtenerColorId(medicamento.getColor()));
            
            // Propiedades extendidas
            JSONObject extendedProperties = new JSONObject();
            JSONObject privateProps = new JSONObject();
            privateProps.put("medicamentoId", medicamento.getId());
            privateProps.put("tipo", "toma_medicamento");
            extendedProperties.put("private", privateProps);
            evento.put("extendedProperties", extendedProperties);
            
            // Crear request
            RequestBody body = RequestBody.create(evento.toString(), JSON);
            Request request = new Request.Builder()
                .url(CALENDAR_API_BASE_URL)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
            
            // Ejecutar request
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Error al crear evento en Google Calendar", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Error desconocido";
                        Log.e(TAG, "Error al crear evento: " + response.code() + " - " + errorBody);
                        if (callback != null) {
                            callback.onError(new Exception("Error al crear evento: " + errorBody));
                        }
                        return;
                    }
                    
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "{}";
                        JSONObject eventoCreado = new JSONObject(responseBody);
                        String eventoId = eventoCreado.getString("id");
                        
                        Log.d(TAG, "Evento creado exitosamente en Google Calendar: " + eventoId);
                        if (callback != null) {
                            callback.onSuccess(eventoId, eventoCreado);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error al parsear respuesta de Google Calendar", e);
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error al crear evento en Google Calendar", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    /**
     * Actualiza un evento existente en Google Calendar
     * Consistente con React: calendarService.js - actualizarEventoToma()
     */
    public void actualizarEventoToma(String accessToken, String eventoId, Medicamento medicamento, 
                                     String fecha, String hora, CalendarCallback callback) {
        try {
            Calendar fechaCompleta = Calendar.getInstance();
            String[] partesFecha = fecha.split("-");
            String[] partesHora = hora.split(":");
            
            if (partesFecha.length == 3 && partesHora.length == 2) {
                fechaCompleta.set(
                    Integer.parseInt(partesFecha[0]),
                    Integer.parseInt(partesFecha[1]) - 1,
                    Integer.parseInt(partesFecha[2]),
                    Integer.parseInt(partesHora[0]),
                    Integer.parseInt(partesHora[1]),
                    0
                );
            } else {
                if (callback != null) {
                    callback.onError(new Exception("Formato de fecha u hora inválido"));
                }
                return;
            }
            
            Calendar fechaFin = (Calendar) fechaCompleta.clone();
            fechaFin.add(Calendar.MINUTE, 15);
            
            String timeZone = java.util.TimeZone.getDefault().getID();
            
            // Crear objeto JSON del evento
            JSONObject evento = new JSONObject();
            evento.put("summary", "💊 " + medicamento.getNombre());
            evento.put("description", "Toma de " + medicamento.getNombre() + "\n" +
                       "Presentación: " + medicamento.getPresentacion() + "\n" +
                       "Condición: " + (medicamento.getAfeccion() != null ? medicamento.getAfeccion() : "N/A") + "\n" +
                       "Stock: " + medicamento.getStockActual() + "/" + 
                       (medicamento.getDiasTratamiento() > 0 ? medicamento.getDiasTratamiento() : medicamento.getStockInicial()));
            
            JSONObject start = new JSONObject();
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            start.put("dateTime", isoFormat.format(fechaCompleta.getTime()));
            start.put("timeZone", timeZone);
            evento.put("start", start);
            
            JSONObject end = new JSONObject();
            end.put("dateTime", isoFormat.format(fechaFin.getTime()));
            end.put("timeZone", timeZone);
            evento.put("end", end);
            
            JSONObject reminders = new JSONObject();
            reminders.put("useDefault", false);
            JSONArray overrides = new JSONArray();
            JSONObject reminder1 = new JSONObject();
            reminder1.put("method", "popup");
            reminder1.put("minutes", 15);
            overrides.put(reminder1);
            JSONObject reminder2 = new JSONObject();
            reminder2.put("method", "popup");
            reminder2.put("minutes", 5);
            overrides.put(reminder2);
            reminders.put("overrides", overrides);
            evento.put("reminders", reminders);
            
            evento.put("colorId", obtenerColorId(medicamento.getColor()));
            
            // Crear request
            RequestBody body = RequestBody.create(evento.toString(), JSON);
            Request request = new Request.Builder()
                .url(CALENDAR_API_BASE_URL + "/" + eventoId)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .put(body)
                .build();
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Error al actualizar evento en Google Calendar", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Error desconocido";
                        Log.e(TAG, "Error al actualizar evento: " + response.code() + " - " + errorBody);
                        if (callback != null) {
                            callback.onError(new Exception("Error al actualizar evento: " + errorBody));
                        }
                        return;
                    }
                    
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "{}";
                        JSONObject eventoActualizado = new JSONObject(responseBody);
                        
                        Log.d(TAG, "Evento actualizado exitosamente en Google Calendar");
                        if (callback != null) {
                            callback.onSuccess(eventoId, eventoActualizado);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error al parsear respuesta de Google Calendar", e);
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error al actualizar evento en Google Calendar", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    /**
     * Elimina un evento de Google Calendar
     * Consistente con React: calendarService.js - eliminarEventoToma()
     */
    public void eliminarEventoToma(String accessToken, String eventoId, CalendarCallback callback) {
        Request request = new Request.Builder()
            .url(CALENDAR_API_BASE_URL + "/" + eventoId)
            .addHeader("Authorization", "Bearer " + accessToken)
            .delete()
            .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error al eliminar evento de Google Calendar", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() && response.code() != 404) {
                    String errorBody = response.body() != null ? response.body().string() : "Error desconocido";
                    Log.e(TAG, "Error al eliminar evento: " + response.code() + " - " + errorBody);
                    if (callback != null) {
                        callback.onError(new Exception("Error al eliminar evento: " + errorBody));
                    }
                    return;
                }
                
                Log.d(TAG, "Evento eliminado exitosamente de Google Calendar: " + eventoId);
                if (callback != null) {
                    callback.onSuccess(eventoId, null);
                }
            }
        });
    }
    
    /**
     * Crea eventos recurrentes para todas las tomas de un medicamento
     * Para medicamentos crónicos, crea eventos para 90 días
     * Para medicamentos ocasionales (tomasDiarias === 0), no crea eventos
     * Consistente con React: calendarService.js - crearEventosRecurrentes()
     */
    public void crearEventosRecurrentes(String accessToken, Medicamento medicamento, 
                                       RecurrentEventsCallback callback) {
        // No crear eventos para medicamentos ocasionales
        if (medicamento.getTomasDiarias() == 0) {
            if (callback != null) {
                callback.onSuccess(new ArrayList<>());
            }
            return;
        }
        
        List<String> eventoIds = new ArrayList<>();
        Calendar fechaHoy = Calendar.getInstance();
        
        // Determinar cuántos días de eventos crear
        int diasTratamiento;
        if (medicamento.getDiasTratamiento() == -1) {
            // Medicamento crónico: crear eventos para 90 días
            diasTratamiento = 90;
        } else {
            // Medicamento con fin de tratamiento: usar días de tratamiento o 30 por defecto
            diasTratamiento = medicamento.getDiasTratamiento() > 0 ? medicamento.getDiasTratamiento() : 30;
        }
        
        // Calcular todas las horas de toma
        List<String> horasToma = new ArrayList<>();
        String primeraToma = medicamento.getHorarioPrimeraToma();
        if (primeraToma == null || primeraToma.isEmpty()) {
            primeraToma = "00:00";
        }
        
        String[] partes = primeraToma.split(":");
        int horaInicial = Integer.parseInt(partes[0]);
        int minutoInicial = partes.length > 1 ? Integer.parseInt(partes[1]) : 0;
        
        int intervalo = 24 / medicamento.getTomasDiarias();
        for (int i = 0; i < medicamento.getTomasDiarias(); i++) {
            int hora = (horaInicial + (i * intervalo)) % 24;
            int minuto = (i == 0) ? minutoInicial : 0;
            horasToma.add(String.format("%02d:%02d", hora, minuto));
        }
        
        // Crear eventos para cada día del tratamiento
        // Limitar a 100 eventos por vez para evitar sobrecarga de la API
        final int maxEventos = 100;
        final int[] eventosCreados = {0};
        final int[] eventosPendientes = {diasTratamiento * horasToma.size()};
        
        for (int dia = 0; dia < diasTratamiento && eventosCreados[0] < maxEventos; dia++) {
            Calendar fecha = (Calendar) fechaHoy.clone();
            fecha.add(Calendar.DAY_OF_YEAR, dia);
            String fechaStr = String.format("%04d-%02d-%02d", 
                fecha.get(Calendar.YEAR),
                fecha.get(Calendar.MONTH) + 1,
                fecha.get(Calendar.DAY_OF_MONTH));
            
            for (String horaToma : horasToma) {
                if (eventosCreados[0] >= maxEventos) {
                    break;
                }
                
                crearEventoToma(accessToken, medicamento, fechaStr, horaToma, 
                    new CalendarCallback() {
                        @Override
                        public void onSuccess(String eventoId, Object evento) {
                            eventoIds.add(eventoId);
                            eventosCreados[0]++;
                            eventosPendientes[0]--;
                            
                            if (eventosPendientes[0] == 0 || eventosCreados[0] >= maxEventos) {
                                if (callback != null) {
                                    callback.onSuccess(eventoIds);
                                }
                            }
                        }
                        
                        @Override
                        public void onError(Exception exception) {
                            eventosPendientes[0]--;
                            Log.w(TAG, "Error al crear evento individual, continuando con los demás", exception);
                            
                            if (eventosPendientes[0] == 0 || eventosCreados[0] >= maxEventos) {
                                if (callback != null) {
                                    callback.onSuccess(eventoIds);
                                }
                            }
                        }
                    });
            }
        }
    }
    
    /**
     * Convierte el color del medicamento a un colorId de Google Calendar
     * Consistente con React: calendarService.js - obtenerColorId()
     */
    private String obtenerColorId(int colorInt) {
        // Convertir color ARGB a hexadecimal
        String colorHex = String.format("#%06X", (0xFFFFFF & colorInt));
        
        // Mapeo de colores hex a colorId de Google Calendar (1-11)
        switch (colorHex.toUpperCase()) {
            case "#FFFFFF":
                return "1"; // Lavanda
            case "#FFB6C1":
                return "11"; // Rosa
            case "#ADD8E6":
                return "9"; // Azul
            case "#F5F5DC":
                return "5"; // Amarillo
            case "#E6E6FA":
                return "3"; // Púrpura
            case "#90EE90":
                return "10"; // Verde
            case "#FFFF00":
                return "5"; // Amarillo
            case "#FFA500":
                return "6"; // Naranja
            case "#800080":
                return "3"; // Púrpura
            case "#00BFFF":
                return "9"; // Azul
            case "#00FF00":
                return "10"; // Verde
            case "#FF0000":
                return "11"; // Rojo
            default:
                return "1"; // Por defecto
        }
    }
    
    /**
     * Interfaz para callbacks de operaciones de calendario
     */
    public interface CalendarCallback {
        void onSuccess(String eventoId, Object evento);
        void onError(Exception exception);
    }
    
    /**
     * Interfaz para callbacks de eventos recurrentes
     */
    public interface RecurrentEventsCallback {
        void onSuccess(List<String> eventoIds);
        void onError(Exception exception);
    }
}

