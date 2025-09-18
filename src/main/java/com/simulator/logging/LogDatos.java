package com.simulator.logging;

public final class LogDatos {

    public final Integer pid;       // null si no aplica
    public final String estado;     // null si no aplica
    public final Integer cpu;       // 0..100, null si no aplica
    public final Integer mem;       // MB, null si no aplica
    public final String algoritmo;  // FCFS, SJF, RR, PRIORIDAD o null
    public final Integer quantum;   // RR o nul
    public final String detalle;    // texto libre

    public LogDatos(Integer pid, String estado, Integer cpu, Integer mem,
            String algoritmo, Integer quatum, String detalle) {
        this.pid = pid;
        this.estado = estado;
        this.cpu = cpu;
        this.mem = mem;
        this.algoritmo = algoritmo;
        this.quantum = quatum;
        this.detalle = detalle == null ? "" : detalle;
    }

    public static LogDatos vacio() {
        return new LogDatos(null, null, null, null, null, null, "");
    }

}
