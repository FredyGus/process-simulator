package com.simulator.logging;

public interface LogWriter {

    void abrir(LogConfig config) throws Exception;

    void escribir(String linea) throws Exception;

    void cerrar() throws Exception;
}
