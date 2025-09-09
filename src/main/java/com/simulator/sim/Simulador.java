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

    private final java.util.concurrent.ConcurrentLinkedQueue<Runnable> acciones = new java.util.concurrent.ConcurrentLinkedQueue<>();

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
        this.planificador = PlanificadorFactory.crear(params.algoritmo, params.quantum);
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

    public void terminarProceso(int pid) {
        acciones.add(() -> doTerminar(pid));
    }

    public void suspenderProceso(int pid) {
        acciones.add(() -> doSuspender(pid));
    }

    public void reanudarProceso(int pid) {
        acciones.add(() -> doReanudar(pid));
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
        procesarAccionesPendientes();
        Proceso seleccionado = planificador.seleccionarProceso();

        if (seleccionado != null) {
            // Si estaba READY, entra a RUNNING
            if (seleccionado.getEstado() == EstadoProceso.READY) {
                seleccionado.cambiarEstado(EstadoProceso.RUNNING);
                logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                        new LogDatos(seleccionado.getPid(), "RUNNING",
                                seleccionado.getCpuUsage(), seleccionado.getMemoria(),
                                params.algoritmo.name(), params.quantum,
                                "READY→RUNNING"));
            }

            // Ejecuta 1 tick de CPU
            seleccionado.avanzarTick(tick);

            // ← NUEVO: avisar al planificador que pasó 1 tick del RUNNING (RR usa esto para el quantum)
            planificador.onTick(seleccionado);

            // Log del tick ejecutado
            logger.registrar(LogEvento.EJECUTAR_TICK, LogNivel.INFO,
                    new LogDatos(seleccionado.getPid(), "RUNNING",
                            seleccionado.getCpuUsage(), seleccionado.getMemoria(),
                            params.algoritmo.name(), params.quantum,
                            "rafagaRestante=" + seleccionado.getTiempoRestante()));

            // ¿Terminó naturalmente?
            if (seleccionado.getEstado() == EstadoProceso.TERMINATED) {
                logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                        new LogDatos(seleccionado.getPid(), "TERMINATED", 0, 0,
                                params.algoritmo.name(), params.quantum,
                                "RUNNING→TERMINATED"));
                logger.registrar(LogEvento.TERMINAR_PROCESO, LogNivel.INFO,
                        new LogDatos(seleccionado.getPid(), "TERMINATED", 0, 0,
                                params.algoritmo.name(), params.quantum,
                                "fin_natural"));
                planificador.removerProceso(seleccionado);

                // ← NUEVO: si no terminó y el planificador dice que hay que preemptar (RR: quantum agotado)
            } else if (planificador.debePreemptar(seleccionado)) {
                // Preempción: vuelve a READY y se rota al final de la cola
                seleccionado.cambiarEstado(EstadoProceso.READY);
                logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                        new LogDatos(seleccionado.getPid(), "READY",
                                0, seleccionado.getMemoria(),
                                params.algoritmo.name(), params.quantum,
                                "preempt: quantum agotado"));

                // Rotación específica de RR (reinicia quantum)
                if (planificador instanceof com.simulator.schedule.PlanificadorRR rr) {
                    rr.rotar(seleccionado);
                } else {
                    // Fallback genérico (por si en el futuro hay otro alg. preemptivo)
                    planificador.removerProceso(seleccionado);
                    planificador.agregarProceso(seleccionado);
                }
            }

        } else {
            // No hay proceso listo
            logger.registrar(LogEvento.IDLE, LogNivel.INFO,
                    new LogDatos(null, "IDLE", null, null,
                            params.algoritmo.name(), params.quantum,
                            "sin procesos listos"));
        }

        // Publicar snapshot para UI/Comparador
        ultimoSnapshot = construirSnapshot();
        if (oyente != null) {
            oyente.onModeloActualizado(ultimoSnapshot);
        }
    }

    private void procesarAccionesPendientes() {
        Runnable r;
        while ((r = acciones.poll()) != null) {
            try {
                r.run();
            } catch (Throwable t) {
                logger.registrar(LogEvento.ERROR, LogNivel.ERROR, new LogDatos(null, null, null, null,
                        params.algoritmo.name(), params.quantum, "accion_fallida=" + t.getMessage()));
            }
        }
    }

    private void doTerminar(int pid) {
        for (Proceso p : procesos) {
            if (p.getPid() == pid && p.getEstado() != EstadoProceso.TERMINATED) {
                p.forzarTerminar();
                logger.registrar(LogEvento.TERMINAR_PROCESO, LogNivel.WARN,
                        new LogDatos(p.getPid(), "TERMINATED", 0, 0,
                                params.algoritmo.name(), params.quantum,
                                "forzado_por_UI"));
                planificador.removerProceso(p);
                break;
            }
        }
    }

    private void doSuspender(int pid) {
        for (Proceso p : procesos) {
            if (p.getPid() == pid && p.getEstado() == EstadoProceso.RUNNING) {
                p.cambiarEstado(EstadoProceso.READY);
                logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                        new LogDatos(p.getPid(), "READY", 0, p.getMemoria(),
                                params.algoritmo.name(), params.quantum,
                                "suspendido_por_UI"));
                planificador.agregarProceso(p);
                break;
            }
        }
    }

    private void doReanudar(int pid) {
        for (Proceso p : procesos) {
            if (p.getPid() == pid && p.getEstado() == EstadoProceso.READY) {
                p.cambiarEstado(EstadoProceso.RUNNING);
                logger.registrar(LogEvento.CAMBIO_ESTADO, LogNivel.INFO,
                        new LogDatos(p.getPid(), "RUNNING", p.getCpuUsage(), p.getMemoria(),
                                params.algoritmo.name(), params.quantum,
                                "reanudar_por_UI"));
                break;
            }
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
        if (a > b) {
            int t = a;
            a = b;
            b = t;
        }
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
    public VistaModelo getUltimoSnapshot() {
        return ultimoSnapshot;
    }

}
