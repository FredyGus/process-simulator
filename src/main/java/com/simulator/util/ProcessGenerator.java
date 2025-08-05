package com.simulator.util;

import com.simulator.model.PCB;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generador de procesos aleatorios para la simulación.
 */
public class ProcessGenerator {
    private static final AtomicInteger count = new AtomicInteger(1);
    private final Random random = new Random();

    private final int maxArrivalTime;
    private final int maxBurstTime;
    private final int maxPriority; 

    /**
     * Crea un generador con rangos máximos.
     * @param maxArrivalTime valor máximo para el tiempo de llegada
     * @param maxBurstTime   valor máximo para la ráfaga de CPU
     * @param maxPriority    valor máximo de prioridad (mínimo 1)
     */
    public ProcessGenerator(int maxArrivalTime, int maxBurstTime, int maxPriority) {
        this.maxArrivalTime = maxArrivalTime;
        this.maxBurstTime = maxBurstTime;
        this.maxPriority = maxPriority;
    }

    /**
     * Genera un solo PCB aleatorio.
     * @return un nuevo PCB
     */
    public PCB generateProcess() {
        String name = "P" + count.getAndIncrement();
        int arrival = random.nextInt(maxArrivalTime + 1);
        int burst = random.nextInt(maxBurstTime) + 1; // al menos 1
        int priority = random.nextInt(maxPriority) + 1; // 1..maxPriority
        return new PCB(name, arrival, burst, priority);
    }

    /**
     * Genera un arreglo de PCB aleatorios.
     * @param n cantidad de procesos a generar
     * @return arreglo de PCBs
     */
    public PCB[] generateProcesses(int n) {
        PCB[] processes = new PCB[n];
        for (int i = 0; i < n; i++) {
            processes[i] = generateProcess();
        }
        return processes;
    }
}