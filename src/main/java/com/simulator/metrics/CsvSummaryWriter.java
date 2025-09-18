package com.simulator.metrics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.List;

/** Escribe resúmenes de métricas (promedios) a CSV. */
public final class CsvSummaryWriter {

    private static final DecimalFormat DF = new DecimalFormat("#,##0.##");

    private CsvSummaryWriter() {}

    /** Exporta resumen de una sola corrida: columnas -> metrica,valor */
    public static void writeSingle(Path out, List<ProcesoMetricas> lista) throws IOException {
        Files.createDirectories(out.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            bw.write("metrica,valor\n");
            int n = lista.size();
            double espera     = lista.stream().mapToInt(m -> MetricsCompat.espera(m)).average().orElse(0);
            double respuesta  = lista.stream().mapToInt(m -> MetricsCompat.respuesta(m)).average().orElse(0);
            double turnaround = lista.stream().mapToInt(m -> MetricsCompat.turnaround(m)).average().orElse(0);
            double ejecucion  = lista.stream().mapToInt(m -> MetricsCompat.ejecucion(m)).average().orElse(0);
            double rafaga     = lista.stream().mapToInt(m -> MetricsCompat.rafagaTotal(m)).average().orElse(0);

            bw.write("procesos," + n + "\n");
            bw.write("espera_prom,"     + DF.format(espera)     + "\n");
            bw.write("respuesta_prom,"  + DF.format(respuesta)  + "\n");
            bw.write("turnaround_prom," + DF.format(turnaround) + "\n");
            bw.write("ejecucion_prom,"  + DF.format(ejecucion)  + "\n");
            bw.write("rafaga_total_prom," + DF.format(rafaga)   + "\n");
        }
    }

    /** Exporta resumen comparado A/B: columnas -> metrica,A(<algA>),B(<algB>) */
    public static void writeCompare(Path out,
                                    String algA, List<ProcesoMetricas> la,
                                    String algB, List<ProcesoMetricas> lb) throws IOException {
        Files.createDirectories(out.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            bw.write("metrica,A(" + algA + "),B(" + algB + ")\n");

            int nA = la.size(), nB = lb.size();

            double aEspera     = la.stream().mapToInt(m -> MetricsCompat.espera(m)).average().orElse(0);
            double aResp       = la.stream().mapToInt(m -> MetricsCompat.respuesta(m)).average().orElse(0);
            double aTurn       = la.stream().mapToInt(m -> MetricsCompat.turnaround(m)).average().orElse(0);
            double aEjec       = la.stream().mapToInt(m -> MetricsCompat.ejecucion(m)).average().orElse(0);
            double aRaf        = la.stream().mapToInt(m -> MetricsCompat.rafagaTotal(m)).average().orElse(0);

            double bEspera     = lb.stream().mapToInt(m -> MetricsCompat.espera(m)).average().orElse(0);
            double bResp       = lb.stream().mapToInt(m -> MetricsCompat.respuesta(m)).average().orElse(0);
            double bTurn       = lb.stream().mapToInt(m -> MetricsCompat.turnaround(m)).average().orElse(0);
            double bEjec       = lb.stream().mapToInt(m -> MetricsCompat.ejecucion(m)).average().orElse(0);
            double bRaf        = lb.stream().mapToInt(m -> MetricsCompat.rafagaTotal(m)).average().orElse(0);

            bw.write("procesos,"          + nA + "," + nB + "\n");
            bw.write("espera_prom,"       + DF.format(aEspera) + "," + DF.format(bEspera) + "\n");
            bw.write("respuesta_prom,"    + DF.format(aResp)   + "," + DF.format(bResp)   + "\n");
            bw.write("turnaround_prom,"   + DF.format(aTurn)   + "," + DF.format(bTurn)   + "\n");
            bw.write("ejecucion_prom,"    + DF.format(aEjec)   + "," + DF.format(bEjec)   + "\n");
            bw.write("rafaga_total_prom," + DF.format(aRaf)    + "," + DF.format(bRaf)    + "\n");
        }
    }
}
