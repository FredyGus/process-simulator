package com.simulator.schedule;

import com.simulator.core.EstadoProceso;
import com.simulator.core.Proceso;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public final class PlanificadorFCFS implements Planificador {

    private final Queue<Proceso> cola = new ArrayDeque<>();

    @Override
    public void agregarProceso(Proceso p) {
        Objects.requireNonNull(p, "proceso");
        cola.offer(p);
    }

    @Override
    public Proceso seleccionarProceso() {
        while (!cola.isEmpty()) {
            Proceso head = cola.peek();
            if (head.getEstado() == EstadoProceso.READY || head.getEstado() == EstadoProceso.RUNNING) {
                return head;
            }
            cola.poll();
        }
        return null;
    }

    @Override
    public void removerProceso(Proceso p) {
        if (p != null) {
            cola.remove(p);
        }
    }

    @Override
    public void reinicializar() {
        cola.clear();
    }

    int size() {
        return cola.size();
    }
}
