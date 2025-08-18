package com.simulator.logging;

import java.time.Instant;
import java.util.Objects;

public final class LogMeta {
    private final Instant timestamp;
    private final LogNivel nivel;
    private final LogEvento evento;
    
    public LogMeta(Instant timestamp, LogNivel nivel, LogEvento evento){
        this.timestamp = Objects.requireNonNull(timestamp);
        this.nivel = Objects.requireNonNull(nivel);
        this.evento = Objects.requireNonNull(evento);
    }
    
    public Instant timestamp() { return timestamp; }
    public LogNivel nive() { return nivel; }
    public LogEvento evento() { return evento; }
    
}