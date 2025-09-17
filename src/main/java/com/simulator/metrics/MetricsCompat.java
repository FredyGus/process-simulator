// src/main/java/com/simulator/metrics/MetricsCompat.java
package com.simulator.metrics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class MetricsCompat {

    private MetricsCompat() {
    }

    // ------ API pública que usan los controladores ------
    public static int espera(ProcesoMetricas m) {
        return readInt(m,
                "getEspera", "espera", "getTiempoEspera", "tiempoEspera", "getWait", "wait", "waitingTime");
    }

    public static int respuesta(ProcesoMetricas m) {
        return readInt(m,
                "getRespuesta", "respuesta", "getTiempoRespuesta", "tiempoRespuesta", "getResponse", "response", "responseTime");
    }

    public static int turnaround(ProcesoMetricas m) {
        return readInt(m,
                "getTurnaround", "turnaround", "getTiempoTurnaround", "tiempoTurnaround", "getTAT", "tat");
    }

    public static int ejecucion(ProcesoMetricas m) {
        return readInt(m,
                "getEjecucion", "ejecucion", "getTiempoEjecucion", "tiempoEjecucion", "getCpu", "cpu", "cpuTime", "serviceTime");
    }

    public static int rafagaTotal(ProcesoMetricas m) {
        return readInt(m,
                "getRafagaTotal", "rafagaTotal", "getRafaga", "rafaga", "getBurst", "burst", "burstTime", "service");
    }

    // ------ Utilidad robusta: prueba métodos y, si falla, campos ------
    private static int readInt(Object bean, String... candidates) {
        Class<?> c = bean.getClass();

        // Métodos sin parámetros
        for (String name : candidates) {
            try {
                Method mt = c.getMethod(name);
                mt.setAccessible(true);
                Object v = mt.invoke(bean);
                if (v instanceof Number num) {
                    return num.intValue();
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ex) {
                throw new IllegalStateException("Error invocando método '" + name + "' en " + c.getName(), ex);
            }
        }

        // Campos públicos/privados
        for (String name : candidates) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                Object v = f.get(bean);
                if (v instanceof Number num) {
                    return num.intValue();
                }
            } catch (NoSuchFieldException ignored) {
            } catch (Exception ex) {
                throw new IllegalStateException("Error leyendo campo '" + name + "' en " + c.getName(), ex);
            }
        }

        throw new IllegalStateException("No se encontró un getter/campo compatible en " + c.getName());
    }
}
