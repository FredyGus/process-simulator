package com.simulator.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxml = new FXMLLoader(getClass().getResource("/ui/home_view.fxml"));
        Scene scene = new Scene(fxml.load(), 980, 640);
        stage.setTitle("Process Simulator");
        stage.setScene(scene);
        stage.show();
    }
    
    public static void main(String[] args){
        launch(args);
    }
}
