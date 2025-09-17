package com.simulator.metrics;

import com.simulator.core.Proceso;

/** Métricas calculadas para un proceso TERMINATED. */
public record ProcesoMetricas(
        int pid,
        String algoritmo,
        int tickLlegada,
        int tickPrimeraEjec,
        int tickFin,
        int rafagaTotal,
        int tiempoEjecucion,
        int tiempoEspera,
        // derivadas
        Integer tiempoRespuesta,   // primeraEjec - llegada  (si corrió)
        Integer turnaround         // fin - llegada          (si terminó)
) {
    /** Construye métricas a partir del Proceso terminado. */
    public static ProcesoMetricas from(Proceso p, String algoritmo) {
        int llegada = p.getTickLlegada();
        int primera = p.getTickPrimeraEjecucion();     // -1 si nunca ejecutó
        int fin = p.getTickFinalizacion();             // -1 si no tenemos el tick
        Integer resp = (primera >= 0) ? (primera - llegada) : null;
        Integer ta   = (fin >= 0)     ? (fin - llegada)     : null;

        return new ProcesoMetricas(
                p.getPid(),
                algoritmo,
                llegada,
                primera,
                fin,
                p.getRafagaTotal(),
                p.getTiempoEjecucion(),
                p.getTiempoEspera(),
                resp,
                ta
        );
    }
}
