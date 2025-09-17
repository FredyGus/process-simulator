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
import java.util.List;
import java.util.Optional;

public class SingleRunController {

    // Tabla
    @FXML
    private TableView<ProcesoVM> tbl;
    @FXML
    private TableColumn<ProcesoVM, Number> colPid, colCpu, colMem, colPrio, colRaf;
    @FXML
    private TableColumn<ProcesoVM, String> colNom, colEstado;

    // Barra superior / estado
    //INICIA
    @FXML
    private Label lblTick, lblActivos;
    @FXML
    private Button btnStart, btnPause, btnStop;
    @FXML
    private Button btnExport;

    // Menú contextual
    @FXML
    private ContextMenu ctxMenu;
    @FXML
    private MenuItem miTerminar, miSuspender, miReanudar;
    private Integer pidMenu;

    // Datos y simulador
    private final ObservableList<ProcesoVM> datos = FXCollections.observableArrayList();
    private Simulador sim;

    // Guardamos los parámetros (necesario para exportar y para título)
    private ParametrosSimulacion params;

    // Estado de control
    private boolean running = false;
    private boolean paused = false;

    // =================== Ciclo de vida / configuración ===================
    public void configurar(ParametrosSimulacion params) {
        // guardamos para uso posterior (exportar CSV, etc.)
        this.params = params;

        // carpeta por corrida de single-run
        String runId = LogNombres.newRunId();
        Path logPath = LogNombres.runPath(runId, params.algoritmo);

        sim = new Simulador(params, logPath, Simulador.ModoGeneracion.AUTOGENERADO);
        sim.setOyente(vm -> Platform.runLater(() -> actualizarTabla(vm.getTick(), vm.getFilas())));
    }

    @FXML
    private void initialize() {
        // Columnas ↔ VM
        colPid.setCellValueFactory(c -> c.getValue().pid);
        colNom.setCellValueFactory(c -> c.getValue().nombre);
        colEstado.setCellValueFactory(c -> c.getValue().estado);
        colCpu.setCellValueFactory(c -> c.getValue().cpu);
        colMem.setCellValueFactory(c -> c.getValue().mem);
        colPrio.setCellValueFactory(c -> c.getValue().prioridad);
        colRaf.setCellValueFactory(c -> c.getValue().rafaga);
        tbl.setItems(datos);

        // Orden por CPU desc (estilo Task Manager)
        tbl.getSortOrder().setAll(colCpu);
        colCpu.setSortType(TableColumn.SortType.DESCENDING);

        // Menú contextual: capturamos el PID en el momento de abrirlo
        ctxMenu.setOnShowing(e -> {
            var vm = tbl.getSelectionModel().getSelectedItem();
            pidMenu = (vm != null) ? vm.pid.get() : null;
        });
        ctxMenu.setOnHidden(e -> pidMenu = null);

        // Asegurar selección de la fila bajo el cursor al abrir el menú
        tbl.setRowFactory(tv -> {
            TableRow<ProcesoVM> row = new TableRow<>();
            row.setOnContextMenuRequested(ev -> {
                if (!row.isEmpty()) {
                    tv.getSelectionModel().select(row.getIndex());
                }
            });
            return row;
        });

        // Acciones del menú
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

    // =================== Botones (Start / Pause / Stop / Export) ===================
    @FXML
    private void onStart() {
        if (sim == null || running) {
            return;
        }
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
            // Si tu Simulador implementa continuar(), úsalo.
            // Si no, puedes llamar otra vez a iniciar() o crear un método continuar().
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
        sim.detener();          // cierra logs y apaga el scheduler interno
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
            // Archivo CSV por algoritmo en single-run
            var out = LogNombres.metricsSinglePath(params.algoritmo);
            com.simulator.metrics.CsvMetricsWriter.write(out, lista);

            new Alert(Alert.AlertType.INFORMATION,
                    "CSV exportado en:\n" + out.toString()).showAndWait();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo exportar CSV:\n" + ex.getMessage()).showAndWait();
        }
    }

    // =================== UI helpers ===================
    private void refreshButtons() {
        btnStart.setDisable(running);
        btnPause.setDisable(!running);
        btnStop.setDisable(!running);
        btnPause.setText(paused ? "Reanudar" : "Pausar");
    }

    private void actualizarTabla(int tick, List<FilaProcesoVM> filas) {
        // Guardar selección actual por PID para restaurar después del refresh
        Integer seleccionado = getSelectedPid().orElse(null);

        lblTick.setText("Tick: " + tick);
        lblActivos.setText("Activos: " + filas.size());

        datos.setAll(
                filas.stream()
                        .map(f -> new ProcesoVM(
                        f.pid(), // record accessors
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
    }

    private Optional<Integer> getSelectedPid() {
        ProcesoVM vm = tbl.getSelectionModel().getSelectedItem();
        return (vm == null) ? Optional.empty() : Optional.of(vm.pid.get());
    }
}
