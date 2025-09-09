package com.simulator.core;

import java.util.Objects;
import java.util.Random;

public final class Proceso {

    private final int pid;
    private final String nombre;
    private EstadoProceso estado;

    private final int tiempoLlegada;
    private int tiempoRestante;     // rafaga restante (ticks)         
    private int prioridad;         // 1 = mas alta (menor numero)

    private int cpuUsage;           // 0...100%
    private int memoria;            // MB (>=0)

    // Metricas
    private int tiempoEjecucion;
    private int tiempoEspera;
    private Integer tiempoFinalizacion;

    private final Random rng;

    // Rangos de simulacion
    private static final int CPU_MIN = 5, CPU_MAX = 100;
    private static final int MEM_MIN = 10, MEM_MAX = 500;

    public Proceso(int pid, String nombre, int tiempoLlegada, int rafagaInicial, int prioridad, Random rng) {
        if (rafagaInicial <= 0) {
            throw new IllegalArgumentException("rafagaInicial debe ser > 0");
        }
        this.pid = pid;
        this.nombre = Objects.requireNonNull(nombre);
        this.tiempoLlegada = tiempoLlegada;
        this.tiempoRestante = rafagaInicial;
        this.prioridad = prioridad <= 0 ? 1 : prioridad;
        this.estado = EstadoProceso.NEW;
        this.rng = rng == null ? new Random() : rng;

        // Valores iniciales
        this.cpuUsage = randBetween(CPU_MIN, 30); // 5..30% al inicio
        this.memoria = randBetween(MEM_MIN, 200); // 10..200 MB al inicio
    }

    // ---------- Ciclo de Simulaccion ---------- 
    // Avanza un tick. El simulador debe llamar a este metodo para el proceso RUNNING
    public void avanzarTick(int tickActual) {
        switch (estado) {
            case RUNNING -> {
                tiempoRestante = Math.max(0, tiempoRestante - 1);
                tiempoEjecucion++;
                actualizarConsumoAleatorio();
                if (tiempoRestante == 0) {
                    estado = EstadoProceso.TERMINATED;
                    cpuUsage = 0;
                    memoria = 0;
                    tiempoFinalizacion = tickActual;
                }
            }
            case READY -> {
                tiempoEspera++;
                cpuUsage = 0;
                // Memoria se mantiene
            }
            case BLOCKED -> {
                cpuUsage = 0;
                // Memoria se mantien
            }
            default -> {
                /* NEW/TERMINATED: nada */ }
        }
    }

    // Cambia el estado del proceso. El simulador controla las transiciones
    public void cambiarEstado(EstadoProceso nuevo) {
        this.estado = nuevo;
        if (nuevo == EstadoProceso.READY) {
            cpuUsage = 0; // No consume cpu en cola
        }
    }

    // Recalcula CPU/MEM de forma aleatoria (usado en RUNNING cada tick)
    private void actualizarConsumoAleatorio() {
        this.cpuUsage = clamp(randBetween(CPU_MIN, CPU_MAX), 0, 100);
        this.memoria = clamp(randBetween(MEM_MIN, MEM_MAX), 0, 2048);
    }

    private int randBetween(int a, int b) {
        if (a > b) {
            int t = a;
            a = b;
            b = t;
        }
        return a + rng.nextInt(b - a + 1);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // --------- Getters ---------
    public int getPid() {
        return pid;
    }

    public String getNombre() {
        return nombre;
    }

    public EstadoProceso getEstado() {
        return estado;
    }

    public int getTiempoLlegada() {
        return tiempoLlegada;
    }

    public int getTiempoRestante() {
        return tiempoRestante;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public int getCpuUsage() {
        return cpuUsage;
    }

    public int getMemoria() {
        return memoria;
    }

    public int getTiempoEjecucion() {
        return tiempoEjecucion;
    }

    public int getTiempoEspera() {
        return tiempoEspera;
    }

    public Integer getTiempoFinalizacion() {
        return tiempoFinalizacion;
    }

    // ------- UTILIDADES -------
    // Util para SJF: Tiempo restante 
    public int compareRemaining(Proceso other) {
        return Integer.compare(this.tiempoRestante, other.tiempoRestante);
    }

    // Util para Prioridad: menor numero = mas prioridad
    public int comparePrioridad(Proceso other) {
        return Integer.compare(this.tiempoRestante, other.tiempoRestante);
    }

    public void forzarTerminar() {
        this.estado = EstadoProceso.TERMINATED;
        this.tiempoRestante = 0;
    }

}
