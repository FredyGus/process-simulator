package com.simulator.scheduler;

import com.simulator.model.PCB;

/**
 * Interfaz para los algoritmos de planificación de CPU.
 */
public interface Scheduler {
    /**
     * Añade un proceso a la cola interna del scheduler.
     * @param pcb el PCB del proceso a planificar
     */
    void addProcess(PCB pcb);

    /**
     * Devuelve el siguiente proceso a ejecutar según el algoritmo.
     * Cambia el estado del proceso a RUNNING.
     * @return el PCB seleccionado o null si no hay más procesos
     */
    PCB getNextProcess();

    /**
     * Indica si aún quedan procesos pendientes de ejecutar.
     * @return true si la cola no está vacía
     */
    boolean hasNextProcess();
}
