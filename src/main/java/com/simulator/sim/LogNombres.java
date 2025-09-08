package com.simulator.sim;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class LogNombres {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy_MM_dd-HH_mm_ss");

    public static Path singleRunPath(TipoAlgoritmo alg) {
        String ts = LocalDateTime.now().format(FMT);
        return Paths.get("logs", "sim-" + alg.name() + "-" + ts + ".log");
    }

    public static String newRunId() {
        return "run-" + LocalDateTime.now().format(FMT);
    }

    public static Path compareDir(String runId) {
        Path dir = Paths.get("logs", "compare", runId);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return dir;
    }

    public static Path comparePath(String runId, TipoAlgoritmo alg) {
        return compareDir(runId).resolve("sim-" + alg.name() + ".log");
    }

    /**
     * Carpeta para single run: logs/run-<ts>
     */
    public static Path runDir(String runId) {
        Path dir = Paths.get("logs", runId); // runId ya trae el prefijo "run-..."
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return dir;
    }

    /**
     * Archivo de log dentro de la carpeta del single run
     */
    public static Path runPath(String runId, TipoAlgoritmo alg) {
        return runDir(runId).resolve("sim-" + alg.name() + ".log");
    }

}
