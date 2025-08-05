package com.simulator.util;

import com.simulator.logging.SimulatorLogger;

public class LogTest {
    public static void main(String[] args) {
        SimulatorLogger.log("INICIO_SIMULACION");
        SimulatorLogger.logEvent("CREAR_PROCESO", "PID=1 | State=NEW");
        SimulatorLogger.logEvent("SCHEDULE", "PID=1 | State=RUNNING");
        SimulatorLogger.log("FIN_SIMULACION");
    }
}
