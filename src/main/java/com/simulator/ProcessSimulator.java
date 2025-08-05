package com.simulator;

import com.simulator.logging.SimulatorLogger;
import com.simulator.model.PCB;
import com.simulator.scheduler.FCFS;
import com.simulator.scheduler.SimulationEngine;
import com.simulator.util.ProcessGenerator;

/**
 * Clase principal de la aplicación: arranca una simulación básica
 * para probar la integración de todos los componentes.
 */
public class ProcessSimulator {
    public static void main(String[] args) {
        // Configuración de ejemplo
        ProcessGenerator gen = new ProcessGenerator(5, 5, 3);
        SimulationEngine engine = new SimulationEngine(new FCFS());

        // Generamos 3 procesos aleatorios y los añadimos al scheduler
        for (var pcb : gen.generateProcesses(3)) {
            SimulatorLogger.logEvent("CREAR_PROCESO",
                "PID=" + pcb.getPid() +
                " | arrival=" + pcb.getArrivalTime() +
                " | burst=" + pcb.getBurstTime());
            engine.addProcess(pcb);
        }

        SimulatorLogger.log("INICIO_SIMULACION");

        // Bucle de simulación: avanzamos hasta que no queden procesos
        while (!engine.isFinished()) {
            engine.tick();
            PCB current = engine.getCurrentProcess();
            if (current != null) {
                SimulatorLogger.logEvent("TICK",
                    "t=" + engine.getCurrentTime() +
                    " | PID=" + current.getPid() +
                    " | rem=" + current.getRemainingTime());
            }
        }

        SimulatorLogger.log("FIN_SIMULACION");
    }
}
