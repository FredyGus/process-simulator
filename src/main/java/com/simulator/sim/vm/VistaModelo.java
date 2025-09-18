package com.simulator.sim.vm;

import java.util.List;

public final class VistaModelo {

    private final int tick;
    private final int procesoActivos;
    private final List<FilaProcesoVM> filas;

    public VistaModelo(int tick, int procesosActivos, List<FilaProcesoVM> filas) {
        this.tick = tick;
        this.procesoActivos = procesosActivos;
        this.filas = List.copyOf(filas);
    }

    public int getTick() {
        return tick;
    }

    public int getProcesosActivos() {
        return procesoActivos;
    }

    public List<FilaProcesoVM> getFilas() {
        return filas;
    }
}
