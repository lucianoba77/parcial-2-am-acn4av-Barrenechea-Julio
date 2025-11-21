package com.controlmedicamentos.myapplication.utils;

/**
 * Utilidad para asignar colores automáticamente a medicamentos
 * Consistente con la lógica de React (colores.js)
 */
public class ColorUtils {
    
    // Colores pasteles disponibles (mismos que en React)
    public static final String[] COLORES_HEX = {
        "#FFB6C1", // Rosa pastel
        "#ADD8E6", // Azul pastel
        "#B0E0E6", // Verde pastel (Sky Blue)
        "#FFFACD", // Amarillo pastel (Lemon Chiffon)
        "#E6E6FA"  // Lavanda pastel
    };
    
    /**
     * Obtiene el color para un medicamento según su índice
     * Los colores se asignan en orden cíclico (0-4, luego se repiten)
     * Consistente con React: obtenerColorPorIndice()
     * 
     * @param indice Índice del medicamento (generalmente la cantidad de medicamentos existentes)
     * @return Color en formato hexadecimal (ej: "#FFB6C1")
     */
    public static String obtenerColorPorIndice(int indice) {
        int indiceColor = indice % COLORES_HEX.length;
        return COLORES_HEX[indiceColor];
    }
    
    /**
     * Convierte un color hexadecimal a int ARGB
     * 
     * @param hexColor Color en formato hexadecimal (ej: "#FFB6C1" o "FFB6C1")
     * @return Color como int ARGB
     */
    public static int hexToInt(String hexColor) {
        try {
            // Remover el # si está presente
            if (hexColor.startsWith("#")) {
                hexColor = hexColor.substring(1);
            }
            
            // Si tiene 6 caracteres, añadir alpha FF
            if (hexColor.length() == 6) {
                hexColor = "FF" + hexColor;
            }
            
            return (int) Long.parseLong(hexColor, 16);
        } catch (Exception e) {
            // Valor por defecto: azul pastel
            return 0xFFADD8E6;
        }
    }
    
    /**
     * Convierte un color int ARGB a hexadecimal
     * 
     * @param colorInt Color como int ARGB
     * @return Color en formato hexadecimal (ej: "#FFB6C1")
     */
    public static String intToHex(int colorInt) {
        return String.format("#%06X", (0xFFFFFF & colorInt));
    }
}

