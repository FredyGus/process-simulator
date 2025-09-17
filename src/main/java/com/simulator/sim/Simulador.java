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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public final class Simulador {

    public enum ModoGeneracion { AUTOGENERADO, COORDINADO }

    private final ParametrosSimulacion params;
    private final Planificador planificador;
    private final LoggerSistema logger;

    // Scheduler único del simulador (single thread)
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Métricas de procesos terminados (fase 9b)
    private final List<com.simulator.metrics.ProcesoMetricas> metricasTerminadas =
            new CopyOnWriteArrayList<>();

    private final Random rng;
    private final List<Proceso> procesos = new ArrayList<>();

    private volatile boolean corriendo = false;
    private int tick = 0;
    private int nextPid = 1;
    private final ModoGeneracion modo;

    // Acciones encoladas desde la UI (terminar/suspender/reanudar)
    private final ConcurrentLinkedQueue<Runnable> acciones = new ConcurrentLinkedQueue<>();

    // Snapshot/oyente para UI
    public interface Oyente { void onModeloActualizado(VistaModelo vm); }
    private Oyente oyente;
    private VistaModelo ultimoSnapshot;

    // --- Constructores ---

    // Single-run (autogenerado)
    public Simulador(ParametrosSimulacion params, Path logPath) {
        this(params, logPath, ModoGeneracion.AUTOGENERADO);
    }

    public Simulador(ParametrosSimulacion params, Path logPath, ModoGeneracion modo) {
        this.params = params;
        this.planificador = PlanificadorFactory.crear(params.algoritmo, params.quantum);
        this.logger = new LoggerSistema(); // instancia independiente
        this.rng = new Random(params.seed);
        this.modo = modo;

        // Logger
        var config = LogConfig.basica(logPath, new RotacionPorTamano(5 * 1024 * 1024, 3));
        logger.iniciar(config, new FileLogWriter(), new TablaFijaFormatter(), new RelojDelSistema());
        logger.registrar(
                LogEvento.INICIO_SIMULACION, LogNivel.INFO,
                new LogDatos(null, "READY", null, null,
                        params.algoritmo.name(), params.quantum,
                        "tickMs=" + params.tickMs + ", probNuevo=" + params.probNuevoProceso)
        );
    }

    // --- Oyente/UI ---

    public void setOyente(Oyente oyente) { this.oyente = oyente; }

    public VistaModelo getUltimoSnapshot() { return ultimoSnapshot; }

    // --- API de acciones desde UI (se encolan y se aplican dentro del tick) ---

    public void terminarProceso(int pid)   { acciones.add(() -> doTerminar(pid)); }
    public void suspenderProceso(int pid)  { acciones.add(() -> doSuspender(pid)); }
    public void reanudarProceso(int pid)   { acciones.add(() -> doReanudar(pid)); }

    // --- Control de ejecución (single-run AUTOGENERADO) ---

    public void iniciar() {
        if (modo != ModoGeneracion.AUTOGENERADO) {
            throw new IllegalStateException("Solo aplica iniciar() en modo AUTOGENERADO");
        }
        if (corriendo) return;
        corriendo = true;
        scheduler.scheduleAtFixedRate(this::runTickSafe, 0, params.tickMs, TimeUnit.MILLISECONDS);
    }

    public void pausar()     { corriendo = false; }
    public void continuar()  { corriendo = true;  }

    public void detener() {
        corriendo = false;
        scheduler.shutdownNow();
        logger.registrar(LogEvento.FIN_SIMULACION, LogNivel.INFO, LogDatos.vacio());
        logger.finalizar();
    }

    public boolean isCorriendo() { return corriendo; }
    public boolean isPausado()   { return !corriendo && !scheduler.isShutdown(); }

    // --- Métricas (getters) ---

    /** Alias antiguo (por compatibilidad) */
    public List<com.simulator.metrics.ProcesoMetricas> getMetricasTerminadas() {
        return List.copyOf(metricasTerminadas);
    }

    /** El que usa el controller: snapshot inmutable de métricas */
    public List<com.simulator.metrics.ProcesoMetricas> getMetricasTerminadasSnapshot() {
        return List.copyOf(metricasTerminadas);
    }

    // --- Bucle de ticks ---

    private void runTickSafe() {
        if (!corriendo) return;
        try {
            runTickAuto();
        } catch (Throwable t) {
            logger.registrar(LogEvento.ERROR, LogNivel.ERROR,
                    new LogDatos(null, null, null, null,
                            params.algoritmo.name(), params.quantum, "ex=" + t.getMessage()));
        }
    }

    // Modo AUTOGENERADO
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
                    new LogDatos(p.getPid(), "READY", 0, p.getMemoria(),
                            params.algoritmo.name(), params.quantum, "NEW→READY"));
        }

        tickCore();
    }

    // Modo COORDINADO (comparador)
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
                        new LogDatos(p.getPid(), "READY", 0, p.getMemoria(),
                                params.algoritmo.name(), params.quantum, "NEW→READY"));
            }
        }
        tickCore();
    }

    // Parte común de un tick
    private void tickCore() {
        procesarAccionesPendientes();

        Proceso seleccionado = planificador.seleccionarProceso();
        if (seleccionado != null) {
            // READY → RUNNING
            if (seleccionado.getEstado() == EstadoProceso.READY) {
                seleccionado.cambiarEstado(EstadoProceso.RUNNING);
                logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                        new LogDatos(seleccionado.getPid(), "RUNNING",
                                seleccionado.getCpuUsage(), seleccionado.getMemoria(),
                                params.algoritmo.name(), params.quantum, "READY→RUNNING"));
            }

            // Ejecuta 1 tick
            seleccionado.avanzarTick(tick);

            // Hook por tick (RR usa esto para el quantum)
            planificador.onTick(seleccionado);

            // Log del tick
            logger.registrar(LogEvento.EJECUTAR_TICK, LogNivel.INFO,
                    new LogDatos(seleccionado.getPid(), "RUNNING",
                            seleccionado.getCpuUsage(), seleccionado.getMemoria(),
                            params.algoritmo.name(), params.quantum,
                            "rafagaRestante=" + seleccionado.getTiempoRestante()));

            // ¿Terminó?
            if (seleccionado.getEstado() == EstadoProceso.TERMINATED) {
                logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                        new LogDatos(seleccionado.getPid(), "TERMINATED", 0, 0,
                                params.algoritmo.name(), params.quantum, "RUNNING→TERMINATED"));
                logger.registrar(LogEvento.TERMINAR_PROCESO, LogNivel.INFO,
                        new LogDatos(seleccionado.getPid(), "TERMINATED", 0, 0,
                                params.algoritmo.name(), params.quantum, "fin_natural"));

                // Guardar métricas del proceso finalizado (fase 9b)
                metricasTerminadas.add(
                        com.simulator.metrics.ProcesoMetricas.from(seleccionado, params.algoritmo.name())
                );

                planificador.removerProceso(seleccionado);

            } else if (planificador.debePreemptar(seleccionado)) {
                // Preempción (RR: quantum agotado)
                seleccionado.cambiarEstado(EstadoProceso.READY);
                logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                        new LogDatos(seleccionado.getPid(), "READY", 0, seleccionado.getMemoria(),
                                params.algoritmo.name(), params.quantum, "preempt: quantum agotado"));

                if (planificador instanceof com.simulator.schedule.PlanificadorRR rr) {
                    rr.rotar(seleccionado);
                } else {
                    // Fallback genérico
                    planificador.removerProceso(seleccionado);
                    planificador.agregarProceso(seleccionado);
                }
            }

        } else {
            // CPU idle
            logger.registrar(LogEvento.IDLE, LogNivel.INFO,
                    new LogDatos(null, "IDLE", null, null,
                            params.algoritmo.name(), params.quantum, "sin procesos listos"));
        }

        // Publicar snapshot para la UI
        ultimoSnapshot = construirSnapshot();
        if (oyente != null) oyente.onModeloActualizado(ultimoSnapshot);
    }

    // Acciones encoladas desde la UI
    private void procesarAccionesPendientes() {
        Runnable r;
        while ((r = acciones.poll()) != null) {
            try {
                r.run();
            } catch (Throwable t) {
                logger.registrar(LogEvento.ERROR, LogNivel.ERROR,
                        new LogDatos(null, null, null, null,
                                params.algoritmo.name(), params.quantum,
                                "accion_fallida=" + t.getMessage()));
            }
        }
    }

    // ---- Acciones concretas ----

    private void doTerminar(int pid) {
        for (Proceso p : procesos) {
            if (p.getPid() == pid && p.getEstado() != EstadoProceso.TERMINATED) {
                // marcar terminado (con tick actual)
                p.forzarTerminar(tick);

                logger.registrar(LogEvento.TERMINAR_PROCESO, LogNivel.WARN,
                        new LogDatos(p.getPid(), "TERMINATED", 0, 0,
                                params.algoritmo.name(), params.quantum, "forzado_por_UI"));

                // capturar métricas también en el forzado
                metricasTerminadas.add(
                        com.simulator.metrics.ProcesoMetricas.from(p, params.algoritmo.name())
                );

                planificador.removerProceso(p);
                break;
            }
        }
    }

    // SUSPENDER: pasa a SUSPENDED y lo saca del planificador
    private void doSuspender(int pid) {
        for (Proceso p : procesos) {
            if (p.getPid() == pid
                    && p.getEstado() != EstadoProceso.TERMINATED
                    && p.getEstado() != EstadoProceso.SUSPENDED) {

                EstadoProceso prev = p.getEstado();
                p.cambiarEstado(EstadoProceso.SUSPENDED);
                planificador.removerProceso(p);

                logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                        new LogDatos(p.getPid(), "SUSPENDED", p.getCpuUsage(), p.getMemoria(),
                                params.algoritmo.name(), params.quantum, prev + "→SUSPENDED"));
                logger.registrar(LogEvento.SUSPENDER, LogNivel.INFO,
                        new LogDatos(p.getPid(), "SUSPENDED", p.getCpuUsage(), p.getMemoria(),
                                params.algoritmo.name(), params.quantum, "usuario"));
                break;
            }
        }
    }

    // REANUDAR: SUSPENDED → READY y vuelve al planificador
    private void doReanudar(int pid) {
        for (Proceso p : procesos) {
            if (p.getPid() == pid && p.getEstado() == EstadoProceso.SUSPENDED) {
                p.cambiarEstado(EstadoProceso.READY);
                planificador.agregarProceso(p);

                logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                        new LogDatos(p.getPid(), "READY", 0, p.getMemoria(),
                                params.algoritmo.name(), params.quantum, "SUSPENDED→READY"));
                logger.registrar(LogEvento.REANUDAR, LogNivel.INFO,
                        new LogDatos(p.getPid(), "READY", 0, p.getMemoria(),
                                params.algoritmo.name(), params.quantum, "usuario"));
                break;
            }
        }
    }

    // ---- Helpers de creación ----

    private Proceso crearProcesoAleatorio() {
        int rafaga = randBetween(params.rafagaMin, params.rafagaMax);
        int prio   = randBetween(params.prioridadMin, params.prioridadMax);
        return new Proceso(nextPid++, "P" + (nextPid - 1), tick, rafaga, prio, rng);
    }

    private Proceso crearProcesoDesdeSpec(ProcesoSpec s) {
        return new Proceso(s.pid(), s.nombre(), tick, s.rafaga(), s.prioridad(), new Random(s.seed()));
    }

    private int randBetween(int a, int b) {
        if (a > b) { int t = a; a = b; b = t; }
        return a + rng.nextInt(b - a + 1);
    }

    // Construye snapshot para la UI
    private VistaModelo construirSnapshot() {
        List<FilaProcesoVM> filas = new ArrayList<>();
        for (Proceso p : procesos) {
            if (p.getEstado() != EstadoProceso.TERMINATED) {
                filas.add(new FilaProcesoVM(
                        p.getPid(),
                        p.getNombre(),
                        p.getEstado().name(),
                        p.getCpuUsage(),
                        p.getMemoria(),
                        p.getPrioridad(),
                        p.getTiempoRestante()
                ));
            }
        }
        return new VistaModelo(tick, filas.size(), filas);
    }
}
