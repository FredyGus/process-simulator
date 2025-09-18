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
        cboAlg.getItems().setAll(TipoAlgoritmo.values());
        cboAlg.getSelectionModel().select(TipoAlgoritmo.FCFS);

        spnTick.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 2000, 500, 50));
        spnProb.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.35, 0.05));
        spnQuantum.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 3, 1));
        txtSeed.setText("12345");

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

        Scene sc = new Scene(root, 672, 600);
        AppStyles.apply(sc);
        stage.setScene(sc);

        stage.show();

    }

    @FXML
    private void onComparar(ActionEvent e) {
        try {
            var algA = cboAlg.getValue();

            var opciones = new ArrayList<>(Arrays.asList(TipoAlgoritmo.values()));
            opciones.remove(algA);

            if (opciones.isEmpty()) {
                new Alert(Alert.AlertType.WARNING,
                        "No hay otro algoritmo disponible para comparar con " + algA).showAndWait();
                return;
            }

            var dlg = new ChoiceDialog<>(opciones.get(0), opciones);
            dlg.setTitle("Confirmación");
            dlg.setHeaderText("Selecciona el Algoritmo B para comparar");
            dlg.setContentText("Algoritmo B:");
            var opt = dlg.showAndWait();
            if (opt.isEmpty()) {
                return;
            }

            var algB = opt.get();

            Integer qBase = null;
            if (algA == TipoAlgoritmo.RR || algB == TipoAlgoritmo.RR) {
                qBase = spnQuantum.isDisabled() ? 3 : spnQuantum.getValue();
            }

            var base = new ParametrosSimulacion(
                    spnTick.getValue(), spnProb.getValue(),
                    5, 12, 1, 5,
                    Long.parseLong(txtSeed.getText().trim()),
                    algA,
                    qBase
            );

            FXMLLoader fxml = new FXMLLoader(getClass().getResource("/ui/compare_view.fxml"));
            Parent root = fxml.load();

            CompareController ctl = fxml.getController();
            ctl.configurar(base, algA, algB);

            Stage stage = new Stage();
            stage.setTitle("Comparar: " + algA + " vs " + algB);

            Scene sc = new Scene(root, 1208, 620);
            AppStyles.apply(sc);
            stage.setScene(sc);

            stage.show();

        } catch (Exception ex) {
            AppStyles.error("No se pudo abrir el comparador:\n" + ex.getMessage());
            //new Alert(Alert.AlertType.ERROR, "No se pudo abrir el comparador:\n" + ex.getMessage()).showAndWait();
        }
    }
}
