// ─────────────────────────────────────────────
// [QUÉ]: Sleeper por defecto con Thread.sleep (producción).
// [RELACIONES]: Bean por defecto del puerto funcional Sleeper; los tests inyectan fakes.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import org.springframework.stereotype.Component;

@Component
public class SleeperReal implements Sleeper {

    @Override
    public void dormir(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Espera de cortesía interrumpida", e);
        }
    }
}
