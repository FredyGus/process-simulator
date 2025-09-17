package com.simulator.ui;

import com.simulator.sim.LogNombres;
import com.simulator.sim.ParametrosSimulacion;
import com.simulator.sim.Simulador;
import com.simulator.sim.TipoAlgoritmo;
import com.simulator.sim.ProcesoSpec;
import com.simulator.sim.vm.FilaProcesoVM;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.Optional;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;

public class CompareController {

    // UI A
    @FXML
    private TableView<ProcesoVM> tblA;
    @FXML
    private TableColumn<ProcesoVM, Number> colPidA, colCpuA, colMemA, colPrioA, colRafA;
    @FXML
    private TableColumn<ProcesoVM, String> colNomA, colEstadoA;
    @FXML
    private Label lblTickA, lblActivosA, lblAlgA;

    // UI B
    @FXML
    private TableView<ProcesoVM> tblB;
    @FXML
    private TableColumn<ProcesoVM, Number> colPidB, colCpuB, colMemB, colPrioB, colRafB;
    @FXML
    private TableColumn<ProcesoVM, String> colNomB, colEstadoB;
    @FXML
    private Label lblTickB, lblActivosB, lblAlgB;
    @FXML
    private MenuItem miTerminarA, miSuspenderA, miReanudarA;
    @FXML
    private MenuItem miTerminarB, miSuspenderB, miReanudarB;
    @FXML
    private ContextMenu ctxA, ctxB;
    private Integer pidMenuA, pidMenuB;

    private final ObservableList<ProcesoVM> datosA = FXCollections.observableArrayList();
    private final ObservableList<ProcesoVM> datosB = FXCollections.observableArrayList();

    // Simuladores
    private Simulador simA, simB;

    // Coordinación
    private ScheduledExecutorService scheduler;
    private boolean running = false;
    private ParametrosSimulacion base;
    private TipoAlgoritmo algA, algB;
    private Random rng;
    private int tick = 0;
    private int nextPid = 1;

    // Botones faltantes
    @FXML
    private Button btnStartAmbos;
    @FXML
    private Button btnStopAmbos;

    @FXML
    private Button btnPauseAmbos;

    private boolean paused = false;   // para el tick coordinado

    public void configurar(ParametrosSimulacion baseParams, TipoAlgoritmo a, TipoAlgoritmo b) {
        this.base = baseParams;
        this.algA = a;
        this.algB = b;

        lblAlgA.setText("A: " + a.name());
        lblAlgB.setText("B: " + b.name());

        // Copias con algoritmos distintos
        var paramsA = new ParametrosSimulacion(
                base.tickMs, base.probNuevoProceso,
                base.rafagaMin, base.rafagaMax,
                base.prioridadMin, base.prioridadMax,
                base.seed, a, a == TipoAlgoritmo.RR ? base.quantum : null
        );
        var paramsB = new ParametrosSimulacion(
                base.tickMs, base.probNuevoProceso,
                base.rafagaMin, base.rafagaMax,
                base.prioridadMin, base.prioridadMax,
                base.seed, b, b == TipoAlgoritmo.RR ? base.quantum : null
        );

        // Rutas de log (reusamos nombres existentes)
        String runId = LogNombres.newRunId();
        Path logA = LogNombres.comparePath(runId, a);
        Path logB = LogNombres.comparePath(runId, b);

        // Simuladores en modo COORDINADO (no llaman iniciar(); los "tiqueamos" nosotros)
        simA = new Simulador(paramsA, logA, Simulador.ModoGeneracion.COORDINADO);
        simB = new Simulador(paramsB, logB, Simulador.ModoGeneracion.COORDINADO);

        // Oyentes: refrescan cada tabla
        simA.setOyente(vm -> Platform.runLater(() -> actualizarTablaA(vm.getTick(), vm.getFilas())));
        simB.setOyente(vm -> Platform.runLater(() -> actualizarTablaB(vm.getTick(), vm.getFilas())));

        // RNG de la coordinación (una sola fuente → mismas llegadas para A y B)
        rng = new Random(base.seed);
    }

