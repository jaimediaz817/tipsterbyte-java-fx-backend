// ─────────────────────────────────────────────
// [QUÉ]: Request DTO de CU-04 (activar liga): las URLs reales de las fuentes de
//        extracción (posiciones, calendario, cuotas) que el usuario suministra.
// [POR QUÉ]: Traduce la request HTTP en el comando ActivarLigaComando que CU-04
//            necesita para crear los DetalleFuenteExtraccion y activar (BR-001 exige
//            fuentes operativas). La validación estructural (Bean Validation) ocurre
//            en esta capa; la regla de negocio en el dominio.
// [ALTERNATIVAS]: Recibir booleanos de disponibilidad (como antes); se descartó porque
//                 el usuario decidió que las URLs viajen dentro de CU-04 y la
//                 disponibilidad se derive de su presencia.
// [RELACIONES]: CU-04 → ActivarLigaComando (application.dto) → Liga.activar().
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request;

public record ActivarLigaRequest(
        String urlPosiciones,
        String urlCalendario,
        String urlCuotas) {
}
