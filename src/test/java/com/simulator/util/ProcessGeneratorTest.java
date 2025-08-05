package com.simulator.util;

import com.simulator.model.PCB;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProcessGeneratorTest {

    @Test
    void testGenerateSingleProcessWithinRanges() {
        int maxArrival = 10;
        int maxBurst = 8;
        int maxPriority = 5;
        ProcessGenerator gen = new ProcessGenerator(maxArrival, maxBurst, maxPriority);

        PCB p = gen.generateProcess();
        assertNotNull(p, "El proceso no debe ser null");
        assertTrue(p.getArrivalTime() >= 0 && p.getArrivalTime() <= maxArrival,
            "arrivalTime debe estar entre 0 y " + maxArrival);
        assertTrue(p.getBurstTime() >= 1 && p.getBurstTime() <= maxBurst,
            "burstTime debe estar entre 1 y " + maxBurst);
        assertTrue(p.getPriority() >= 1 && p.getPriority() <= maxPriority,
            "priority debe estar entre 1 y " + maxPriority);
    }

    @Test
    void testGenerateMultipleProcessesUniqueNames() {
        ProcessGenerator gen = new ProcessGenerator(5, 5, 3);
        int n = 10;
        PCB[] arr = gen.generateProcesses(n);
        assertNotNull(arr);
        assertEquals(n, arr.length, "Debe generar exactamente n procesos");

        // Verificar nombres únicos
        java.util.Set<String> names = new java.util.HashSet<>();
        for (PCB p : arr) {
            names.add(p.getName());
        }
        assertEquals(n, names.size(), "Los nombres de procesos deben ser únicos");
    }
}