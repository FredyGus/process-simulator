package com.simulator.scheduler;

import com.simulator.logging.SimulatorLogger;
import com.simulator.model.PCB;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;


/**
 * Scheduler SJF (Shortest-Job-First) non-preemptive.
 * Siempre escoge el proceso con ráfaga restante más corta.
 */

public class SJF implements Scheduler {
    private final Queue<PCB> queue = new PriorityQueue<>(Comparator.comparing(PCB::getBurstTime));
    
    @Override
    public void addProcess(PCB pcb) {
        pcb.setState(PCB.State.READY);
        SimulatorLogger.logEvent("ADD_PROCESS_SJF",
            "PID=" + pcb.getPid() + " | burst=" + pcb.getBurstTime());
        queue.add(pcb);
    }

    @Override
    public PCB getNextProcess() {
        PCB pcb = queue.poll();
        if (pcb != null) {
            pcb.setState(PCB.State.RUNNING);
            SimulatorLogger.logEvent("DISPATCH_SJF",
                "PID=" + pcb.getPid() + " | burst=" + pcb.getBurstTime());
        }
        return pcb;
    }

    @Override
    public boolean hasNextProcess() {
        return !queue.isEmpty();
    }
}

