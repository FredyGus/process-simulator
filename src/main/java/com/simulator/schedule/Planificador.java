package com.simulator.schedule;

import com.simulator.core.Proceso;

public interface Planificador {

    void agregarProceso(Proceso p);

    Proceso seleccionarProceso();

    void removerProceso(Proceso p);

    void reinicializar();

    default void onTick(Proceso running) {

    }

    default boolean debePreemptar(Proceso running) {
        return false;
    }

}
