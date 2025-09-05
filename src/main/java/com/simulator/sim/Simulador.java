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

    public enum ModoGeneracion {
        AUTOGENERADO, COORDINADO
    }

    private final ParametrosSimulacion params;
    private final Planificador planificador;
    private final LoggerSistema logger;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Random rng;
    private final List<Proceso> procesos = new ArrayList<>();

    private volatile boolean corriendo = false;
    private int tick = 0;
    private int nextPid = 1;
    private final ModoGeneracion modo;

    public interface Oyente {

        void onModeloActualizado(VistaModelo vm);
    }
    private Oyente oyente;
    private VistaModelo ultimoSnapshot;

    // Constructor sigle-run (AUTOGENERADO)
    public Simulador(ParametrosSimulacion params, Path logPath) {
        this(params, logPath, ModoGeneracion.AUTOGENERADO);
    }

    public Simulador(ParametrosSimulacion params, Path logPath, ModoGeneracion modo) {
        this.params = params;
        this.planificador = PlanificadorFactory.crear(params.algoritmo);
        this.logger = new LoggerSistema(); // instancia independiente
        this.rng = new Random(params.seed);
        this.modo = modo;

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
        if (modo != ModoGeneracion.AUTOGENERADO) {
            throw new IllegalStateException("Solo aplica iniciar() en modo AUTOGENERADO");
        }

        if (corriendo) {
            return;
        }
        corriendo = true;
        scheduler.scheduleAtFixedRate(this::runTickSafe, 0, params.tickMs, TimeUnit.MILLISECONDS);
    }

    public void pausar() {
        corriendo = false;
    }

    public void detener() {
        corriendo = false;
        scheduler.shutdownNow();
        logger.registrar(LogEvento.FIN_SIMULACION, LogNivel.INFO, LogDatos.vacio());
        logger.finalizar();
    }

    private void runTickSafe() {
        if (!corriendo) {
            return;
        }
        try {
            runTickAuto();
        } catch (Throwable t) {
            logger.registrar(LogEvento.ERROR, LogNivel.ERROR, new LogDatos(null, null, null, null,
                    params.algoritmo.name(), params.quantum, "ex=" + t.getMessage()));
        }
    }

    // AUTOGENERADO
    private void runTickAuto() {
        tick++;
        // Llegadas aleatorias
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
        // Núcleo del tick
        tickCore();
    }

    public void tickCoordinado(List<ProcesoSpec> llegadas) {
        tick++;
        if (llegadas != null) {
            for (ProcesoSpec spec : llegadas) {
                Proceso p = crearProcesoDesdeSpec(spec);
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
        }
        tickCore();
    }

    // Parte común: selección/ejecución/snapshot
    private void tickCore() {
        Proceso seleccionado = planificador.seleccionarProceso();
        if (seleccionado != null) {
            if (seleccionado.getEstado() == EstadoProceso.READY) {
                seleccionado.cambiarEstado(EstadoProceso.RUNNING);
                logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                        new LogDatos(seleccionado.getPid(), "RUNNING", seleccionado.getCpuUsage(), seleccionado.getMemoria(),
                                params.algoritmo.name(), params.quantum, "READY→RUNNING"));
            }
            seleccionado.avanzarTick(tick);
            logger.registrar(LogEvento.EJECUTAR_TICK, LogNivel.INFO,
                    new LogDatos(seleccionado.getPid(), "RUNNING",
                            seleccionado.getCpuUsage(), seleccionado.getMemoria(),
                            params.algoritmo.name(), params.quantum,
                            "rafagaRestante=" + seleccionado.getTiempoRestante()));
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

        // Snapshot
        ultimoSnapshot = construirSnapshot();
        if (oyente != null) {
            oyente.onModeloActualizado(ultimoSnapshot);
        }
    }
    
    private Proceso crearProcesoAleatorio() {
        int rafaga = randBetween(params.rafagaMin, params.rafagaMax);
        int prio = randBetween(params.prioridadMin, params.prioridadMax);
        Proceso p = new Proceso(nextPid++, "P" + (nextPid - 1), tick, rafaga, prio, rng);
        return p;
    }

    // NUEVO: crear desde ProcesoSpec (semilla propia por proceso para que ambos sims sean idénticos)
    private Proceso crearProcesoDesdeSpec(ProcesoSpec s) {
        return new Proceso(s.pid(), s.nombre(), tick, s.rafaga(), s.prioridad(), new Random(s.seed()));
    }
    
    private int randBetween(int a, int b) {
        if (a > b) { int t = a; a = b; b = t; }
        return a + rng.nextInt(b - a + 1);
    }
    
    private VistaModelo construirSnapshot() {
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

    // NUEVO: acceso al último snapshot (útil para el comparador/UI)
    public VistaModelo getUltimoSnapshot() { return ultimoSnapshot; }

}
