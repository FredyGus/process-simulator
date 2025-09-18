package com.simulator.ui;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;

public final class AppStyles {

    private AppStyles() {
    }
    public static final String URL = AppStyles.class.getResource("/ui/app.css").toExternalForm();

    public static void apply(Scene scene) {
        if (!scene.getStylesheets().contains(URL)) {
            scene.getStylesheets().add(URL);
        }
    }

    public static void apply(Dialog<?> dialog) {
        var pane = dialog.getDialogPane();
        if (!pane.getStylesheets().contains(URL)) {
            pane.getStylesheets().add(URL);
        }
        if (!pane.getStyleClass().contains("app-root")) {
            pane.getStyleClass().add("app-root");
        }
    }

    public static void show(Alert.AlertType type, String text) {
        Alert a = new Alert(type, text);
        apply(a);
        a.showAndWait();
    }

    public static void error(String text) {
        show(Alert.AlertType.ERROR, text);
    }

    public static void warn(String text) {
        show(Alert.AlertType.WARNING, text);
    }

    public static void info(String text) {
        show(Alert.AlertType.INFORMATION, text);
    }
}