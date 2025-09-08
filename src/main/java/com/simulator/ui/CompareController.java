package com.simulator.ui;

import com.simulator.sim.LogNombres;
import com.simulator.sim.ParametrosSimulacion;
import com.simulator.sim.Simulador;
import com.simulator.sim.Simulador.ModoGeneracion;
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
    }

    @FXML
    private void onStart() {
        if (running) {
            return;
        }
        running = true;
        tick = 0;
        nextPid = 1;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::tickCoordinado, 0, base.tickMs, TimeUnit.MILLISECONDS);
    }

    @FXML
    private void onStop() {
        if (!running) {
            return;
        }
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        // Cerramos logs de ambos
        simA.detener();
        simB.detener();
    }

    private void tickCoordinado() {
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
        datosA.setAll(filas.stream()
                .map(f -> new ProcesoVM(f.pid(), f.nombre(), f.estado(),
                f.cpu(), f.memoria(), f.prioridad(), f.rafagaRestante()))
                .toList());
    }

    private void actualizarTablaB(int tk, List<FilaProcesoVM> filas) {
        lblTickB.setText("Tick B: " + tk);
        lblActivosB.setText("Activos B: " + filas.size());
        datosB.setAll(filas.stream()
                .map(f -> new ProcesoVM(f.pid(), f.nombre(), f.estado(),
                f.cpu(), f.memoria(), f.prioridad(), f.rafagaRestante()))
                .toList());
    }
}
