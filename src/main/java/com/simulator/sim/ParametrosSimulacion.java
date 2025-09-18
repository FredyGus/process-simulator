package com.simulator.sim;

public final class ParametrosSimulacion {

    public final int tickMs;
    public final double probNuevoProceso;
    public final int rafagaMin, rafagaMax;
    public final int prioridadMin, prioridadMax;
    public final long seed;
    public final TipoAlgoritmo algoritmo;
    public final Integer quantum;

    public ParametrosSimulacion(int tickMs, double probNuevoProceso, int rafagaMin, int rafagaMax, int prioridadMin,
            int prioridadMax, long seed, TipoAlgoritmo algoritmo, Integer quantum) {
        this.tickMs = tickMs;
        this.probNuevoProceso = probNuevoProceso;
        this.rafagaMin = rafagaMin;
        this.rafagaMax = rafagaMax;
        this.prioridadMin = prioridadMin;
        this.prioridadMax = prioridadMax;
        this.seed = seed;
        this.algoritmo = algoritmo;
        this.quantum = quantum;

    }

    public static ParametrosSimulacion defaultFCFS() {
        return new ParametrosSimulacion(
                500, 0.35, 5, 12, 1, 5, 12345L, TipoAlgoritmo.FCFS, null
        );
    }
}
