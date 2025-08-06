package com.simulator.scheduler;

import com.simulator.model.PCB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SJFTest {

    private SJF scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SJF();
    }

    @Test
    void testOrderByBurstTime() {
        PCB p1 = new PCB("P1", 0, 8, 1);
        PCB p2 = new PCB("P2", 0, 3, 1);
        PCB p3 = new PCB("P3", 0, 5, 1);

        scheduler.addProcess(p1);
        scheduler.addProcess(p2);
        scheduler.addProcess(p3);

        // El primero debe ser p2 (burst = 3)
        PCB next = scheduler.getNextProcess();
        assertSame(p2, next);

        // Luego p3 (burst = 5)
        next = scheduler.getNextProcess();
        assertSame(p3, next);

        // Luego p1 (burst = 8)
        next = scheduler.getNextProcess();
        assertSame(p1, next);

        // Ninguno más
        assertFalse(scheduler.hasNextProcess());
    }

    @Test
    void testDispatchSetsRunningState() {
        PCB p = new PCB("P", 0, 2, 1);
        scheduler.addProcess(p);
        PCB next = scheduler.getNextProcess();
        assertEquals(PCB.State.RUNNING, next.getState());
    }
}
