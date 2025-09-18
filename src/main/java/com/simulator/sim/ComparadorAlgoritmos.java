package com.simulator.sim;

import com.simulator.sim.Simulador.ModoGeneracion;
import com.simulator.sim.vm.VistaModelo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public final class ComparadorAlgoritmos {

    public interface Oyente {

        void onModeloActualizadoA(VistaModelo vmA);

        void onModeloActualizadoB(VistaModelo vmB);
    }

    private final ParametrosSimulacion base;
    private final TipoAlgoritmo algA, algB;
    private final String runId;
    private final ScheduledExecutorService scheduler
            = Executors.newSingleThreadScheduledExecutor();
    private final Random rng;
    private final int tickMs;

    private final Simulador simA;
    private final Simulador simB;
    private final Oyente oyente;

    private volatile boolean corriendo = false;
    private int nextPid = 1;

    public ComparadorAlgoritmos(ParametrosSimulacion baseParams, TipoAlgoritmo algA, TipoAlgoritmo algB, Oyente oyente) {
        this.base = baseParams;
        this.algA = algA;
        this.algB = algB;
        this.tickMs = baseParams.tickMs;
        this.rng = new Random(baseParams.seed);
        this.oyente = oyente;

        this.runId = LogNombres.newRunId();
        Path dir = LogNombres.compareDir(runId);
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {

        }

        var pA = new ParametrosSimulacion(base.tickMs, base.probNuevoProceso, base.rafagaMin,
                base.rafagaMax, base.prioridadMin, base.prioridadMax, base.seed, algA, base.quantum);

        var pB = new ParametrosSimulacion(base.tickMs, base.probNuevoProceso, base.rafagaMin,
                base.rafagaMax, base.prioridadMin, base.prioridadMax, base.seed, algB, base.quantum);

        this.simA = new Simulador(pA, LogNombres.comparePath(runId, algA), ModoGeneracion.COORDINADO);
        this.simB = new Simulador(pB, LogNombres.comparePath(runId, algB), ModoGeneracion.COORDINADO);
    }

    public void iniciar() {
        if (corriendo) {
            return;
        }
        corriendo = true;
        scheduler.scheduleAtFixedRate(this::tickSafe, 0, tickMs, TimeUnit.MILLISECONDS);
    }

    public void detener() {
        corriendo = false;
        scheduler.shutdownNow();
        simA.detener();
        simB.detener();
    }

    private void tickSafe() {
        if (!corriendo) {
            return;
        }
        try {
            tick();
        } catch (Throwable t) {

        }
    }

    private void tick() {
        List<ProcesoSpec> llegadas = new ArrayList<>();
        if (rng.nextDouble() < base.probNuevoProceso) {
            int pid = nextPid++;
            int rafaga = randBetween(base.rafagaMin, base.rafagaMax);
            int prioridad = randBetween(base.prioridadMin, base.prioridadMax);
            long seedProc = (base.seed * 31L) ^ pid;
            llegadas.add(new ProcesoSpec(pid, "P" + pid, rafaga, prioridad, seedProc));
        }

        simA.tickCoordinado(llegadas);
        simB.tickCoordinado(llegadas);

        if (oyente != null) {
            oyente.onModeloActualizadoA(simA.getUltimoSnapshot());
            oyente.onModeloActualizadoB(simB.getUltimoSnapshot());
        }
    }

    private int randBetween(int a, int b) {
        if (a > b) {
            int t = a;
            a = b;
            b = t;
        }
        return a + rng.nextInt(b - a + 1);
    }

}
