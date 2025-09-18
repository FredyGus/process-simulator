package com.simulator.ui;

import com.simulator.sim.LogNombres;
import com.simulator.sim.ParametrosSimulacion;
import com.simulator.sim.Simulador;
import com.simulator.sim.vm.FilaProcesoVM;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import javafx.beans.binding.Bindings;

public class SingleRunController {

    @FXML
    private TableView<ProcesoVM> tbl;
    @FXML
    private TableColumn<ProcesoVM, Number> colPid, colCpu, colMem, colPrio, colRaf;
    @FXML
    private TableColumn<ProcesoVM, String> colNom, colEstado;

    @FXML
    private Label lblTick, lblActivos;
    @FXML
    private Button btnStart, btnPause, btnStop, btnExport;

    @FXML
    private ContextMenu ctxMenu;
    @FXML
    private MenuItem miTerminar, miSuspender, miReanudar;
    private Integer pidMenu;

    private final ObservableList<ProcesoVM> datos = FXCollections.observableArrayList();
    private Simulador sim;

    private ParametrosSimulacion params;

    private boolean running = false;
    private boolean paused = false;
    private final java.util.List<Integer> serieActivos = new java.util.ArrayList<>();

    public void configurar(ParametrosSimulacion params) {
        this.params = params;

        String runId = LogNombres.newRunId();
        Path logPath = LogNombres.runPath(runId, params.algoritmo);

        sim = new Simulador(params, logPath, Simulador.ModoGeneracion.AUTOGENERADO);
        sim.setOyente(vm -> Platform.runLater(() -> actualizarTabla(vm.getTick(), vm.getFilas())));
    }

    @FXML
    private void initialize() {
        colPid.setCellValueFactory(c -> c.getValue().pid);
        colNom.setCellValueFactory(c -> c.getValue().nombre);
        colEstado.setCellValueFactory(c -> c.getValue().estado);
        colCpu.setCellValueFactory(c -> c.getValue().cpu);
        colMem.setCellValueFactory(c -> c.getValue().mem);
        colPrio.setCellValueFactory(c -> c.getValue().prioridad);
        colRaf.setCellValueFactory(c -> c.getValue().rafaga);
        tbl.setItems(datos);

        tbl.getSortOrder().setAll(colCpu);
        colCpu.setSortType(TableColumn.SortType.DESCENDING);

        ctxMenu.setOnShowing(e -> {
            var vm = tbl.getSelectionModel().getSelectedItem();
            pidMenu = (vm != null) ? vm.pid.get() : null;
        });
        ctxMenu.setOnHidden(e -> pidMenu = null);

        tbl.setRowFactory(tv -> {
            TableRow<ProcesoVM> row = new TableRow<>();

            row.setOnContextMenuRequested(ev -> {
                if (!row.isEmpty()) {
                    tv.getSelectionModel().select(row.getIndex());
                }
            });

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(ctxMenu)
            );

            return row;
        });

        miTerminar.setOnAction(e -> {
            if (pidMenu != null) {
                sim.terminarProceso(pidMenu);
            }
        });
        miSuspender.setOnAction(e -> {
            if (pidMenu != null) {
                sim.suspenderProceso(pidMenu);
            }
        });
        miReanudar.setOnAction(e -> {
            if (pidMenu != null) {
                sim.reanudarProceso(pidMenu);
            }
        });

