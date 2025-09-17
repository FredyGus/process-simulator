package com.simulator.metrics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public final class CsvMetricsWriter {

    private CsvMetricsWriter() {
    }

    public static void write(Path file, List<ProcesoMetricas> rows) throws IOException {
        Files.createDirectories(file.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            w.write(String.join(",",
                    "pid", "algoritmo",
                    "llegada", "primera_ejec", "fin",
                    "rafaga_total", "ejecucion", "espera",
                    "respuesta", "turnaround"));
            w.newLine();

            for (ProcesoMetricas m : rows) {
                w.write(String.format("%d,%s,%d,%s,%s,%d,%d,%d,%s,%s",
                        m.pid(),
                        m.algoritmo(),
                        m.tickLlegada(),
                        toCsvNum(m.tickPrimeraEjec()),
                        toCsvNum(m.tickFin()),
                        m.rafagaTotal(),
                        m.tiempoEjecucion(),
                        m.tiempoEspera(),
                        toCsvNum(m.tiempoRespuesta()),
                        toCsvNum(m.turnaround())
                ));
                w.newLine();
            }
        }
    }

    private static String toCsvNum(int v) {
        return Integer.toString(v);
    }

    private static String toCsvNum(Integer v) {
        return v == null ? "" : Integer.toString(v);
    }
}
