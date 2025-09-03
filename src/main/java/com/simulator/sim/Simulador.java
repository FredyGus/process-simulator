package com.simulator.sim;

import com.simulator.core.EstadoProceso;
import com.simulator.core.Proceso;
import com.simulator.logging.*;
import com.simulator.logging.format.TablaFijaFormatter;
import com.simulator.logging.rotate.RotacionPorTamano;
import com.simulator.schedule.Planificador;
import com.simulator.sim.vm.FilaProcesoVM;
import com.simulator.sim.vm.VistaModelo;
import com.simulator.time.RelojDelSistema;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public final class Simulador {
    private final ParametrosSimulacion params;
    private final Planificador planificador;
    private final LoggerSistema logger;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Random rng;
    private final List<Proceso> procesos = new ArrayList<>();

    private volatile boolean corriendo = false;
    private int tick = 0;
    private int nextPid = 1;

    public interface Oyente {
        void onModeloActualizado(VistaModelo vm);
    }
    private Oyente oyente;

    public Simulador(ParametrosSimulacion params, Path logPath) {
        this.params = params;
        this.planificador = PlanificadorFactory.crear(params.algoritmo);
        this.logger = new LoggerSistema(); // instancia independiente
        this.rng = new Random(params.seed);

        // inicia logger
        var config = LogConfig.basica(logPath, new RotacionPorTamano(5 * 1024 * 1024, 3));
        logger.iniciar(config, new FileLogWriter(), new TablaFijaFormatter(), new RelojDelSistema());

        logger.registrar(LogEvento.INICIO_SIMULACION, LogNivel.INFO,
                new LogDatos(null, "READY", null, null, params.algoritmo.name(), params.quantum,
                        "tickMs=" + params.tickMs + ", probNuevo=" + params.probNuevoProceso));
    }

    public void setOyente(Oyente oyente) {
        this.oyente = oyente;
    }

    public void iniciar() {
        if (corriendo) return;
        corriendo = true;
        scheduler.scheduleAtFixedRate(this::runTickSafe, 0, params.tickMs, TimeUnit.MILLISECONDS);
    }

    public void pausar() { corriendo = false; }

    public void detener() {
        corriendo = false;
        scheduler.shutdownNow();
        logger.registrar(LogEvento.FIN_SIMULACION, LogNivel.INFO, LogDatos.vacio());
        logger.finalizar();
    }

    private void runTickSafe() {
        if (!corriendo) return;
        try { runTick(); } catch (Throwable t) {
            logger.registrar(LogEvento.ERROR, LogNivel.ERROR, new LogDatos(null, null, null, null,
                    params.algoritmo.name(), params.quantum, "ex=" + t.getMessage()));
        }
    }

    private void runTick() {
        tick++;

        // 1) Llegadas aleatorias
        if (rng.nextDouble() < params.probNuevoProceso) {
            Proceso p = crearProcesoAleatorio();
            procesos.add(p);
            p.cambiarEstado(EstadoProceso.READY);
            planificador.agregarProceso(p);
            logger.registrar(LogEvento.CREAR_PROCESO, LogNivel.INFO,
                    new LogDatos(p.getPid(), "READY", 0, p.getMemoria(),
                            params.algoritmo.name(), params.quantum,
                            "rafaga=" + p.getTiempoRestante() + ", prioridad=" + p.getPrioridad()));
            logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                    new LogDatos(p.getPid(), "READY", 0, p.getMemoria(), params.algoritmo.name(), params.quantum, "NEW→READY"));
        }

        // 2) Selección
        Proceso seleccionado = planificador.seleccionarProceso();

        // 3) Ejecutar/avanzar estados
        if (seleccionado != null) {
            if (seleccionado.getEstado() == EstadoProceso.READY) {
                seleccionado.cambiarEstado(EstadoProceso.RUNNING);
                logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                        new LogDatos(seleccionado.getPid(), "RUNNING", seleccionado.getCpuUsage(), seleccionado.getMemoria(),
                                params.algoritmo.name(), params.quantum, "READY→RUNNING"));
            }

            // Ejecuta 1 tick
            seleccionado.avanzarTick(tick);
            logger.registrar(LogEvento.EJECUTAR_TICK, LogNivel.INFO,
                    new LogDatos(seleccionado.getPid(), "RUNNING",
                            seleccionado.getCpuUsage(), seleccionado.getMemoria(),
                            params.algoritmo.name(), params.quantum,
                            "rafagaRestante=" + seleccionado.getTiempoRestante()));

            // Si terminó, remuévelo
            if (seleccionado.getEstado() == EstadoProceso.TERMINATED) {
                logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                        new LogDatos(seleccionado.getPid(), "TERMINATED", 0, 0,
                                params.algoritmo.name(), params.quantum, "RUNNING→TERMINATED"));
                logger.registrar(LogEvento.TERMINAR_PROCESO, LogNivel.INFO,
                        new LogDatos(seleccionado.getPid(), "TERMINATED", 0, 0,
                                params.algoritmo.name(), params.quantum, "fin_natural"));
                planificador.removerProceso(seleccionado);
            }
        } else {
            logger.registrar(LogEvento.IDLE, LogNivel.INFO,
                    new LogDatos(null, "IDLE", null, null, params.algoritmo.name(), params.quantum, "sin procesos listos"));
        }

        // 4) Construir snapshot (para UI futura)
        if (oyente != null) {
            oyente.onModeloActualizado(snapshotActual());
        }
    }

    private Proceso crearProcesoAleatorio() {
        int rafaga = randBetween(params.rafagaMin, params.rafagaMax);
        int prio = randBetween(params.prioridadMin, params.prioridadMax);
        Proceso p = new Proceso(nextPid++, "P" + (nextPid - 1), tick, rafaga, prio, rng);
        return p;
    }

    private int randBetween(int a, int b) {
        if (a > b) { int t = a; a = b; b = t; }
        return a + rng.nextInt(b - a + 1);
    }

    private VistaModelo snapshotActual() {
        List<FilaProcesoVM> filas = new ArrayList<>();
        for (Proceso p : procesos) {
            if (p.getEstado() != EstadoProceso.TERMINATED) {
                filas.add(new FilaProcesoVM(
                        p.getPid(), p.getNombre(), p.getEstado().name(),
                        p.getCpuUsage(), p.getMemoria(), p.getPrioridad(), p.getTiempoRestante()
                ));
            }
        }
        return new VistaModelo(tick, filas.size(), filas);
    }
}
