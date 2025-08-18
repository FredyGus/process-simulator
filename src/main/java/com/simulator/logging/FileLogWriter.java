package com.simulator.logging;

import com.simulator.logging.rotate.PoliticaRotacion;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;

public final class FileLogWriter implements LogWriter {

    private Path archivoActual;
    private BufferedWriter out;
    private PoliticaRotacion rotacion;
    private Charset charset;

    @Override
    public void abrir(LogConfig config) throws Exception {
        this.archivoActual = config.rutaBase;
        this.rotacion = config.politicaRotacion;
        this.charset = config.charset;
        ensureParentDir(archivoActual);
        out = Files.newBufferedWriter(archivoActual,
                charset,
                config.append ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND}
                : new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING});
    }

    @Override
    public void escribir(String linea) throws Exception {
        if (rotacion != null && rotacion.debeRotar(archivoActual)) {
            // cerrar, rotar, reabrir
            out.flush();
            out.close();
            rotacion.rotar(archivoActual);
            out = Files.newBufferedWriter(archivoActual, charset, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        out.write(linea);
        out.newLine();
        out.flush();
    }

    @Override
    public void cerrar() throws Exception {
        if (out != null) {
            try {
                out.flush();
            } catch (IOException ignored) {
            }
            try {
                out.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void ensureParentDir(Path p) throws IOException {
        Path parent = p.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
}
