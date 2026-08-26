// ─────────────────────────────────────────────
// [QUÉ]: Servicio de cortesía con el scraper Python (H-06): envuelve llamadas a las
//        fuentes de poblamiento con pausa (throttle) y reintento con backoff exponencial.
// [POR QUÉ]: El poblamiento dispara cientos de llamadas consecutivas; sin cortesía el
//            scraper martillea Soccerway/Flashscore/Wplay y arriesga baneos, y un fallo
//            transitorio de red pierde ese país/liga para toda la corrida.
//            Pausa antes de la primera llamada + backoff creciente entre reintentos.
//            Si está deshabilitado (app.fuentes.cortesia.enabled=false) es passthrough
//            puro: cero coste, cero sleeps.
// [ALTERNATIVAS]: Resilience4j (RateLimiter/Retry); se descarta por añadir dependencia
//                 para una necesidad sencilla. Circuit breaker; se difiere a FASE 16
//                 (decisión del usuario — ver hallazgos-arquitectura.md H-06).
// [RELACIONES]: Usado por los decoradores Proveedor*ConCortesia (#1/#5/#6). Inyectable
//               Sleeper para tests deterministas.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ServicioCortesia {

    private final boolean enabled;
    private final long pausaMs;
    private final int reintentos;
    private final long backoffMs;
    private final Sleeper sleeper;

    // [QUÉ]: Instancia sin cortesía (para tests unitarios de adapters).
    public static ServicioCortesia passthrough() {
        return new ServicioCortesia(m -> { }, false, 0, 0, 0);
    }

    public ServicioCortesia(Sleeper sleeper,
                            @Value("${app.fuentes.cortesia.enabled:true}") boolean enabled,
                            @Value("${app.fuentes.cortesia.pausa-ms:250}") long pausaMs,
                            @Value("${app.fuentes.cortesia.reintentos:2}") int reintentos,
                            @Value("${app.fuentes.cortesia.backoff-ms:1500}") long backoffMs) {
        this.sleeper = sleeper;
        this.enabled = enabled;
        this.pausaMs = pausaMs;
        this.reintentos = Math.max(0, reintentos);
        this.backoffMs = backoffMs;
    }

    // [QUÉ]: Ejecuta la llamada al scraper con cortesía y devuelve su resultado.
    // [POR QUÉ]: Secuencia: [pausa] intento 1 → si falla y quedan reintentos:
    //            [backoff x2^n] intento n+1... Agotados los reintentos, relanza el último
    //            error (CU-10 ya tolera fallos por liga/país).
    public <T> T ejecutar(Supplier<T> llamada) {
        if (!enabled) {
            return llamada.get();
        }
        RuntimeException ultimoError = null;
        int intentosTotales = reintentos + 1;
        for (int intento = 0; intento < intentosTotales; intento++) {
            dormir(pausaMs);
            try {
                long inicio = System.currentTimeMillis();
                System.out.println("[CORTESIA] >>> intento " + (intento + 1) + "/" + intentosTotales + " ENVIADO a la fuente (thread=" + Thread.currentThread() + ")");
                T resultado = llamada.get();
                System.out.println("[CORTESIA] <<< intento " + (intento + 1) + "/" + intentosTotales + " RESPONDIO OK en " + (System.currentTimeMillis() - inicio) + " ms");
                return resultado;
            } catch (RuntimeException ex) {
                ultimoError = ex;
                System.out.println("[CORTESIA] !!! intento " + (intento + 1) + "/" + intentosTotales + " FALLO: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                if (intento < reintentos) {
                    dormir(backoffMs * (1L << intento)); // backoff exponencial: 1x, 2x, 4x...
                }
            }
        }
        throw ultimoError;
    }

    private void dormir(long millis) {
        if (millis > 0) {
            sleeper.dormir(millis);
        }
    }
}
