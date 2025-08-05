package com.simulator.scheduler;

import com.simulator.model.PCB;

/**
 * Motor de simulación: avanza el tiempo en ticks,
 * asigna la CPU y decrementa remainingTime.
 */
public class SimulationEngine {
    private final Scheduler scheduler;
    private PCB currentProcess;
    private int currentTime = 0;

    /**
     * Crea un motor de simulación usando el scheduler dado.
     * @param scheduler el algoritmo de planificación (FCFS, SJF, etc.)
     */
    public SimulationEngine(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.currentProcess = null;
    }

    /** @return el tiempo actual de simulación (en ticks) */
    public int getCurrentTime() {
        return currentTime;
    }

    /** @return el proceso que está corriendo en este tick, o null si no hay */
    public PCB getCurrentProcess() {
        return currentProcess;
    }

    /**
     * Añade un proceso al scheduler.
     * @param pcb el PCB a planificar
     */
    public void addProcess(PCB pcb) {
        scheduler.addProcess(pcb);
    }

    /**
     * Avanza un tick de simulación:
     * - Si no hay proceso en CPU o el actual ya terminó, obtiene uno nuevo.
     * - Si hay proceso, decrementa su remainingTime.
     * - Incrementa currentTime.
     */
    public void tick() {
        if (currentProcess == null || currentProcess.getRemainingTime() <= 0) {
            currentProcess = scheduler.getNextProcess();
        }
        if (currentProcess != null) {
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - 1);
        }
        currentTime++;
    }

    /**
     * Indica si la simulación ya terminó:
     * no hay proceso en CPU y el scheduler está vacío.
     */
    public boolean isFinished() {
        return currentProcess == null && !scheduler.hasNextProcess();
    }
}
