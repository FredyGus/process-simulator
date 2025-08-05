package com.simulator.scheduler;

import com.simulator.model.PCB;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Scheduler FCFS (First–Come, First–Served) sin preempción.
 */
public class FCFS implements Scheduler {
    private final Queue<PCB> queue = new LinkedList<>();

    @Override
    public void addProcess(PCB pcb) {
        // El proceso pasa a estado READY al añadirse
        pcb.setState(PCB.State.READY);
        queue.add(pcb);
    }

    @Override
    public PCB getNextProcess() {
        // Toma el siguiente de la cola FIFO
        PCB pcb = queue.poll();
        if (pcb != null) {
            pcb.setState(PCB.State.RUNNING);
        }
        return pcb;
    }

    @Override
    public boolean hasNextProcess() {
        return !queue.isEmpty();
    }
}
