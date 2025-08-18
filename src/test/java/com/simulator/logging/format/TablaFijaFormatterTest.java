package com.simulator.logging.format;

import com.simulator.logging.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class TablaFijaFormatterTest {

    @Test
    void formateaCabeceraYSimpleLinea() {
        TablaFijaFormatter f = new TablaFijaFormatter();
        String header = f.cabecera();
        String sep = f.separador();

        // Cabecera básica
        assertTrue(header.contains("TIMESTAMP"),
                () -> "Header no contiene TIMESTAMP.\nHEADER:\n" + header);
        assertTrue(header.contains("EVENTO"),
                () -> "Header no contiene EVENTO.\nHEADER:\n" + header);

        // Acepta separador con '+' o sólo con '-'
        boolean separadorOk = sep.contains("+") || sep.chars().allMatch(c -> c=='-');
        assertTrue(separadorOk, () -> "Separador inesperado.\nSEPARADOR:\n" + sep);

        // Línea de ejemplo (ojo: el timestamp se formatea en zona local)
        LogMeta meta = new LogMeta(Instant.parse("2025-08-15T10:00:00Z"),
                LogNivel.INFO, LogEvento.CREAR_PROCESO);
        LogDatos datos = new LogDatos(1, "NEW", 12, 45, "FCFS", null, "rafaga=8, prioridad=3");

        String linea = f.formatear(meta, datos);

        // Verificaciones por contenido real (sin etiquetas PID=/CPU=/MEM=)
        assertTrue(linea.contains("CREAR_PROCESO"), () -> "LINEA:\n" + linea);
        assertTrue(linea.contains("NEW"), () -> "LINEA:\n" + linea);
        assertTrue(linea.contains("12%"), () -> "LINEA:\n" + linea);
        assertTrue(linea.contains("45MB"), () -> "LINEA:\n" + linea);
        assertTrue(linea.contains("FCFS"), () -> "LINEA:\n" + linea);

        // PID como número en su columna (tolerante a espacios)
        Pattern pidCol = Pattern.compile("\\|\\s*1\\s*\\|");
        assertTrue(pidCol.matcher(linea).find(),
                () -> "No se encontró la columna PID con '1'.\nLINEA:\n" + linea);
    }
}
