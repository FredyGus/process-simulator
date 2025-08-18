package com.simulator.logging;

import com.simulator.logging.rotate.PoliticaRotacion;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class LogConfig {
    public final Path rutaBase;                 // p.ej Paths.get("simulator.log")
    public final Charset charset;               // UJTF-8
    public final boolean append;                // true
    public final LogNivel nivelMinimo;          // INFO
    public final PoliticaRotacion politicaRotacion;
    
    public LogConfig(Path rutaBase, Charset charset, boolean append, LogNivel nivelMinimo, PoliticaRotacion politicaRotacion){
        this.rutaBase = rutaBase;
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
        this.append = append;
        this.nivelMinimo = nivelMinimo == null ? LogNivel.INFO : nivelMinimo;
        this.politicaRotacion = politicaRotacion;
    }
    
    public static LogConfig basica(Path rutaBase, PoliticaRotacion rot){
        return new LogConfig(rutaBase, StandardCharsets.UTF_8, true, LogNivel.INFO, rot);
    }
}