    @FXML
    private void initialize() {
        // A
        colPidA.setCellValueFactory(c -> c.getValue().pid);
        colNomA.setCellValueFactory(c -> c.getValue().nombre);
        colEstadoA.setCellValueFactory(c -> c.getValue().estado);
        colCpuA.setCellValueFactory(c -> c.getValue().cpu);
        colMemA.setCellValueFactory(c -> c.getValue().mem);
        colPrioA.setCellValueFactory(c -> c.getValue().prioridad);
        colRafA.setCellValueFactory(c -> c.getValue().rafaga);
        tblA.setItems(datosA);
        tblA.getSortOrder().setAll(colCpuA);
        colCpuA.setSortType(TableColumn.SortType.DESCENDING);

        // B
        colPidB.setCellValueFactory(c -> c.getValue().pid);
        colNomB.setCellValueFactory(c -> c.getValue().nombre);
        colEstadoB.setCellValueFactory(c -> c.getValue().estado);
        colCpuB.setCellValueFactory(c -> c.getValue().cpu);
        colMemB.setCellValueFactory(c -> c.getValue().mem);
        colPrioB.setCellValueFactory(c -> c.getValue().prioridad);
        colRafB.setCellValueFactory(c -> c.getValue().rafaga);
        tblB.setItems(datosB);
        tblB.getSortOrder().setAll(colCpuB);
        colCpuB.setSortType(TableColumn.SortType.DESCENDING);

        ctxA.setOnShowing(e -> {
            var vm = tblA.getSelectionModel().getSelectedItem();
            pidMenuA = (vm != null) ? vm.pid.get() : null;
        });
        ctxA.setOnHidden(e -> pidMenuA = null);

        tblA.setRowFactory(tv -> {
            TableRow<ProcesoVM> row = new TableRow<>();
            row.setOnContextMenuRequested(ev -> {
                if (!row.isEmpty()) {
                    tv.getSelectionModel().select(row.getIndex());
                }
            });
            return row;
        });

        miTerminarA.setOnAction(e -> {
            if (pidMenuA != null) {
                simA.terminarProceso(pidMenuA);
            }
        });
        miSuspenderA.setOnAction(e -> {
            if (pidMenuA != null) {
                simA.suspenderProceso(pidMenuA);
            }
        });
        miReanudarA.setOnAction(e -> {
            if (pidMenuA != null) {
                simA.reanudarProceso(pidMenuA);
            }
        });

        ctxB.setOnShowing(e -> {
            var vm = tblB.getSelectionModel().getSelectedItem();
            pidMenuB = (vm != null) ? vm.pid.get() : null;
        });
        ctxB.setOnHidden(e -> pidMenuB = null);

        tblB.setRowFactory(tv -> {
            TableRow<ProcesoVM> row = new TableRow<>();
            row.setOnContextMenuRequested(ev -> {
                if (!row.isEmpty()) {
                    tv.getSelectionModel().select(row.getIndex());
                }
            });
            return row;
        });

        miTerminarB.setOnAction(e -> {
            if (pidMenuB != null) {
                simB.terminarProceso(pidMenuB);
            }
        });
        miSuspenderB.setOnAction(e -> {
            if (pidMenuB != null) {
                simB.suspenderProceso(pidMenuB);
            }
        });
        miReanudarB.setOnAction(e -> {
            if (pidMenuB != null) {
                simB.reanudarProceso(pidMenuB);
            }
        });

        refreshButtonsAB();

    }

    @FXML
    private void onStartAmbos() {
        if (running) {
            return;
        }
        running = true;
        paused = false;
        tick = 0;
        nextPid = 1;

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::tickCoordinado, 0, base.tickMs, TimeUnit.MILLISECONDS);

        // Arrancan en "corriendo"
        simA.continuar();
        simB.continuar();

