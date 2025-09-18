package com.simulator.core;

import java.util.Objects;
import java.util.Random;

public final class Proceso {

    private final int pid;
    private final String nombre;

    private EstadoProceso estado = EstadoProceso.NEW;
    private final int prioridad;        // 1 = más alta (menor número)
    private int tiempoRestante;         // ráfaga restante (ticks)

    private final int tiempoLlegada;

    private int cpuUsage;               // 0..100
    private int memoria;                // MB (>=0)

    private int tiempoEjecucion;        // ticks ejecutados (RUNNING)
    private int tiempoEspera;           // ticks esperando (READY)

    private final int tickLlegada;         // = tiempoLlegada (alias)
    private int tickPrimeraEjecucion = -1; // primer tick estando RUNNING
    private int tickFinalizacion = -1;     // tick cuando pasa a TERMINATED
    private final int rafagaTotal;         // ráfaga original

    private final Random rng;

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
        this.rafagaTotal = rafagaInicial;
        this.prioridad = (prioridad <= 0) ? 1 : prioridad;
        this.estado = EstadoProceso.NEW;

        this.rng = (rng == null) ? new Random() : rng;

        this.cpuUsage = randBetween(CPU_MIN, 30); // 5..30% al inicio
        this.memoria = randBetween(MEM_MIN, 200); // 10..200 MB al inicio

        this.tickLlegada = tiempoLlegada;
    }

    public void avanzarTick(int tickActual) {
        switch (estado) {
            case RUNNING -> {
                if (tickPrimeraEjecucion < 0) {
                    tickPrimeraEjecucion = tickActual;
                }

                tiempoRestante = Math.max(0, tiempoRestante - 1);
                tiempoEjecucion++;
                actualizarConsumoAleatorio();

                if (tiempoRestante == 0) {
                    estado = EstadoProceso.TERMINATED;
                    cpuUsage = 0;
                    memoria = 0;
                    tickFinalizacion = tickActual;
                }
            }
            case READY -> {
                tiempoEspera++;
                cpuUsage = 0; 
            }
            case BLOCKED -> {
                cpuUsage = 0;
            }
            default -> {
                // NEW/TERMINATED: nada
            }
        }
    }

    
    public void cambiarEstado(EstadoProceso nuevo) {
        this.estado = nuevo;
        if (nuevo == EstadoProceso.READY) {
            cpuUsage = 0; 
        }
    }

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

    public int getPid() {
        return pid;
    }

    public String getNombre() {
        return nombre;
    }

    public EstadoProceso getEstado() {
        return estado;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public int getTiempoLlegada() {
        return tiempoLlegada;
    }

    public int getTiempoRestante() {
        return tiempoRestante;
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

    public int getTickLlegada() {
        return tickLlegada;
    }

    
    public int getTickPrimeraEjecucion() {
        return tickPrimeraEjecucion;
    }

    public int getTickFinalizacion() {
        return tickFinalizacion;
    }

    public int getRafagaTotal() {
        return rafagaTotal;
    }

    public boolean haComenzado() {
        return tickPrimeraEjecucion >= 0;
    }

    public boolean haTerminado() {
        return tickFinalizacion >= 0;
    }

    public int compareRemaining(Proceso other) {
        return Integer.compare(this.tiempoRestante, other.tiempoRestante);
    }

    public int comparePrioridad(Proceso other) {
        return Integer.compare(this.prioridad, other.prioridad);
    }

    public void forzarTerminar(int tickActual) {
        this.estado = EstadoProceso.TERMINATED;
        this.tiempoRestante = 0;
        this.cpuUsage = 0;
        this.memoria = 0;
        this.tickFinalizacion = tickActual;
    }

    public void forzarTerminar() {
        forzarTerminar(-1);
    }

    public void acumularEsperaUnTick() {
        tiempoEspera++;
    }

}
