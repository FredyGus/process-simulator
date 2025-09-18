package com.simulator;

import com.simulator.logging.*;
import com.simulator.logging.format.TablaFijaFormatter;
import com.simulator.logging.rotate.RotacionPorTamano;
import com.simulator.time.RelojDelSistema;

import java.nio.file.Paths;

public class LoggerQuickDemo {

    public static void main(String[] args) {
        var logger = new LoggerSistema();
        var config = LogConfig.basica(Paths.get("simulador.log"), new RotacionPorTamano(256 * 1024, 3));
        var writer = new FileLogWriter();
        var formatter = new TablaFijaFormatter();

        logger.iniciar(config, writer, formatter, new RelojDelSistema());

        // Inicio de simulación
        logger.registrar(LogEvento.INICIO_SIMULACION, LogNivel.INFO,
                new LogDatos(null, "READY", null, null, "FCFS", null, "tickMs=500, probNuevo=0.35"));

        var rnd = new java.util.Random();
        for (int i = 0; i < 5; i++) {
            int cpu = 10 + rnd.nextInt(90);    // CPU entre 10 y 99
            int mem = 20 + rnd.nextInt(400);   // Memoria entre 20 y 419
            logger.registrar(LogEvento.EJECUTAR_TICK, LogNivel.INFO,
                    new LogDatos(1, "RUNNING", cpu, mem, "FCFS", null, "rafagaRestante=" + (7 - i)));
        }

        // Fin de simulación
        logger.registrar(LogEvento.TERMINAR_PROCESO, LogNivel.INFO,
                new LogDatos(1, "TERMINATED", 0, 0, "FCFS", null, "fin_natural"));

        logger.finalizar();
    }
}
