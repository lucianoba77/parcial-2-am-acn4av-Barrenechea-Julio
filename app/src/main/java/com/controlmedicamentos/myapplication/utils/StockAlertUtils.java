package com.controlmedicamentos.myapplication.utils;

import android.util.Log;
import com.controlmedicamentos.myapplication.models.Medicamento;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utilidad para monitorear el stock de medicamentos y generar alertas
 * Consistente con React: hooks/useStockAlerts.js
 */
public class StockAlertUtils {
    private static final String TAG = "StockAlertUtils";
    private static final int DIAS_ANTES_ALERTA = 7; // Por defecto, alertar 7 días antes
    
    /**
     * Interfaz para recibir alertas de stock
     */
    public interface StockAlertListener {
        void onStockAgotado(Medicamento medicamento);
        void onStockBajo(Medicamento medicamento, int diasRestantes, String mensaje);
    }
    
    private static Set<String> alertados = new HashSet<>();
    
    /**
     * Verifica el stock de los medicamentos y genera alertas
     * Consistente con React: useStockAlerts.js líneas 13-101
     * 
     * @param medicamentos Lista de medicamentos a verificar
     * @param listener Listener para recibir las alertas
     * @param diasAntesAlerta Días antes de que se acabe el stock para alertar (por defecto 7)
     */
    public static void verificarStock(List<Medicamento> medicamentos, StockAlertListener listener, int diasAntesAlerta) {
        if (medicamentos == null || medicamentos.isEmpty()) {
            return;
        }
        
        int diasAlerta = diasAntesAlerta > 0 ? diasAntesAlerta : DIAS_ANTES_ALERTA;
        
        for (Medicamento medicamento : medicamentos) {
            if (!medicamento.isActivo()) {
                continue;
            }
            
            int stockActual = medicamento.getStockActual();
            int tomasDiarias = medicamento.getTomasDiarias();
            int diasTratamiento = medicamento.getDiasTratamiento();
            
            // Calcular días restantes de stock
            int diasRestantes = 0;
            if (tomasDiarias > 0) {
                diasRestantes = Math.floorDiv(stockActual, tomasDiarias);
            } else {
                // Para medicamentos ocasionales, no calcular días restantes
                diasRestantes = -1;
            }
            
            String medicamentoId = medicamento.getId();
            
            // Si el stock está en 0 o negativo
            if (stockActual <= 0) {
                String keyAgotado = medicamentoId + "-agotado";
                if (!alertados.contains(keyAgotado)) {
                    alertados.add(keyAgotado);
                    if (listener != null) {
                        listener.onStockAgotado(medicamento);
                    }
                    Log.w(TAG, "Stock agotado: " + medicamento.getNombre());
                }
            }
            // Verificar si el tratamiento tiene más días que el stock restante
            else if (diasTratamiento > 0 && diasRestantes >= 0 && diasRestantes < diasTratamiento) {
                // Alerta cuando quede 1 día
                if (diasRestantes == 1) {
                    String key1Dia = medicamentoId + "-1dia";
                    if (!alertados.contains(key1Dia)) {
                        alertados.add(key1Dia);
                        // Limpiar otras alertas
                        alertados.remove(medicamentoId + "-2dias");
                        alertados.remove(medicamentoId + "-3dias");
                        alertados.remove(medicamentoId + "-7dias");
                        
                        String mensaje = medicamento.getNombre() + ": Solo queda medicamento para 1 día. Debes agregar stock para completar el tratamiento.";
                        if (listener != null) {
                            listener.onStockBajo(medicamento, 1, mensaje);
                        }
                        Log.w(TAG, mensaje);
                    }
                }
                // Alerta cuando queden 2 días
                else if (diasRestantes == 2) {
                    String key2Dias = medicamentoId + "-2dias";
                    if (!alertados.contains(key2Dias)) {
                        alertados.add(key2Dias);
                        // Limpiar otras alertas
                        alertados.remove(medicamentoId + "-3dias");
                        alertados.remove(medicamentoId + "-7dias");
                        
                        String mensaje = medicamento.getNombre() + ": Queda medicamento para 2 días.";
                        if (listener != null) {
                            listener.onStockBajo(medicamento, 2, mensaje);
                        }
                        Log.w(TAG, mensaje);
                    }
                }
                // Alerta cuando queden 3 días
                else if (diasRestantes == 3) {
                    String key3Dias = medicamentoId + "-3dias";
                    if (!alertados.contains(key3Dias)) {
                        alertados.add(key3Dias);
                        // Limpiar otras alertas
                        alertados.remove(medicamentoId + "-7dias");
                        
                        String mensaje = medicamento.getNombre() + ": Queda medicamento para 3 días.";
                        if (listener != null) {
                            listener.onStockBajo(medicamento, 3, mensaje);
                        }
                        Log.w(TAG, mensaje);
                    }
                }
                // Alerta cuando queden 7 días o menos (pero más de 3)
                else if (diasRestantes <= diasAlerta && diasRestantes > 3) {
                    String key7Dias = medicamentoId + "-7dias";
                    if (!alertados.contains(key7Dias)) {
                        alertados.add(key7Dias);
                        
                        String mensaje = medicamento.getNombre() + ": El medicamento se acabará antes de terminar el tratamiento. Quedan aproximadamente " + diasRestantes + " días de stock.";
                        if (listener != null) {
                            listener.onStockBajo(medicamento, diasRestantes, mensaje);
                        }
                        Log.w(TAG, mensaje);
                    }
                } else {
                    // Si el stock se recuperó, remover todas las alertas
                    alertados.remove(medicamentoId + "-1dia");
                    alertados.remove(medicamentoId + "-2dias");
                    alertados.remove(medicamentoId + "-3dias");
                    alertados.remove(medicamentoId + "-7dias");
                    alertados.remove(medicamentoId + "-agotado");
                }
            } else {
                // Si el stock es suficiente, remover todas las alertas
                alertados.remove(medicamentoId + "-1dia");
                alertados.remove(medicamentoId + "-2dias");
                alertados.remove(medicamentoId + "-3dias");
                alertados.remove(medicamentoId + "-7dias");
                alertados.remove(medicamentoId + "-agotado");
            }
        }
        
        // Limpiar alertas de medicamentos que ya no existen
        Set<String> medicamentoIds = new HashSet<>();
        for (Medicamento med : medicamentos) {
            medicamentoIds.add(med.getId());
        }
        
        Set<String> alertadosAClear = new HashSet<>();
        for (String key : alertados) {
            String medicamentoId = key.split("-")[0];
            if (!medicamentoIds.contains(medicamentoId)) {
                alertadosAClear.add(key);
            }
        }
        alertados.removeAll(alertadosAClear);
    }
    
    /**
     * Limpia todas las alertas
     */
    public static void limpiarAlertas() {
        alertados.clear();
    }
}

