// ─────────────────────────────────────────────
// [QUÉ]: Comando de entrada para CU-04 (activar liga): las URLs reales de cada
//        fuente de extracción (posiciones, calendario, cuotas) que el usuario
//        suministra al activar la liga.
// [POR QUÉ]: Encapsula la entrada de CU-04 en un solo objeto. Cada URL se asocia a
//            la liga como DetalleFuenteExtraccion y determina la disponibilidad de
//            la fuente para BR-001 (ligas inactivas no se extraen sin fuentes).
// [ALTERNATIVAS]: Pasar DisponibilidadFuentes (booleanos); se descartó porque el
//                 usuario decidió que las URLs viajen dentro de CU-04 y la
//                 disponibilidad se deriva de su presencia.
// [RELACIONES]: CU-04 → ActivarLigaUseCase + DetalleFuenteExtraccion + Liga.activar().
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

public record ActivarLigaComando(
        String urlPosiciones,
        String urlCalendario,
        String urlCuotas) {
}
