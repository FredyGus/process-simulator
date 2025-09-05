package com.simulator.sim;

import com.simulator.schedule.*;

public final class PlanificadorFactory{
    public static Planificador crear(TipoAlgoritmo tipo, Integer quantum){
        return switch (tipo){
            case FCFS -> new PlanificadorFCFS();
            case RR -> new PlanificadorRR(quantum != null ? quantum : 3);
                // SJF Y PRIORIDAD PENDIENTES
            default -> throw new UnsupportedOperationException("Algotitmo no implementado: " + tipo);
        };
    }
}