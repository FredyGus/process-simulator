package com.simulator.schedule;

import com.simulator.core.EstadoProceso;
import com.simulator.core.Proceso;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class PlanificadorRRTest {

    private Proceso listo(int pid) {
        Proceso p = new Proceso(pid, "P"+pid, 0, 10, 3, new Random(pid));
        p.cambiarEstado(EstadoProceso.READY);
        return p;
    }

    @Test
    void rotaEnOrdenConQuantum2() {
        PlanificadorRR rr = new PlanificadorRR(2);
        Proceso p1 = listo(1), p2 = listo(2), p3 = listo(3);
        rr.agregarProceso(p1);
        rr.agregarProceso(p2);
        rr.agregarProceso(p3);

        // 1er quantum de p1
        assertEquals(1, rr.seleccionarProceso().getPid());
        rr.onTick(p1); rr.onTick(p1);
        assertTrue(rr.debePreemptar(p1));
        rr.rotar(p1);

        // debería venir p2
        assertEquals(2, rr.seleccionarProceso().getPid());
        rr.onTick(p2); rr.onTick(p2);
        assertTrue(rr.debePreemptar(p2));
        rr.rotar(p2);

        // luego p3
        assertEquals(3, rr.seleccionarProceso().getPid());
    }

    @Test
    void alTerminarNoSeReinserta() {
        PlanificadorRR rr = new PlanificadorRR(1);
        Proceso p1 = listo(1);
        Proceso p2 = listo(2);
        rr.agregarProceso(p1);
        rr.agregarProceso(p2);

        // corre p1 un tick y "termina"
        // (simulamos terminación removiéndolo; en el simulador real lo hará el modelo)
        rr.onTick(p1);
        assertTrue(rr.debePreemptar(p1)); // quantum agotado
        rr.removerProceso(p1);

        // debe quedar p2 al frente
        assertEquals(2, rr.seleccionarProceso().getPid());
    }
}
