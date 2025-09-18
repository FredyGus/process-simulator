package com.simulator;

import com.simulator.sim.*;

public class SimuladorQuickRun {
    public static void main(String[] args) throws InterruptedException {
        var params = ParametrosSimulacion.defaultFCFS();
        var logPath = LogNombres.singleRunPath(params.algoritmo);
        
        var sim = new Simulador(params, logPath);
        sim.iniciar();
        
        Thread.sleep(10_000);
        
        sim.detener();
        System.out.println("Simulacion finalizada. Revisa el log: " + logPath);
        
    }
}