// ─────────────────────────────────────────────
// [QUÉ]: Comando de CU-14 para registrar/reemplazar un país de interés.
// [POR QUÉ]: Transporta el dato desde las interfaces (iso_alpha2 + nombre + maxLigasPorPais opcional);
//            la prioridad se deriva en el caso de uso (siguiente libre o posición
//            en la lista), el cliente no la calcula.
// [ALTERNATIVAS]: Incluir prioridad en el comando; se descarta porque expondría a la
//                 UI una responsabilidad de ordenación que es del backend.
// [RELACIONES]: CU-14 → GestionarPaisesInteresUseCase → PaisInteres.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

public record RegistrarPaisInteresComando(
        String isoAlpha2,
        String nombre,
        Integer maxLigasPorPais) {
}