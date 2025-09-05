package com.simulator.sim;

// Especificacion de una llegada para modo coordinado (clonable en ambos simuladores)
public record ProcesoSpec (int pid, String nombre, int rafaga, int prioridad, long seed) {}