package com.simulator.logging;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Logger centralizado para la simulación de procesos.
 * Escribe eventos en el archivo "simulation.log".
 */
public class SimulatorLogger {
    private static final Logger logger = Logger.getLogger("SimulatorLogger");

    static {
        try {
            // Archivo de log (append = true)
            // Handler fileHandler = new FileHandler("simulation.log", true);
            
            // append = false -> Limpia el log al iniciar
            // Handler fileHandler = new FileHandler("simulation.log", false); 
            
            // maximo 1 MB por archivo, hasta 5 archivos de backup y append = true
            Handler fileHandler = new FileHandler("simulation.log", 1024*1024, 5, true);
            
            fileHandler.setFormatter(new Formatter() {
                private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
                @Override
                public String format(LogRecord record) {
                    String timestamp = sdf.format(new Date(record.getMillis()));
                    return String.format("[%s] %s%n", timestamp, record.getMessage());
                }
            });
            logger.addHandler(fileHandler);
            // Evitar mensajes en consola además del archivo
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            // Mensaje sencillo para el usuario 
            System.err.println("Error configurando SimulatorLogger: " + e.getMessage());
            
            // Stack trace completo para debug
            e.printStackTrace();
        }
    }

    /**
     * Registra un evento simple en el log.
     * @param message Mensaje a registrar.
     */
    public static void log(String message) {
        logger.info(message);
    }

    /**
     * Registra un evento con detalles tipo clave=valor.
     * @param event   Nombre del evento
     * @param details Cadena con detalle, por ejemplo "PID=3 | State=READY"
     */
    public static void logEvent(String event, String details) {
        log(event + " | " + details);
    }
}
