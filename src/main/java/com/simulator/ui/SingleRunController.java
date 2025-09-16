package com.simulator.ui;

import com.simulator.sim.*;
import com.simulator.sim.vm.FilaProcesoVM;   // <- para que 'f' tenga getters
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.nio.file.Path;
import java.util.Optional;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;

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
    private Button btnStart, btnStop;
    @FXML
    private MenuItem miTerminar;
    @FXML
    private MenuItem miSuspender;
    @FXML
    private MenuItem miReanudar;
    @FXML
    private ContextMenu ctxMenu;
    private Integer pidMenu;

    private final ObservableList<ProcesoVM> datos = FXCollections.observableArrayList();
    private Simulador sim;

    public void configurar(ParametrosSimulacion params) {

        // AHORA: una carpeta por corrida de single run
        String runId = LogNombres.newRunId();          // p.ej. "run-2025_09_06-01_25_30"
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

        // ordenar visual por CPU desc (estilo Task Manager)
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
    }

    @FXML
    private void onStart() {
        btnStart.setDisable(true);
        btnStop.setDisable(false);
        sim.iniciar();
    }

    @FXML
    private void onStop() {
        btnStart.setDisable(false);
        btnStop.setDisable(true);
        sim.detener();
    }

    private void actualizarTabla(int tick, java.util.List<FilaProcesoVM> filas) {
        Integer seleccionado = getSelectedPid().orElse(null);
        lblTick.setText("Tick: " + tick);
        lblActivos.setText("Activos: " + filas.size());

        datos.setAll(
                filas.stream()
                        .map(f -> new ProcesoVM(
                        f.pid(), // <-- record accessor (no get)
                        f.nombre(),
                        f.estado(),
                        f.cpu(),
                        f.memoria(),
                        f.prioridad(),
                        f.rafagaRestante()))
                        .toList() // o .collect(Collectors.toList()) si lo prefieres
        );
        
        if (seleccionado != null){
            int idx = -1;
            for (int i = 0; i < datos.size(); i++){
                if (datos.get(i).pid.get() == seleccionado ){ idx = i; break;}
            }
            if (idx >= 0){
                tbl.getSelectionModel().select(idx);
                tbl.scrollTo(idx);
            }
        }
    }

    private Optional<Integer> getSelectedPid() {
        ProcesoVM vm = tbl.getSelectionModel().getSelectedItem();
        if (vm == null) {
            return Optional.empty();
        }
        // Si ProcesoVM expone propiedades JavaFX públicas:
        return Optional.of(vm.pid.get());
        // Si en tu ProcesoVM usas getters (getPid()):
        // return Optional.of(vm.getPid());
    }

}
