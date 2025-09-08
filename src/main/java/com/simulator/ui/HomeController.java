package com.simulator.ui;

import com.simulator.sim.ParametrosSimulacion;
import com.simulator.sim.TipoAlgoritmo;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Parent;

import java.util.ArrayList;
import java.util.Arrays;

public class HomeController {

    @FXML
    private ComboBox<TipoAlgoritmo> cboAlg;
    @FXML
    private Spinner<Integer> spnTick;
    @FXML
    private Spinner<Double> spnProb;
    @FXML
    private Spinner<Integer> spnQuantum;
    @FXML
    private TextField txtSeed;

    @FXML
    private void initialize() {
        // Algoritmos disponibles
        cboAlg.getItems().setAll(TipoAlgoritmo.values());
        cboAlg.getSelectionModel().select(TipoAlgoritmo.FCFS);

        // Parámetros base
        spnTick.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 2000, 500, 50));
        spnProb.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.35, 0.05));
        spnQuantum.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 3, 1));
        txtSeed.setText("12345");

        // Quantum solo para RR (A)
        cboAlg.valueProperty().addListener((obs, a, b) -> spnQuantum.setDisable(b != TipoAlgoritmo.RR));
        spnQuantum.setDisable(cboAlg.getValue() != TipoAlgoritmo.RR); // estado inicial correcto
    }

    @FXML
    private void onIniciar(ActionEvent e) throws Exception {
        var alg = cboAlg.getValue();
        var params = new ParametrosSimulacion(
                spnTick.getValue(), spnProb.getValue(),
                5, 12, 1, 5,
                Long.parseLong(txtSeed.getText().trim()),
                alg,
                alg == TipoAlgoritmo.RR ? spnQuantum.getValue() : null
        );

        FXMLLoader fxml = new FXMLLoader(getClass().getResource("/ui/single_view.fxml"));
        Parent root = fxml.load();
        var ctl = fxml.getController();
        if (ctl instanceof SingleRunController c) {
            c.configurar(params);
        }
        Stage stage = new Stage();
        stage.setTitle("Simulación (" + alg + ")");
        stage.setScene(new Scene(root, 600, 600));
        stage.show();
    }

    @FXML
    private void onComparar(ActionEvent e) {
        try {
            // A = seleccionado en el combo principal
            var algA = cboAlg.getValue();

            // Opciones para B: todos menos A (prohibimos A vs A)
            var opciones = new ArrayList<>(Arrays.asList(TipoAlgoritmo.values()));
            opciones.remove(algA);

            if (opciones.isEmpty()) {
                new Alert(Alert.AlertType.WARNING,
                        "No hay otro algoritmo disponible para comparar con " + algA).showAndWait();
                return;
            }

            // Diálogo para seleccionar B
            var dlg = new ChoiceDialog<>(opciones.get(0), opciones);
            dlg.setTitle("Confirmación");
            dlg.setHeaderText("Selecciona el Algoritmo B para comparar");
            dlg.setContentText("Algoritmo B:");
            var opt = dlg.showAndWait();
            if (opt.isEmpty()) return;

            var algB = opt.get();

            // Quantum base:
            // - Si alguno es RR, enviamos un quantum.
            // - Si el spinner está deshabilitado (A ≠ RR), usamos 3 por defecto.
            Integer qBase = null;
            if (algA == TipoAlgoritmo.RR || algB == TipoAlgoritmo.RR) {
                qBase = spnQuantum.isDisabled() ? 3 : spnQuantum.getValue();
            }

            // Parámetros base (tick, prob, seed, rangos)
            var base = new ParametrosSimulacion(
                    spnTick.getValue(), spnProb.getValue(),
                    5, 12, 1, 5,
                    Long.parseLong(txtSeed.getText().trim()),
                    algA,   // se sobreescribe por lado dentro del comparador
                    qBase
            );

            // Cargar vista comparador y pasar config
            FXMLLoader fxml = new FXMLLoader(getClass().getResource("/ui/compare_view.fxml"));
            Parent root = fxml.load();
            CompareController ctl = fxml.getController();
            ctl.configurar(base, algA, algB);

            Stage stage = new Stage();
            stage.setTitle("Comparar: " + algA + " vs " + algB);
            stage.setScene(new Scene(root, 1200, 620));
            stage.show();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el comparador:\n" + ex.getMessage()).showAndWait();
        }
    }
}
