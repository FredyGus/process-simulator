package com.simulator.schedule;

import com.simulator.core.EstadoProceso;
import com.simulator.core.Proceso;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

// FCFS no-preemptivo: respeta orden de llegada
public final class PlanificadorFCFS implements Planificador {

    private final Queue<Proceso> cola = new ArrayDeque<>();

    @Override
    public void agregarProceso(Proceso p) {
        Objects.requireNonNull(p, "proceso");
        cola.offer(p);
    }

    @Override
    public Proceso seleccionarProceso() {
        // Devuelve el primer READY en la cola, limpia los que ya no aplican
        while (!cola.isEmpty()) {
            Proceso head = cola.peek();
            if (head.getEstado() == EstadoProceso.READY || head.getEstado() == EstadoProceso.RUNNING) {
                return head; // RUNNING solo para soportar continuidad si el simulador lo reusa
            }
            // Si quedo BLOCKED/TERMINATED en la cola por error, lo quitamos
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

    // Visibilidad para test
    int size() {
        return cola.size();
    }
}