        refreshButtons();
    }

    @FXML
    private void onStart() {
        if (sim == null || running) {
            return;
        }
        serieActivos.clear();
        sim.iniciar();
        running = true;
        paused = false;
        refreshButtons();
    }

    @FXML
    private void onPauseResume() {
        if (sim == null || !running) {
            return;
        }

        if (!paused) {
            sim.pausar();
            paused = true;
        } else {
            sim.continuar();
            paused = false;
        }
        refreshButtons();
    }

    @FXML
    private void onStop() {
        if (sim == null || !running) {
            return;
        }

        sim.detener();
        running = false;
        paused = false;
        refreshButtons();
    }

    @FXML
    private void onExportMetrics() {
        try {
            var lista = sim.getMetricasTerminadasSnapshot();
            if (lista.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION,
                        "Aún no hay procesos terminados para exportar.").showAndWait();
                return;
            }
            var out = LogNombres.metricsSinglePath(params.algoritmo);
            com.simulator.metrics.CsvMetricsWriter.write(out, lista);

            new Alert(Alert.AlertType.INFORMATION,
                    "CSV exportado en:\n" + out).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo exportar CSV:\n" + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onShowResumen() {
        if (sim == null) {
            return;
        }

        var lista = sim.getMetricasTerminadasSnapshot();
        if (lista.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Aún no hay procesos terminados.").showAndWait();
            return;
        }

        double avgEspera = lista.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.espera(m)).average().orElse(0);
        double avgRespuesta = lista.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.respuesta(m)).average().orElse(0);
        double avgTurnaround = lista.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.turnaround(m)).average().orElse(0);
        double avgEjecucion = lista.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.ejecucion(m)).average().orElse(0);
        double avgRafaga = lista.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.rafagaTotal(m)).average().orElse(0);

        int n = lista.size();

        TableView<RowMetric> tv = new TableView<>();
        TableColumn<RowMetric, String> c1 = new TableColumn<>("Métrica");
        TableColumn<RowMetric, String> c2 = new TableColumn<>("Valor");

        c1.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getNombre()));
        c2.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getValor()));
        tv.getColumns().addAll(c1, c2);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        tv.getItems().addAll(
                new RowMetric("Procesos", String.valueOf(n)),
                new RowMetric("Espera (prom)", fmt(avgEspera)),
                new RowMetric("Respuesta (prom)", fmt(avgRespuesta)),
                new RowMetric("Turnaround (prom)", fmt(avgTurnaround)),
                new RowMetric("Ejecución (prom)", fmt(avgEjecucion)),
                new RowMetric("Ráfaga total (prom)", fmt(avgRafaga))
        );

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Resumen de métricas");
        dlg.initOwner(btnStart.getScene().getWindow());
        dlg.setResizable(true);
        dlg.getDialogPane().setContent(tv);
        dlg.getDialogPane().setPrefSize(560, 360);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        com.simulator.ui.AppStyles.apply(dlg);
        dlg.showAndWait();
    }

    private void refreshButtons() {
        btnStart.setDisable(running);
        btnPause.setDisable(!running);
        btnStop.setDisable(!running);
        btnPause.setText(paused ? "Reanudar" : "Pausar");
    }

    private void actualizarTabla(int tick, List<FilaProcesoVM> filas) {
        Integer seleccionado = getSelectedPid().orElse(null);

        lblTick.setText("Tick: " + tick);
        lblActivos.setText("Activos: " + filas.size());

        datos.setAll(
                filas.stream()
                        .map(f -> new ProcesoVM(
                        f.pid(),
                        f.nombre(),
                        f.estado(),
                        f.cpu(),
                        f.memoria(),
                        f.prioridad(),
                        f.rafagaRestante()))
                        .toList()
        );

        if (seleccionado != null) {
            for (int i = 0; i < datos.size(); i++) {
                if (datos.get(i).pid.get() == seleccionado) {
                    tbl.getSelectionModel().select(i);
                    tbl.scrollTo(i);
                    break;
                }
            }
        }

        if (serieActivos.size() == tick - 1) {
            serieActivos.add(filas.size());
        } else if (tick - 1 < serieActivos.size()) {
            serieActivos.set(tick - 1, filas.size());
        }

    }

    @FXML
    private void onShowEvolucion() {
        if (serieActivos.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Aún no hay datos de evolución.").showAndWait();
            return;
        }

        var x = new javafx.scene.chart.NumberAxis("Tick", 1, Math.max(serieActivos.size(), 1), 1);
        var y = new javafx.scene.chart.NumberAxis();
        y.setLabel("Activos");

        var chart = new javafx.scene.chart.LineChart<Number, Number>(x, y);
        chart.setTitle("Evolución de activos por tick");

        var serie = new javafx.scene.chart.XYChart.Series<Number, Number>();

        serie.setName("Activos");
        for (int i = 0; i < serieActivos.size(); i++) {
            int tick = i + 1;
            serie.getData().add(new javafx.scene.chart.XYChart.Data<>(tick, serieActivos.get(i)));
        }
        chart.getData().add(serie);

        var dlg = new Dialog<Void>();
        dlg.setTitle("Evolución");
        dlg.initOwner(btnStart.getScene().getWindow());
        dlg.setResizable(true);
        dlg.getDialogPane().setContent(chart);
        dlg.getDialogPane().setPrefSize(720, 420);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        com.simulator.ui.AppStyles.apply(dlg);
        dlg.showAndWait();
    }

    private Optional<Integer> getSelectedPid() {
        var vm = tbl.getSelectionModel().getSelectedItem();
        return (vm == null) ? Optional.empty() : Optional.of(vm.pid.get());
    }

    private static final DecimalFormat DF = new DecimalFormat("#,##0.##");

    private static String fmt(double v) {
        return DF.format(v);
    }

    public static final class RowMetric {

        private final String nombre;
        private final String valor;

        public RowMetric(String nombre, String valor) {
            this.nombre = nombre;
            this.valor = valor;
        }

        public String getNombre() {
            return nombre;
        }

        public String getValor() {
            return valor;
        }
    }

    @FXML
    private void onExportResumen() {
        try {
            var lista = sim.getMetricasTerminadasSnapshot();
            if (lista.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION,
                        "Aún no hay procesos terminados para exportar.").showAndWait();
                return;
            }

            java.nio.file.Path metricsCsv = com.simulator.sim.LogNombres.metricsSinglePath(params.algoritmo);
            java.nio.file.Path dir = metricsCsv.getParent();
            java.nio.file.Path out = dir.resolve("summary-" + params.algoritmo.name() + ".csv");

            com.simulator.metrics.CsvSummaryWriter.writeSingle(out, lista);

            new Alert(Alert.AlertType.INFORMATION,
                    "Resumen exportado en:\n" + out.toString()).showAndWait();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo exportar el resumen:\n" + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onShowChart() {
        if (sim == null) {
            return;
        }

        var lista = sim.getMetricasTerminadasSnapshot();
        if (lista.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Aún no hay procesos terminados.").showAndWait();
            return;
        }

        double espera = lista.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.espera(m)).average().orElse(0);
        double respuesta = lista.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.respuesta(m)).average().orElse(0);
        double turnaround = lista.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.turnaround(m)).average().orElse(0);
        double ejecucion = lista.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.ejecucion(m)).average().orElse(0);
        double rafaga = lista.stream().mapToInt(m -> com.simulator.metrics.MetricsCompat.rafagaTotal(m)).average().orElse(0);

        var data = com.simulator.ui.charts.ChartsFactory.orderedMap();
        data.put("Espera", espera);
        data.put("Respuesta", respuesta);
        data.put("Turnaround", turnaround);
        data.put("Ejecución", ejecucion);
        data.put("Ráfaga total", rafaga);

        var node = com.simulator.ui.charts.ChartsFactory.barSingle(
                "Promedios (" + params.algoritmo.name() + ")", data);

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Gráfica de métricas");
        dlg.initOwner(btnStart.getScene().getWindow());
        dlg.setResizable(true);
        dlg.getDialogPane().setContent(node);
        dlg.getDialogPane().setPrefSize(720, 420);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        com.simulator.ui.AppStyles.apply(dlg);
        dlg.showAndWait();
    }

}
