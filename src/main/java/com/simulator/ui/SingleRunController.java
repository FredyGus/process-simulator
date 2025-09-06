package com.simulator.ui;

import com.simulator.sim.*;
import com.simulator.sim.vm.FilaProcesoVM;   // <- para que 'f' tenga getters
import java.util.stream.Collectors;          // <- si usamos collect(toList)
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.nio.file.Path;

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

    private final ObservableList<ProcesoVM> datos = FXCollections.observableArrayList();
    private Simulador sim;

    public void configurar(ParametrosSimulacion params) {
        Path logPath = LogNombres.singleRunPath(params.algoritmo);
        sim = new Simulador(params, logPath);
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
    lblTick.setText("Tick: " + tick);
    lblActivos.setText("Activos: " + filas.size());

    datos.setAll(
        filas.stream()
             .map(f -> new ProcesoVM(
                     f.pid(),              // <-- record accessor (no get)
                     f.nombre(),
                     f.estado(),
                     f.cpu(),
                     f.memoria(),
                     f.prioridad(),
                     f.rafagaRestante()))
             .toList()                   // o .collect(Collectors.toList()) si lo prefieres
    );
}


}
