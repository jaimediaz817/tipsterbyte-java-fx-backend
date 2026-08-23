// ─────────────────────────────────────────────
// [QUÉ]: Abstracción de espera para la cortesía con el scraper (H-06).
// [POR QUÉ]: Permite que los tests del decorador de cortesía verifiquen pausas y
//            backoffs de forma determinista sin dormir de verdad (inyectan un fake).
// [ALTERNATIVAS]: Thread.sleep directo en el decorador; se descarta porque haría los
//                 tests lentos e inciertos.
// [RELACIONES]: Inyectado en ServicioCortesia; implementado por el bean por defecto
//               con Thread.sleep.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

@FunctionalInterface
public interface Sleeper {

    // [QUÉ]: Duerme los milisegundos indicados (interrupción se propaga como IllegalStateException).
    void dormir(long millis);
}