        refreshButtonsAB();
    }

    @FXML
    private void onStopAmbos() {
        if (!running) {
            return;
        }
        running = false;
        paused = false;

        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        simA.detener();
        simB.detener();

        refreshButtonsAB();
    }

    @FXML
    private void onPauseAmbos() {
        if (!running) {
            return;
        }

        if (!paused) {
            paused = true;
            // pausar sims internos (por consistencia)
            simA.pausar();
            simB.pausar();
        } else {
            paused = false;
            simA.continuar();
            simB.continuar();
        }
        refreshButtonsAB();
    }

    private void refreshButtonsAB() {
        btnStartAmbos.setDisable(running);
        btnPauseAmbos.setDisable(!running);
        btnStopAmbos.setDisable(!running);

        btnPauseAmbos.setText(paused ? "Reanudar ambos" : "Pausar ambos");
    }

    private void tickCoordinado() {
        if (paused) {
            return;
        }
        tick++;
        // Generar llegadas idénticas para A y B
        List<ProcesoSpec> llegadas = new ArrayList<>();
        if (rng.nextDouble() < base.probNuevoProceso) {
            int pid = nextPid++;
            int rafaga = randBetween(base.rafagaMin, base.rafagaMax);
            int prio = randBetween(base.prioridadMin, base.prioridadMax);
            long seedProc = rng.nextLong();
            llegadas.add(new ProcesoSpec(pid, "P" + pid, rafaga, prio, seedProc));
        }
        simA.tickCoordinado(llegadas);
        simB.tickCoordinado(llegadas);
    }

    private int randBetween(int a, int b) {
        if (a > b) {
            int t = a;
            a = b;
            b = t;
        }
        return a + rng.nextInt(b - a + 1);
    }

    private void actualizarTablaA(int tk, List<FilaProcesoVM> filas) {
        lblTickA.setText("Tick A: " + tk);
        lblActivosA.setText("Activos A: " + filas.size());

        // 1) guardar PID seleccionado actual (si hay)
        Integer seleccionado = Optional.ofNullable(tblA.getSelectionModel().getSelectedItem())
                .map(vm -> vm.pid.get()).orElse(null);

        // 2) refrescar datos
        datosA.setAll(filas.stream()
                .map(f -> new ProcesoVM(f.pid(), f.nombre(), f.estado(),
                f.cpu(), f.memoria(), f.prioridad(), f.rafagaRestante()))
                .toList());

        // 3) restaurar selección por PID
        if (seleccionado != null) {
            for (int i = 0; i < datosA.size(); i++) {
                if (datosA.get(i).pid.get() == seleccionado) {
                    tblA.getSelectionModel().select(i);
                    tblA.scrollTo(i); // opcional
                    break;
                }
            }
        }
    }

    private void actualizarTablaB(int tk, List<FilaProcesoVM> filas) {
        lblTickB.setText("Tick B: " + tk);
        lblActivosB.setText("Activos B: " + filas.size());

        Integer seleccionado = Optional.ofNullable(tblB.getSelectionModel().getSelectedItem())
                .map(vm -> vm.pid.get()).orElse(null);

        datosB.setAll(filas.stream()
                .map(f -> new ProcesoVM(f.pid(), f.nombre(), f.estado(),
                f.cpu(), f.memoria(), f.prioridad(), f.rafagaRestante()))
                .toList());

        if (seleccionado != null) {
            for (int i = 0; i < datosB.size(); i++) {
                if (datosB.get(i).pid.get() == seleccionado) {
                    tblB.getSelectionModel().select(i);
                    tblB.scrollTo(i);
                    break;
                }
            }
        }
    }

    private Optional<Integer> getSelectedPidA() {
        ProcesoVM vm = tblA.getSelectionModel().getSelectedItem();
        if (vm == null) {
            return Optional.empty();
        }
        return Optional.of(vm.pid.get());       // o vm.getPid()
    }

    private Optional<Integer> getSelectedPidB() {
        ProcesoVM vm = tblB.getSelectionModel().getSelectedItem();
        if (vm == null) {
            return Optional.empty();
        }
        return Optional.of(vm.pid.get());       // o vm.getPid()
    }
}
