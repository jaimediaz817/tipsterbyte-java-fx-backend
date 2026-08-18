// ─────────────────────────────────────────────
// [QUÉ]: Claves canónicas del cache-aside de lecturas de fuentes externas.
// [POR QUÉ]: Centraliza el formato de clave ("posiciones:{ligaId}", etc.) para que
//            application (casos de uso que invalidan) e infrastructure (decoradores que
//            guardan/leen) compartan el mismo contrato sin strings duplicados.
// [ALTERNATIVAS]: Definir las claves en cada decorador; se descarta porque la
//                 invalidación desde los casos de uso (CU-01/02/03) necesita conocer
//                 exactamente la misma clave que el decorador.
// [RELACIONES]: Usado por los decoradores ProveedorXxxCacheable y por los casos de uso
//               de sincronización que invalidan (FASE 12).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import java.util.UUID;

public final class CacheClaves {

    private CacheClaves() {
    }

    public static String posiciones(UUID ligaId) {
        return "posiciones:" + ligaId;
    }

    public static String calendario(UUID ligaId) {
        return "calendario:" + ligaId;
    }

    public static String cuotas(UUID partidoId) {
        return "cuotas:" + partidoId;
    }

    public static String paises() {
        return "paises";
    }
}
