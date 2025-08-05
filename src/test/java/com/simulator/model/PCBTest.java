package com.simulator.model;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PCBTest {

    @Test
    void testPidAutoIncrement() {
        PCB p1 = new PCB("P1", 0, 10, 1);
        PCB p2 = new PCB("P2", 1, 5, 2);
        assertEquals(p1.getPid() + 1, p2.getPid(),
            "El PID debe incrementarse automáticamente");
    }

    @Test
    void testGettersAndSetters() {
        PCB pcb = new PCB("Test", 2, 20, 3);
        // Comprobamos getters
        assertEquals("Test", pcb.getName());
        assertEquals(2, pcb.getArrivalTime());
        assertEquals(20, pcb.getBurstTime());

        // Comprobamos setters
        pcb.setRemainingTime(15);
        assertEquals(15, pcb.getRemainingTime(),
            "remainingTime debe actualizarse");

        pcb.setPriority(4);
        assertEquals(4, pcb.getPriority(),
            "priority debe actualizarse");

        pcb.setState(PCB.State.RUNNING);
        assertEquals(PCB.State.RUNNING, pcb.getState(),
            "state debe actualizarse a RUNNING");
    }
}
