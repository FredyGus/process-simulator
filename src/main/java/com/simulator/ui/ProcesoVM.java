package com.simulator.ui;

import javafx.beans.property.*;

public class ProcesoVM {

    public final IntegerProperty pid = new SimpleIntegerProperty();
    public final StringProperty nombre = new SimpleStringProperty();
    public final StringProperty estado = new SimpleStringProperty();
    public final IntegerProperty cpu = new SimpleIntegerProperty();
    public final IntegerProperty mem = new SimpleIntegerProperty();
    public final IntegerProperty prioridad = new SimpleIntegerProperty();
    public final IntegerProperty rafaga = new SimpleIntegerProperty();

    public ProcesoVM(int pid, String nombre, String estado, int cpu, int mem, int prioridad, int rafaga) {
        this.pid.set(pid);
        this.nombre.set(nombre);
        this.estado.set(estado);
        this.cpu.set(cpu);
        this.mem.set(mem);
        this.prioridad.set(prioridad);
        this.rafaga.set(rafaga);
    }
}
