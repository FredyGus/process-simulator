package com.simulator.schedule;

import com.simulator.core.EstadoProceso;
import com.simulator.core.Proceso;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class PlanificadorFCFSTest {
    private Proceso nuevo (int pid) {
        // rafaga 5, prioridad 3; arribo 0; RNG fijo para reproducibilidad
        Proceso p = new Proceso (pid, "P"+pid, 0, 5, 3, new Random(pid));
        p.cambiarEstado(EstadoProceso.READY);
        return p;
    }
    
    @Test
    void mantieneOrdenDeLlegada(){
        PlanificadorFCFS fcfs = new PlanificadorFCFS();
        Proceso p1 = nuevo(1);
        Proceso p2 = nuevo(2);
        Proceso p3 = nuevo(3);
        
        fcfs.agregarProceso(p1);
        fcfs.agregarProceso(p2);
        fcfs.agregarProceso(p3);
        
        assertEquals(1, fcfs.seleccionarProceso().getPid());
        // Simulador no ha removido aun, sigue el mismo al frente
        assertEquals(1, fcfs.seleccionarProceso().getPid());
        
        // Cuando el simulador termine o saque a p1, lo removemos
        fcfs.removerProceso(p1);
        assertEquals(2, fcfs.seleccionarProceso().getPid());
        
        fcfs.removerProceso(p2);
        assertEquals(3, fcfs.seleccionarProceso().getPid());
    }
    
    @Test 
    void ignoraBloqueadosOTerminadosEnLaCabeza() {
        PlanificadorFCFS fcfs = new PlanificadorFCFS();
        Proceso p1 = nuevo(1);
        Proceso p2 = nuevo(2);
        
        fcfs.agregarProceso(p1);
        fcfs.agregarProceso(p2);
        
        // Si por error p1 se bloquea y no lo ha removido aun...
        p1.cambiarEstado(EstadoProceso.BLOCKED);
        
        // SeleccionarProceso() debe "Saltar" p1 y ofrecer p2
        assertEquals(2, fcfs.seleccionarProceso().getPid());
        // Y si pregunta de nuevo, deve seguir devolvviendo p2 (p1 se purgo)
        assertEquals(2, fcfs.seleccionarProceso().getPid());
    }
    
    @Test
    void reinicializarLimpiaCola(){
        PlanificadorFCFS fcfs = new PlanificadorFCFS();
        fcfs.agregarProceso(nuevo(1));
        fcfs.agregarProceso(nuevo(2));
        fcfs.reinicializar();
        assertNull(fcfs.seleccionarProceso());
    }
}