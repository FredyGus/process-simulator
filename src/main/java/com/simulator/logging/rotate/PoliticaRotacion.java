package com.simulator.logging.rotate;

import java.nio.file.Path;

public interface PoliticaRotacion {

    boolean debeRotar(Path archivoActual) throws Exception;

    Path rotar(Path archivoActual) throws Exception;
}
