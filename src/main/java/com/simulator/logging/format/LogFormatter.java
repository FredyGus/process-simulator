package com.simulator.logging.format;

import com.simulator.logging.LogDatos;
import com.simulator.logging.LogMeta;

public interface LogFormatter {
    String cabecera();
    String separador();
    String formatear(LogMeta meta, LogDatos datos);
}