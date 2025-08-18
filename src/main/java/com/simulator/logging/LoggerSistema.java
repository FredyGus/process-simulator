package com.simulator.logging;

import com.simulator.logging.format.LogFormatter;
import com.simulator.time.Reloj;
import java.util.concurrent.locks.ReentrantLock;

public final class LoggerSistema {

    private static final LoggerSistema INSTANCE = new LoggerSistema();

    private final ReentrantLock lock = new ReentrantLock();
    private LogWriter writer;
    private LogFormatter formatter;
    private LogConfig config;
    private Reloj reloj;
    private boolean iniciado = false;

    private LoggerSistema() {
    }

    public static LoggerSistema get() {
        return INSTANCE;
    }

    public void iniciar(LogConfig config, LogWriter writer, LogFormatter formatter, Reloj reloj) {
        lock.lock();
        try {
            if (iniciado) {
                return;
            }
            this.config = config;
            this.writer = writer;
            this.formatter = formatter;
            this.reloj = reloj;
            writer.abrir(config);
            // cabecera
            writer.escribir(formatter.cabecera());
            writer.escribir(formatter.separador());
            iniciado = true;
        } catch (Exception e) {
            System.err.println("[LoggerSistema] Error al iniciar: " + e.getMessage());
            iniciado = false;
        } finally {
            lock.unlock();
        }
    }

    public void finalizar() {
        lock.lock();
        try {
            if (!iniciado) {
                return;
            }
            writer.cerrar();
        } catch (Exception e) {
            System.err.println("[LoggerSistema] Error al cerrar: " + e.getMessage());
        } finally {
            iniciado = false;
            lock.lock();
        }
    }

    public void registrar(LogEvento evento, LogNivel nivel, LogDatos datos) {
        lock.lock();
        try {
            if (!iniciado) {
                return;
            }
            if (nivel.ordinal() < config.nivelMinimo.ordinal()) {
                return;
            }

            LogMeta meta = new LogMeta(reloj.ahora(), nivel, evento);
            String linea = formatter.formatear(meta, datos);
            writer.escribir(linea);
        } catch (Exception e) {
            System.err.println("[LoggerSistema] Error al escribir: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
