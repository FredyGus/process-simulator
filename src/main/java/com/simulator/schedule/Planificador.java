package com.simulator.schedule;

import com.simulator.core.Proceso;

public interface Planificador {

    // Agrega un proceso a la cola de listos del planificador
    void agregarProceso(Proceso p);

    // Selecciona el proceso que deberia ejecutar ahora
    Proceso seleccionarProceso();

    // Remueve el proceso (termino o lo saco el simulador)
    void removerProceso(Proceso p);

    // Limpia estructuras internas
    void reinicializar();

    // Hook opcional por tick (RR/aging). En FCFS no hace nada
    default void onTick() {

    }
}
