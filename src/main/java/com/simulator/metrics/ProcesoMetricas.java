package com.simulator.metrics;

import com.simulator.core.Proceso;

public record ProcesoMetricas(
        int pid,
        int tickLlegada,
        int tickPrimeraEjec,
        int tickFin,
        int rafagaTotal,
        int turnaround,   // tickFin - llegada
        int espera,       // turnaround - rafagaTotal
        int respuesta     // primeraEjec - llegada
) {
    public static ProcesoMetricas from(Proceso p) {
        int llegada = p.getTickLlegada();
        int fin = p.getTickFinalizacion();
        int primera = p.getTickPrimeraEjecucion();
        int rafaga = p.getRafagaTotal();

        // En caso de que aún no haya algún valor, usamos 0 o -1 (según tu preferencia).
        int turnaround = (fin >= 0) ? (fin - llegada) : -1;
        int espera     = (turnaround >= 0) ? (turnaround - rafaga) : -1;
        int respuesta  = (primera >= 0) ? (primera - llegada) : -1;

        return new ProcesoMetricas(
                p.getPid(), llegada, primera, fin, rafaga,
                turnaround, espera, respuesta
        );
    }
}
