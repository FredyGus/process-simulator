package com.simulator.ui;

import com.simulator.sim.LogNombres;
import com.simulator.sim.ParametrosSimulacion;
import com.simulator.sim.ProcesoSpec;
import com.simulator.sim.Simulador;
import com.simulator.sim.TipoAlgoritmo;
import com.simulator.sim.vm.FilaProcesoVM;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.binding.Bindings;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CompareController {

    @FXML
    private TableView<ProcesoVM> tblA;
    @FXML
    private TableColumn<ProcesoVM, Number> colPidA, colCpuA, colMemA, colPrioA, colRafA;
    @FXML
    private TableColumn<ProcesoVM, String> colNomA, colEstadoA;
    @FXML
    private Label lblTickA, lblActivosA, lblAlgA;

    @FXML
    private TableView<ProcesoVM> tblB;
    @FXML
    private TableColumn<ProcesoVM, Number> colPidB, colCpuB, colMemB, colPrioB, colRafB;
    @FXML
    private TableColumn<ProcesoVM, String> colNomB, colEstadoB;
    @FXML
    private Label lblTickB, lblActivosB, lblAlgB;

    @FXML
    private ContextMenu ctxA, ctxB;
    @FXML
    private MenuItem miTerminarA, miSuspenderA, miReanudarA;
    @FXML
    private MenuItem miTerminarB, miSuspenderB, miReanudarB;
    private Integer pidMenuA, pidMenuB;

    @FXML
    private Button btnStartAmbos, btnPauseAmbos, btnStopAmbos;

    private final ObservableList<ProcesoVM> datosA = FXCollections.observableArrayList();
    private final ObservableList<ProcesoVM> datosB = FXCollections.observableArrayList();

    private Simulador simA, simB;

    private ScheduledExecutorService scheduler;
    private boolean running = false;
    private boolean paused = false;
    private ParametrosSimulacion base;
    private TipoAlgoritmo algA, algB;
    private Random rng;
    private int tick = 0;
    private int nextPid = 1;

    private String runId;

    private final java.util.List<Integer> serieActivosA = new java.util.ArrayList<>();
    private final java.util.List<Integer> serieActivosB = new java.util.ArrayList<>();

    public void configurar(ParametrosSimulacion baseParams, TipoAlgoritmo a, TipoAlgoritmo b) {
        this.base = baseParams;
        this.algA = a;
        this.algB = b;

        lblAlgA.setText(a.name());
        lblAlgB.setText(b.name());

        this.runId = LogNombres.newRunId();
        Path logA = LogNombres.comparePath(runId, a);
        Path logB = LogNombres.comparePath(runId, b);

        var paramsA = new ParametrosSimulacion(
                base.tickMs, base.probNuevoProceso,
                base.rafagaMin, base.rafagaMax,
                base.prioridadMin, base.prioridadMax,
                base.seed, a, (a == TipoAlgoritmo.RR ? base.quantum : null));
        var paramsB = new ParametrosSimulacion(
                base.tickMs, base.probNuevoProceso,
                base.rafagaMin, base.rafagaMax,
                base.prioridadMin, base.prioridadMax,
                base.seed, b, (b == TipoAlgoritmo.RR ? base.quantum : null));

        simA = new Simulador(paramsA, logA, Simulador.ModoGeneracion.COORDINADO);
        simB = new Simulador(paramsB, logB, Simulador.ModoGeneracion.COORDINADO);

        simA.setOyente(vm -> Platform.runLater(() -> actualizarTablaA(vm.getTick(), vm.getFilas())));
        simB.setOyente(vm -> Platform.runLater(() -> actualizarTablaB(vm.getTick(), vm.getFilas())));

        rng = new Random(base.seed);
    }

    @FXML
    private void initialize() {
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

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(ctxA)
            );

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

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(ctxB)
            );

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
        serieActivosA.clear();
        serieActivosB.clear();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::tickCoordinado, 0, base.tickMs, TimeUnit.MILLISECONDS);

        // por consistencia, en "corriendo"
        simA.continuar();
        simB.continuar();

        refreshButtonsAB();
    }

    @FXML
    private void onShowEvolucionAB() {
        if (serieActivosA.isEmpty() && serieActivosB.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Aún no hay datos de evolución.").showAndWait();
            return;
        }

        int maxTicks = Math.max(serieActivosA.size(), serieActivosB.size());
        var x = new javafx.scene.chart.NumberAxis("Tick", 1, Math.max(maxTicks, 1), 1);
        var y = new javafx.scene.chart.NumberAxis();
        y.setLabel("Activos");

        var chart = new javafx.scene.chart.LineChart<Number, Number>(x, y);
        chart.setTitle("Evolución de activos (A/B)");

        var sA = new javafx.scene.chart.XYChart.Series<Number, Number>();
        sA.setName("A (" + algA.name() + ")");
        for (int i = 0; i < serieActivosA.size(); i++) {
            sA.getData().add(new javafx.scene.chart.XYChart.Data<>(i + 1, serieActivosA.get(i)));
        }

        var sB = new javafx.scene.chart.XYChart.Series<Number, Number>();
        sB.setName("B (" + algB.name() + ")");
        for (int i = 0; i < serieActivosB.size(); i++) {
            sB.getData().add(new javafx.scene.chart.XYChart.Data<>(i + 1, serieActivosB.get(i)));
        }

        chart.getData().setAll(sA, sB);

        var dlg = new Dialog<Void>();
        dlg.setTitle("Evolución A/B");
        dlg.initOwner(btnStartAmbos.getScene().getWindow());
        dlg.setResizable(true);
        dlg.getDialogPane().setContent(chart);
        dlg.getDialogPane().setPrefSize(860, 480);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    @FXML
    private void onPauseAmbos() {
        if (!running) {
            return;
        }

        if (!paused) {
            paused = true;
            simA.pausar();
            simB.pausar();
        } else {
            paused = false;
            simA.continuar();
            simB.continuar();
        }
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

        List<ProcesoSpec> llegadas = new ArrayList<>();
        if (rng.nextDouble() < base.probNuevoProceso) {
            int pid = nextPid++;
            int raf = randBetween(base.rafagaMin, base.rafagaMax);
            int prio = randBetween(base.prioridadMin, base.prioridadMax);
            long seed = rng.nextLong();
            llegadas.add(new ProcesoSpec(pid, "P" + pid, raf, prio, seed));
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

    @FXML
    private void onExportA() {
        try {
            var lista = simA.getMetricasTerminadasSnapshot();
            if (lista.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "Aún no hay métricas en A.").showAndWait();
                return;
            }
            var out = LogNombres.metricsComparePath(runId, algA);
            com.simulator.metrics.CsvMetricsWriter.write(out, lista);
            new Alert(Alert.AlertType.INFORMATION, "CSV A exportado en:\n" + out).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Error exportando A:\n" + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onExportB() {
        try {
            var lista = simB.getMetricasTerminadasSnapshot();
            if (lista.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "Aún no hay métricas en B.").showAndWait();
                return;
            }
            var out = LogNombres.metricsComparePath(runId, algB);
            com.simulator.metrics.CsvMetricsWriter.write(out, lista);
            new Alert(Alert.AlertType.INFORMATION, "CSV B exportado en:\n" + out).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Error exportando B:\n" + ex.getMessage()).showAndWait();
        }
    }

    private void actualizarTablaA(int tk, List<FilaProcesoVM> filas) {
        lblTickA.setText("Tick A: " + tk);
        lblActivosA.setText("Activos A: " + filas.size());

        Integer seleccionado = Optional.ofNullable(tblA.getSelectionModel().getSelectedItem())
                .map(vm -> vm.pid.get()).orElse(null);

        datosA.setAll(filas.stream()
                .map(f -> new ProcesoVM(f.pid(), f.nombre(), f.estado(),
                f.cpu(), f.memoria(), f.prioridad(), f.rafagaRestante()))
                .toList());

        if (seleccionado != null) {
            for (int i = 0; i < datosA.size(); i++) {
                if (datosA.get(i).pid.get() == seleccionado) {
                    tblA.getSelectionModel().select(i);
                    tblA.scrollTo(i);
                    break;
                }
            }
        }
        if (serieActivosA.size() == tk - 1) {
            serieActivosA.add(filas.size());
        } else if (tk - 1 < serieActivosA.size()) {
            serieActivosA.set(tk - 1, filas.size());
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

        if (serieActivosB.size() == tk - 1) {
            serieActivosB.add(filas.size());
        } else if (tk - 1 < serieActivosB.size()) {
            serieActivosB.set(tk - 1, filas.size());
        }

    }

    @FXML
    private void onShowResumenAB() {
        if (simA == null || simB == null) {
            return;
        }

        var la = simA.getMetricasTerminadasSnapshot();
        var lb = simB.getMetricasTerminadasSnapshot();

        if (la.isEmpty() && lb.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Aún no hay procesos terminados en A ni en B.").showAndWait();
            return;
        }

        TableView<RowAB> tv = new TableView<>();
        TableColumn<RowAB, String> cM = new TableColumn<>("Métrica");
        TableColumn<RowAB, String> cA = new TableColumn<>("A (" + algA.name() + ")");
        TableColumn<RowAB, String> cB = new TableColumn<>("B (" + algB.name() + ")");

        cM.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getMetrica()));
        cA.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getValorA()));
        cB.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getValorB()));

        tv.getColumns().addAll(cM, cA, cB);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        int nA = la.size();
        double aEspera = la.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.espera(m)).average().orElse(0);
        double aResp = la.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.respuesta(m)).average().orElse(0);
        double aTurn = la.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.turnaround(m)).average().orElse(0);
        double aEjec = la.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.ejecucion(m)).average().orElse(0);
        double aRaf = la.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.rafagaTotal(m)).average().orElse(0);

        int nB = lb.size();
        double bEspera = lb.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.espera(m)).average().orElse(0);
        double bResp = lb.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.respuesta(m)).average().orElse(0);
        double bTurn = lb.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.turnaround(m)).average().orElse(0);
        double bEjec = lb.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.ejecucion(m)).average().orElse(0);
        double bRaf = lb.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.rafagaTotal(m)).average().orElse(0);

        tv.getItems().addAll(
                new RowAB("Procesos", String.valueOf(nA), String.valueOf(nB)),
                new RowAB("Espera (prom)", fmt(aEspera), fmt(bEspera)),
                new RowAB("Respuesta (prom)", fmt(aResp), fmt(bResp)),
                new RowAB("Turnaround (prom)", fmt(aTurn), fmt(bTurn)),
                new RowAB("Ejecución (prom)", fmt(aEjec), fmt(bEjec)),
                new RowAB("Ráfaga total (prom)", fmt(aRaf), fmt(bRaf))
        );

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Resumen A/B");
        dlg.initOwner(btnStartAmbos.getScene().getWindow());
        dlg.setResizable(true);
        dlg.getDialogPane().setContent(tv);
        dlg.getDialogPane().setPrefSize(720, 380);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    private static final DecimalFormat DF = new DecimalFormat("#,##0.##");

    private static String fmt(double v) {
        return DF.format(v);
    }

    public static final class RowAB {

        private final String metrica, valorA, valorB;

        public RowAB(String m, String a, String b) {
            this.metrica = m;
            this.valorA = a;
            this.valorB = b;
        }

        public String getMetrica() {
            return metrica;
        }

        public String getValorA() {
            return valorA;
        }

        public String getValorB() {
            return valorB;
        }
    }

    @FXML
    private void onExportResumenAB() {
        try {
            var la = simA.getMetricasTerminadasSnapshot();
            var lb = simB.getMetricasTerminadasSnapshot();

            if (la.isEmpty() && lb.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION,
                        "Aún no hay procesos terminados en A ni en B.").showAndWait();
                return;
            }

            java.nio.file.Path csvA = com.simulator.sim.LogNombres.metricsComparePath(runId, algA);
            java.nio.file.Path dir = csvA.getParent();
            java.nio.file.Path out = dir.resolve("summary-compare.csv");

            com.simulator.metrics.CsvSummaryWriter.writeCompare(
                    out, algA.name(), la, algB.name(), lb
            );

            new Alert(Alert.AlertType.INFORMATION,
                    "Resumen A/B exportado en:\n" + out.toString()).showAndWait();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo exportar el resumen A/B:\n" + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onShowChartAB() {
        if (simA == null || simB == null) {
            return;
        }

        var la = simA.getMetricasTerminadasSnapshot();
        var lb = simB.getMetricasTerminadasSnapshot();
        if (la.isEmpty() && lb.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Aún no hay procesos terminados en A ni en B.").showAndWait();
            return;
        }

        double aEspera = la.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.espera(m)).average().orElse(0);
        double aResp = la.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.respuesta(m)).average().orElse(0);
        double aTurn = la.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.turnaround(m)).average().orElse(0);
        double aEjec = la.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.ejecucion(m)).average().orElse(0);
        double aRaf = la.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.rafagaTotal(m)).average().orElse(0);

        double bEspera = lb.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.espera(m)).average().orElse(0);
        double bResp = lb.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.respuesta(m)).average().orElse(0);
        double bTurn = lb.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.turnaround(m)).average().orElse(0);
        double bEjec = lb.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.ejecucion(m)).average().orElse(0);
        double bRaf = lb.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.rafagaTotal(m)).average().orElse(0);

        var mA = com.simulator.ui.charts.ChartsFactory.orderedMap();
        mA.put("Espera", aEspera);
        mA.put("Respuesta", aResp);
        mA.put("Turnaround", aTurn);
        mA.put("Ejecución", aEjec);
        mA.put("Ráfaga total", aRaf);

        var mB = com.simulator.ui.charts.ChartsFactory.orderedMap();
        mB.put("Espera", bEspera);
        mB.put("Respuesta", bResp);
        mB.put("Turnaround", bTurn);
        mB.put("Ejecución", bEjec);
        mB.put("Ráfaga total", bRaf);

        var node = com.simulator.ui.charts.ChartsFactory.barCompare(
                "Promedios A/B",
                "A (" + algA.name() + ")", mA,
                "B (" + algB.name() + ")", mB
        );

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Gráfica de métricas A/B");
        dlg.initOwner(btnStartAmbos.getScene().getWindow());
        dlg.setResizable(true);
        dlg.getDialogPane().setContent(node);
        dlg.getDialogPane().setPrefSize(860, 480);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

}
