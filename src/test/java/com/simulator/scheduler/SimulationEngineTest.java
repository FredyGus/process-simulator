package com.simulator.scheduler;

import com.simulator.model.PCB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulationEngineTest {

    private SimulationEngine engine;

    @BeforeEach
    void setUp() {
        // Usamos FCFS para esta prueba
        engine = new SimulationEngine(new FCFS());
    }

    @Test
    void testTickDecrementsRemainingTimeAndIncrementsTime() {
        // Creamos un proceso con burstTime = 5
        PCB p = new PCB("P1", 0, 5, 1);
        engine.addProcess(p);

        // Antes de tick: tiempo=0, remainingTime=5
        assertEquals(0, engine.getCurrentTime());
        assertEquals(5, p.getRemainingTime());

        // Un tick de simulación
        engine.tick();

        // Después de tick: tiempo=1, remainingTime=4
        assertEquals(1, engine.getCurrentTime(),
            "El timepo de simulación debe incrementarse en 1");
        assertEquals(4, p.getRemainingTime(),
            "remainingTime debe decrementarse en 1");
    }

    @Test
    void testProcessFinishesAndEngineBecomesIdle() {
        // Proceso con burstTime = 2
        PCB p = new PCB("P2", 0, 2, 1);
        engine.addProcess(p);

        // Dos ticks: agota el proceso
        engine.tick();  // remainingTime -> 1
        engine.tick();  // remainingTime -> 0

        assertEquals(2, engine.getCurrentTime());
        assertEquals(0, p.getRemainingTime(),
            "remainingTime debe llegar a 0");

        // Un tick más: ya no hay procesos
        engine.tick();
        assertTrue(engine.isFinished(),
            "La simulación debe marcarse como finalizada");
        assertNull(engine.getCurrentProcess(),
            "No debe haber proceso corriendo");
    }
}
