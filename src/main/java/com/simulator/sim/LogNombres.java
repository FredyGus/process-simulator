package com.simulator.sim;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class LogNombres {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy_MM_dd-HH_mm_ss");
    
    public static Path singleRunPath(TipoAlgoritmo alg){
        String ts = LocalDateTime.now().format(FMT);
        return Paths.get("logs", "sim-" + alg.name() + "-" + ts + ".log");
    }
    
    public static String newRunId() {
        return "run-" + LocalDateTime.now().format(FMT);
    }
    
    public static Path compareDir(String runId){
        return Paths.get("logs", "compare", runId);
    }
    
    public static Path comparePath(String runId, TipoAlgoritmo alg){
        return compareDir(runId).resolve("sim-" + alg.name() + ".log");
    }
}