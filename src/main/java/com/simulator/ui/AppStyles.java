package com.simulator.ui;

import javafx.scene.Scene;
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
}
