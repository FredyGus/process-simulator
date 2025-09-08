package com.simulator.schedule;

import com.simulator.core.EstadoProceso;
import com.simulator.core.Proceso;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlanificadorSJF implements Planificador {
    
    private final List<Proceso> ready = new ArrayList<>();
    
    private static final Comparator<Proceso> COMP =
            Comparator.comparingInt(Proceso::getTiempoRestante)
            .thenComparingInt(Proceso::getPid);
    
    @Override
    public void agregarProceso(Proceso p){
        // Solo gestionamos los que estan listos (READY/RUNNING se manera en simulador)
        if (p.getEstado() != EstadoProceso.TERMINATED){
            ready.add(p);
            ready.sort(COMP);
        }
    }
    
    @Override
    public Proceso seleccionarProceso(){
        // SJF no expropiativo: Si el que esta en RUNNING sigue vivo, se mantiene
        // Aqui solo devolvemos el "mejor" de la cola READY cuando el CPU esta libre
        // El simulador ya llama a cambiar a RUNNING la primera vez
        // (Si ya hay uno en RUNNING, el simulador lo volvera a elegir por ser el mas activo.)
        
        if (ready.isEmpty()) return null;
        // Devuelve el primero no terminado
        for(Proceso p : ready){
            if (p.getEstado() != EstadoProceso.TERMINATED){
                return p;
            }
        }
        
        return null;
    }
    
    @Override
    public void removerProceso(Proceso p){
        ready.remove(p);
    }
    
    @Override
    public void reinicializar() {
        ready.clear();
    }
}