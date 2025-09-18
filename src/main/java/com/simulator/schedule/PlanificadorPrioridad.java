package com.simulator.schedule;

import com.simulator.core.EstadoProceso;
import com.simulator.core.Proceso;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlanificadorPrioridad implements Planificador {

    private final List<Proceso> ready = new ArrayList<>();

    private static final Comparator<Proceso> COMP
            = Comparator.comparingInt(Proceso::getPrioridad)
                    .thenComparingInt(Proceso::getPid);

    @Override
    public void agregarProceso(Proceso p) {
        if (p.getEstado() != EstadoProceso.TERMINATED) {
            ready.add(p);
            ready.sort(COMP);
        }
    }

    @Override
    public Proceso seleccionarProceso() {
        if (ready.isEmpty()) {
            return null;
        }
        for (Proceso p : ready) {
            if (p.getEstado() != EstadoProceso.TERMINATED) {
                return p;
            }
        }
        return null;
    }

    @Override
    public void removerProceso(Proceso p) {
        ready.remove(p);
    }

    @Override
    public void reinicializar() {
        ready.clear();
    }
}
