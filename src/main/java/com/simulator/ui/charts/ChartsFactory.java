package com.simulator.ui.charts;

import javafx.scene.chart.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ChartsFactory {

    private ChartsFactory() {
    }

    private static final DecimalFormat DF = new DecimalFormat("#,##0.##");

    public static Node barSingle(String titulo, Map<String, Number> metricas) {
        var x = new CategoryAxis();
        var y = new NumberAxis();
        x.setLabel("Métrica");
        y.setLabel("Promedio");
        var chart = new BarChart<>(x, y);
        chart.setTitle(titulo);
        chart.setLegendVisible(false);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        metricas.forEach((k, v) -> serie.getData().add(new XYChart.Data<>(k, v)));
        chart.getData().add(serie);

        BorderPane bp = new BorderPane(chart);
        bp.setMinSize(640, 360);
        return bp;
    }

    public static Node barCompare(String titulo,
            String etiquetaA, Map<String, Number> mA,
            String etiquetaB, Map<String, Number> mB) {
        var x = new CategoryAxis();
        var y = new NumberAxis();
        x.setLabel("Métrica");
        y.setLabel("Promedio");
        var chart = new BarChart<>(x, y);
        chart.setTitle(titulo);
        chart.setLegendVisible(true);

        XYChart.Series<String, Number> serieA = new XYChart.Series<>();
        serieA.setName(etiquetaA);
        XYChart.Series<String, Number> serieB = new XYChart.Series<>();
        serieB.setName(etiquetaB);

        for (String k : mA.keySet()) {
            x.getCategories().add(k);
            serieA.getData().add(new XYChart.Data<>(k, mA.get(k)));
            Number vb = mB.get(k);
            if (vb != null) {
                serieB.getData().add(new XYChart.Data<>(k, vb));
            }
        }
        for (String k : mB.keySet()) {
            if (!x.getCategories().contains(k)) {
                x.getCategories().add(k);
                serieB.getData().add(new XYChart.Data<>(k, mB.get(k)));
            }
        }

        chart.getData().addAll(serieA, serieB);
        BorderPane bp = new BorderPane(chart);
        bp.setMinSize(720, 400);
        return bp;
    }

    public static Map<String, Number> orderedMap() {
        return new LinkedHashMap<>();
    }
}
