// ─────────────────────────────────────────────
// [QUÉ]: Servicio de dominio que normaliza nombres de equipos para COMPARACIÓN
//        (sin tildes/diacríticos + trim + colapso de espacios + minúsculas).
// [POR QUÉ]: Las fuentes escriben los nombres distinto ("Atlético Nacional" vs
//            "Atletico Nacional", "Boca Juniors  " vs "Boca Juniors"). El matching
//            por nombre exacto (decisión FASE 8.5, fuzzy difiere a FASE 17) fallaba
//            ante esas diferencias cosméticas y creaba equipos duplicados. La
//            normalización centralizada aquí garantiza UNA sola regla de comparación
//            para todos los consumidores (CU-10 poblar, CU-01 posiciones, CU-02
//            calendario). El nombre ORIGINAL se conserva para display; solo la
//            comparación normaliza.
// [ALTERNATIVAS]: Que cada scraper Python entregue nombres ya limpios; se descarta
//                 porque depende de N implementaciones externas y pierde fidelidad
//                 de display. Comparación case-insensitive simple; se descarta porque
//                 no resuelve tildes (el caso dominante en español).
// [RELACIONES]: Usado por SincronizarCatalogoUseCase (CU-10, fuente #6),
//               SincronizarPosicionesUseCase (CU-01), SincronizarCalendarioUseCase
//               (CU-02) y CacheClaves.equipos() (clave estable país+liga).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.service;

import java.text.Normalizer;

public final class NormalizadorNombresEquipos {

    private NormalizadorNombresEquipos() {
    }

    // [QUÉ]: Devuelve el nombre normalizado para comparación; cadena vacía si es nulo.
    public static String normalizar(String nombre) {
        if (nombre == null) {
            return "";
        }
        String sinDiacriticos = Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinDiacriticos.replaceAll("\\s+", " ").toLowerCase();
    }
}
