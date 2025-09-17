package com.simulator.core;

import java.util.Objects;
import java.util.Random;

public final class Proceso {

    // Identidad
    private final int pid;
    private final String nombre;

    // Estado y atributos base
    private EstadoProceso estado = EstadoProceso.NEW;
    private final int prioridad;        // 1 = más alta (menor número)
    private int tiempoRestante;         // ráfaga restante (ticks)

    // Llegada (tick del sistema en que entra)
    private final int tiempoLlegada;

    // Consumo simulado
    private int cpuUsage;               // 0..100
    private int memoria;                // MB (>=0)

    // Métricas (contadores en tiempo real)
    private int tiempoEjecucion;        // ticks ejecutados (RUNNING)
    private int tiempoEspera;           // ticks esperando (READY)

    // Instrumentación de Fase 9a (tiempos clave)
    private final int tickLlegada;         // = tiempoLlegada (alias)
    private int tickPrimeraEjecucion = -1; // primer tick estando RUNNING
    private int tickFinalizacion = -1;     // tick cuando pasa a TERMINATED
    private final int rafagaTotal;         // ráfaga original

    // Fuente de aleatoriedad
    private final Random rng;

    // Rangos de simulación (ajústalos si quieres)
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

        // Valores iniciales
        this.cpuUsage = randBetween(CPU_MIN, 30); // 5..30% al inicio
        this.memoria = randBetween(MEM_MIN, 200); // 10..200 MB al inicio

        // Instrumentación
        this.tickLlegada = tiempoLlegada;
    }

    // ---------- Ciclo de Simulación ----------
    /**
     * Avanza un tick. El simulador debe llamar a este método para el proceso
     * RUNNING y puede llamarlo también para READY/BLOCKED para acumular
     * espera/estados.
     */
    public void avanzarTick(int tickActual) {
        switch (estado) {
            case RUNNING -> {
                // Marcar primera ejecución si corresponde
                if (tickPrimeraEjecucion < 0) {
                    tickPrimeraEjecucion = tickActual;
                }

                // Ejecuta
                tiempoRestante = Math.max(0, tiempoRestante - 1);
                tiempoEjecucion++;
                actualizarConsumoAleatorio();

                // ¿Terminó?
                if (tiempoRestante == 0) {
                    estado = EstadoProceso.TERMINATED;
                    cpuUsage = 0;
                    memoria = 0;
                    tickFinalizacion = tickActual;
                }
            }
            case READY -> {
                // Espera en cola
                tiempoEspera++;
                cpuUsage = 0; // en cola no consume CPU
                // Memoria se mantiene
            }
            case BLOCKED -> {
                cpuUsage = 0;
                // Memoria se mantiene
            }
            default -> {
                // NEW/TERMINATED: nada
            }
        }
    }

    /**
     * Cambia el estado del proceso. El simulador controla las transiciones. (No
     * marcamos primera ejecución aquí porque no recibimos tick; eso se hace en
     * avanzarTick)
     */
    public void cambiarEstado(EstadoProceso nuevo) {
        this.estado = nuevo;
        if (nuevo == EstadoProceso.READY) {
            cpuUsage = 0; // no consume CPU en cola
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

    // --------- Getters básicos ---------
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

    // --------- Instrumentación (Fase 9a) ---------
    public int getTickLlegada() {
        return tickLlegada;
    }

    /**
     * -1 si aún no ejecutó
     */
    public int getTickPrimeraEjecucion() {
        return tickPrimeraEjecucion;
    }

    /**
     * -1 si aún no terminó
     */
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

    // ------- Utilidades para planificadores -------
    // SJF: tiempo restante
    public int compareRemaining(Proceso other) {
        return Integer.compare(this.tiempoRestante, other.tiempoRestante);
    }

    // Prioridad: menor número = más prioridad
    public int comparePrioridad(Proceso other) {
        return Integer.compare(this.prioridad, other.prioridad);
    }

    /**
     * Terminar forzadamente (acción de usuario). No marcamos tickFinalizacion
     * aquí porque no recibimos el tick actual; el simulador puede completar ese
     * dato si lo requiere.
     */
    // Forzado desde la UI indicando el tick actual
    public void forzarTerminar(int tickActual) {
        this.estado = EstadoProceso.TERMINATED;
        this.tiempoRestante = 0;
        this.cpuUsage = 0;
        this.memoria = 0;
        this.tickFinalizacion = tickActual; // <-- este es el que usamos
    }

// Compatibilidad (si alguien lo llama sin tick)
    public void forzarTerminar() {
        forzarTerminar(-1);
    }

    public void acumularEsperaUnTick() {
        tiempoEspera++;
    }

}
