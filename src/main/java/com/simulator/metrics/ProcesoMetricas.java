package com.simulator.metrics;

import com.simulator.core.Proceso;

/**
 * Métricas por proceso terminado.
 *
 * respuesta = primeraEjecucion - llegada turnaround = finalizacion - llegada
 * espera = turnaround - rafagaTotal
 */
public record ProcesoMetricas(
        int pid,
        String nombre,
        String algoritmo,
        int llegada,
        int primeraEjecucion,
        int finalizacion,
        int rafagaTotal,
        int respuesta,
        int espera,
        int turnaround) {

    public static ProcesoMetricas from(Proceso p, String algoritmo) {
        int llegada = p.getTickLlegada();
        int primera = p.getTickPrimeraEjecucion();
        int fin = p.getTickFinalizacion();
        int rafaga = p.getRafagaTotal();

        int respuesta = (primera >= 0 && llegada >= 0) ? (primera - llegada) : -1;
        int turnaround = (fin >= 0 && llegada >= 0) ? (fin - llegada) : -1;
        int espera = (turnaround >= 0 && rafaga >= 0) ? (turnaround - rafaga) : -1;

        return new ProcesoMetricas(
                p.getPid(),
                p.getNombre(),
                algoritmo,
                llegada,
                primera,
                fin,
                rafaga,
                respuesta,
                espera,
                turnaround
        );
    }
}
