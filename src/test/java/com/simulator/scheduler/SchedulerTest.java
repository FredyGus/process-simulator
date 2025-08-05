package com.simulator.scheduler;

import com.simulator.model.PCB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SchedulerTest {

    private FCFS scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new FCFS();
    }

    @Test
    void testAddAndHasNextProcess() {
        assertFalse(scheduler.hasNextProcess(), "Inicialmente no debe haber procesos");
        PCB p = new PCB("P1", 0, 5, 1);
        scheduler.addProcess(p);
        assertTrue(scheduler.hasNextProcess(), "Después de añadir, debe haber procesos");
    }

    @Test
    void testGetNextProcessOrderAndState() {
        PCB p1 = new PCB("P1", 0, 5, 1);
        PCB p2 = new PCB("P2", 1, 3, 1);

        scheduler.addProcess(p1);
        scheduler.addProcess(p2);

        // El primer proceso que sale debe ser p1
        PCB next = scheduler.getNextProcess();
        assertSame(p1, next, "FCFS debe devolver primero p1");
        assertEquals(PCB.State.RUNNING, p1.getState(), "p1 debe estar en estado RUNNING");

        // Ahora queda p2
        assertTrue(scheduler.hasNextProcess(), "Aún debe haber procesos");
        next = scheduler.getNextProcess();
        assertSame(p2, next, "El segundo debe ser p2");
        assertEquals(PCB.State.RUNNING, p2.getState(), "p2 debe estar en estado RUNNING");

        // Ya no quedan más
        assertFalse(scheduler.hasNextProcess(), "No debe haber más procesos");
    }

    @Test
    void testGetNextProcessWhenEmpty() {
        assertNull(scheduler.getNextProcess(), "Debe devolver null si no hay procesos");
    }
}
