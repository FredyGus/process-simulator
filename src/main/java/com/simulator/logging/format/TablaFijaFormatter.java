package com.simulator.logging.format;

import com.simulator.logging.LogDatos;
import com.simulator.logging.LogMeta;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TablaFijaFormatter implements LogFormatter {

    private static final DateTimeFormatter TS_FMT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final TablaAnchos w = new TablaAnchos();

    @Override
    public String cabecera() {
        return String.format("%-" + w.ts + "s | %-" + w.evento + "s | %"
                + w.pid + "s | %-" + w.estado + "s | %"
                + w.cpu + "s | %" + w.mem + "s | %-" + w.alg + "s | %"
                + w.q + "s | %s",
                "TIMESTAMP", "EVENTO", "PID", "ESTADO", "CPU%", "MEM", "ALGORITMO", "QUANTUM", "DETALLE");
    }

    @Override
    public String separador() {
        return "-".repeat(w.ts + 1) + "+" + "-".repeat(w.evento + 2) + "+"
                + "-".repeat(w.pid + 2) + "+" + "-".repeat(w.estado + 2) + "+"
                + "-".repeat(w.cpu + 2) + "+" + "-".repeat(w.mem + 2) + "+"
                + "-".repeat(w.alg + 2) + "+" + "-".repeat(w.q + 2) + "+"
                + "-".repeat(29);
    }

    @Override
    public String formatear(LogMeta meta, LogDatos d) {
        String ts = TS_FMT.format(meta.timestamp());
        String evento = meta.evento().name();
        String pid = d.pid == null ? "-" : String.valueOf(d.pid);
        String estado = d.estado == null ? "-" : d.estado;
        String cpu = d.cpu == null ? "-" : (d.cpu + "%");
        String mem = d.mem == null ? "-" : (d.mem + "MB");
        String alg = d.algoritmo == null ? "-" : d.algoritmo;
        String q = d.quantum == null ? "-" : String.valueOf(d.quantum);
        String detalle = d.detalle == null ? "" : d.detalle;

        return String.format("%-" + w.ts + "s | %-" + w.evento + "s | %"
                + w.pid + "s | %-" + w.estado + "s | %"
                + w.cpu + "s | %" + w.mem + "s | %-" + w.alg + "s | %"
                + w.q + "s | %s",
                ts, evento, pid, estado, cpu, mem, alg, q, detalle);
    }

}
