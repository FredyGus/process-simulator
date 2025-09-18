package com.simulator.metrics;

import com.simulator.core.Proceso;

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
        Integer tiempoRespuesta,
        Integer turnaround) {

    public static ProcesoMetricas from(Proceso p, String algoritmo) {
        int llegada = p.getTickLlegada();
        int primera = p.getTickPrimeraEjecucion();
        int fin = p.getTickFinalizacion();
        Integer resp = (primera >= 0) ? (primera - llegada) : null;
        Integer ta = (fin >= 0) ? (fin - llegada) : null;

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
