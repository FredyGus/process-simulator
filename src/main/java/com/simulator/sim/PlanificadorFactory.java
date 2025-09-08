package com.simulator.sim;

import com.simulator.schedule.*;

public final class PlanificadorFactory{
    public static Planificador crear(TipoAlgoritmo tipo, Integer quantum){
        return switch (tipo){
            case FCFS -> new PlanificadorFCFS();
            case RR -> new PlanificadorRR(quantum != null ? quantum : 3);
            case SJF -> new PlanificadorSJF();
                // PRIORIDAD PENDIENTE
            default -> throw new UnsupportedOperationException("Algotitmo no implementado: " + tipo);
        };
    }
    
    // Sobrecarga existente, si la tienes
    public static Planificador crear(TipoAlgoritmo alg) {
        return crear(alg, null);
    }
}