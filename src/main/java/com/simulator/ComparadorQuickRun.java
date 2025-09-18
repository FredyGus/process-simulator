package com.simulator;

import com.simulator.sim.*;
import com.simulator.sim.vm.VistaModelo;

public class ComparadorQuickRun {
    public static void main(String[] args) throws InterruptedException {
        var base = new ParametrosSimulacion(500, 0.35, 5, 12, 1, 5, 12345L, TipoAlgoritmo.FCFS, 3); // Usamos sus parametros generales
        TipoAlgoritmo A = TipoAlgoritmo.FCFS;
        TipoAlgoritmo B = TipoAlgoritmo.RR;
        
        var comp = new ComparadorAlgoritmos(base, A, B, new ComparadorAlgoritmos.Oyente() {
            @Override
            public void onModeloActualizadoA(VistaModelo vmA) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public void onModeloActualizadoB(VistaModelo vmB) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }
        });
        
        comp.iniciar();
        Thread.sleep(10_000);
        comp.detener();
        
        System.out.println("Comparacion dinalizada. Revisa la carpeta logs/compare/");
    }
}