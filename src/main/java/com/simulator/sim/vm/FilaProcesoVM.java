package com.simulator.sim.vm;

public record FilaProcesoVM(int pid, String nombre, String estado, int cpu, int memoria, int prioridad, int rafagaRestante) {

}
