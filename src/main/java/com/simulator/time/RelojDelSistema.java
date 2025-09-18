package com.simulator.time;

import java.time.Instant;

public final class RelojDelSistema implements Reloj {

    @Override
    public Instant ahora() {
        return Instant.now();
    }
}
