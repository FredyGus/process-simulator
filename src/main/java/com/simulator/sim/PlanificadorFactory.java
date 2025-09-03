package com.simulator.sim;

import com.simulator.schedule.Planificador;
import com.simulator.schedule.PlanificadorFCFS;

public final class PlanificadorFactory{
    public static Planificador crear(TipoAlgoritmo tipo){
        return switch (tipo) {
            case FCFS -> new PlanificadorFCFS();
            default -> throw new UnsupportedOperationException("Algoritmo no implementado: " + tipo);
        };
    }
}