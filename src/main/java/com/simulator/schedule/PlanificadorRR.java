package com.simulator.schedule;

import com.simulator.core.EstadoProceso;
import com.simulator.core.Proceso;

import java.util.*;

public final class PlanificadorRR implements Planificador {

    private final int quantumTicks;
    private final Deque<Proceso> cola = new ArrayDeque<>();
    private final Map<Integer, Integer> qRestante = new HashMap<>();

    public PlanificadorRR(int quatumTicks) {
        if (quatumTicks <= 0) throw new IllegalArgumentException("quantumTicks debe ser > 0");
        this.quantumTicks = quatumTicks;
    }
    
    @Override
    public void agregarProceso(Proceso p){
        Objects.requireNonNull(p);
        if (!cola.contains(p)) cola.offerLast(p);
        // Siempre (re)inicializamos el quantum al (re)insertar.
        qRestante.put(p.getPid(), quantumTicks);
    }
    
    @Override
    public Proceso seleccionarProceso() {
        // Devuelve el primero que este READY o RUNNING; Purga invalidos
        while (!cola.isEmpty()){
            Proceso h = cola.peekFirst();
            if (h.getEstado() == EstadoProceso.READY || h.getEstado() == EstadoProceso.RUNNING){
                return h;
            }
            cola.pollFirst();
            qRestante.remove(h.getPid());
        }
        return null;
    }
    
    @Override
    public void removerProceso(Proceso p){
        if (p == null) return;
        cola.remove(p);
        qRestante.remove(p.getPid());
    }
    
    @Override
    public void reinicializar() {
        cola.clear();
        qRestante.clear();
    }
    
    @Override
    public void onTick(Proceso runnig){
        if (runnig == null) return;
        Integer q = qRestante.get(runnig.getPid());
        if (q != null && q > 0) qRestante.put(runnig.getPid(), q -1);   
    }
    
    @Override
    public boolean debePreemptar(Proceso running) {
        if (running == null) return false;
        Integer q = qRestante.get(running.getPid());
        return q != null && q <= 0 && running.getEstado() != EstadoProceso.TERMINATED;
    }
    
    // Rotación: mueve el running al final y reinicia su quantum
    public void rotar(Proceso running) {
        if (running == null) return;
        // quitar cabeza si coincide y reinsertar al final
        if (!cola.isEmpty() && cola.peekFirst() == running) {
            cola.pollFirst();
        } else {
            cola.remove(running);
        }
        cola.offerLast(running);
        qRestante.put(running.getPid(), quantumTicks);
    }
    
    // Visibilidad package para tests
    int size() { return cola.size(); }
    int quantumRestante(int pid) { return qRestante.getOrDefault(pid, -1); }
}
