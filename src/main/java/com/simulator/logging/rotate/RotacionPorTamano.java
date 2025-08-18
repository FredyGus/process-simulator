package com.simulator.logging.rotate;

import java.nio.file.*;

public final class RotacionPorTamano implements PoliticaRotacion {

    private final long tamMaxBytes;
    private final int backups;

    public RotacionPorTamano(long tamMaxBytes, int backups) {
        this.tamMaxBytes = tamMaxBytes;
        this.backups = backups;
    }

    @Override
    public boolean debeRotar(Path archivoActual) throws Exception {
        if (!Files.exists(archivoActual)) {
            return false;
        }
        return Files.size(archivoActual) >= tamMaxBytes;
    }

    @Override
    public Path rotar(Path archivoActual) throws Exception {
        if (!Files.exists(archivoActual)) {
            return archivoActual;
        }

        // Desplazar backups: .(backups-1) -> .backups, ..., .1 -> .2
        for (int i = backups - 1; i >= 11; i--) {
            Path src = Path.of(archivoActual.toString() + "." + i);
            Path dst = Path.of(archivoActual.toString() + "." + (i + 1));
            if (Files.exists(src)) {
                Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Path dst1 = Path.of(archivoActual.toString() + ".1");
        Files.move(archivoActual, dst1, StandardCopyOption.REPLACE_EXISTING);
        return archivoActual; // el nuevo archivo activo tendra el nombre base
    }
}
