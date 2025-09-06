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

public class HomeController {
    @FXML private ComboBox<TipoAlgoritmo> cboAlg;
    @FXML private Spinner<Integer> spnTick;
    @FXML private Spinner<Double> spnProb;
    @FXML private Spinner<Integer> spnQuantum;
    @FXML private TextField txtSeed;

    @FXML
    private void initialize() {
        cboAlg.getItems().setAll(TipoAlgoritmo.FCFS, TipoAlgoritmo.RR);
        cboAlg.getSelectionModel().select(TipoAlgoritmo.FCFS);
        spnTick.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 2000, 500, 50));
        spnProb.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.35, 0.05));
        spnQuantum.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 3, 1));
        txtSeed.setText("12345");
        cboAlg.valueProperty().addListener((obs, a, b) -> spnQuantum.setDisable(b != TipoAlgoritmo.RR));
        spnQuantum.setDisable(true);
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
        if (ctl instanceof SingleRunController c) c.configurar(params);
        Stage stage = new Stage();
        stage.setTitle("Simulación (" + alg + ")");
        stage.setScene(new Scene(root, 1024, 600));
        stage.show();
    }

    @FXML
    private void onComparar(ActionEvent e) {
        // Lo hacemos en 6b (siguiente paso).
        new Alert(Alert.AlertType.INFORMATION, "Comparador: lo implementamos en el siguiente paso.").showAndWait();
    }
}
