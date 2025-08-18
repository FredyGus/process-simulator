package com.simulator;

import com.simulator.logging.*;
import com.simulator.logging.format.TablaFijaFormatter;
import com.simulator.logging.rotate.RotacionPorTamano;
import com.simulator.time.RelojDelSistema;

import java.nio.file.Paths;

public class LoggerQuickDemo {
    public static void main(String[] args) {
        var logger = LoggerSistema.get();
        var config = LogConfig.basica(Paths.get("simulador.log"), new RotacionPorTamano(256 * 1024, 3));
        var writer = new FileLogWriter();
        var formatter = new TablaFijaFormatter();

        logger.iniciar(config, writer, formatter, new RelojDelSistema());

        logger.registrar(LogEvento.INICIO_SIMULACION, LogNivel.INFO,
                new LogDatos(null, "READY", null, null, "FCFS", null, "tickMs=500, probNuevo=0.35"));

        logger.registrar(LogEvento.CREAR_PROCESO, LogNivel.INFO,
                new LogDatos(1, "NEW", 12, 45, "FCFS", null, "rafaga=8, prioridad=3"));

        logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                new LogDatos(1, "RUNNING", 56, 120, "FCFS", null, "READY→RUNNING"));

        logger.registrar(LogEvento.TERMINAR_PROCESO, LogNivel.INFO,
                new LogDatos(1, "TERMINATED", 0, 0, "FCFS", null, "fin_natural"));

        logger.finalizar();
    }
}