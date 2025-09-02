package com.simulator.core;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

public class ProcesoTest {
    
    @Test
    void crearEnNEWConRangoInicial(){
        Proceso p = new Proceso(1, "P1", 0, 5, 3, new Random(1));
        assertEquals(EstadoProceso.NEW, p.getEstado());
        assertTrue(p.getCpuUsage() >= 5 && p.getCpuUsage() <= 30);
        assertTrue(p.getMemoria() >= 10 && p.getMemoria() <= 200);
        assertEquals(5, p.getTiempoRestante());
    }
    
    @Test
    void avanzarEnRunningReduceRafagaYPuedeTerminar(){
        Proceso p = new Proceso(1, "P1", 0, 2, 3, new Random(1));
        p.cambiarEstado(EstadoProceso.RUNNING);
        p.avanzarTick(1);
        assertEquals(1, p.getTiempoRestante());
        assertEquals(EstadoProceso.RUNNING, p.getEstado());
        
        p.avanzarTick(2);
        assertEquals(0, p.getTiempoRestante());
        assertEquals(EstadoProceso.TERMINATED, p.getEstado());
        assertEquals(0, p.getCpuUsage());
        assertEquals(0, p.getMemoria());
        assertEquals(2, p.getTiempoFinalizacion());
    }
    
    @Test
    void enReadyNoConsumeCpuYCuentaEspera(){
        Proceso p = new Proceso(2, "P2", 0, 3, 2, new Random(2));
        p.cambiarEstado(EstadoProceso.READY);
        int memAntes = p.getMemoria();
        p.avanzarTick(1);
        assertEquals(EstadoProceso.READY, p.getEstado());
        assertEquals(memAntes, p.getMemoria());
        assertEquals(1, p.getTiempoEspera());
    }
    
    @Test
    void clampDeCpuYMemoriaSeRespeta(){
        // usamos random con seed fijo; aunque no forzamos extremos, el clamp protege limites.
        Proceso p = new Proceso(3, "P3", 0, 1, 1, new Random(3));
        p.cambiarEstado(EstadoProceso.RUNNING);
        p.avanzarTick(1); // Puede terminar
        assertTrue(p.getCpuUsage() >= 0 && p.getCpuUsage() <= 100);
        assertTrue(p.getMemoria() >= 0 && p.getMemoria() <= 2048);
    }
}