// ─────────────────────────────────────────────
// [QUÉ]: Comando de entrada para CU-04 (activar liga): disponibilidad de las tres
//        fuentes de datos (posiciones, calendario, cuotas).
// [POR QUÉ]: BR-001 exige que las fuentes estén operativas para activar la liga.
//            Este DTO traslada esa información desde la capa de interfaces (que
//            consulta la configuración de infraestructura) hasta el caso de uso.
// [ALTERNATIVAS]: Pasar tres booleanos sueltos; se descarta porque un record nombra
//                 cada fuente y hace el comando auto-explicativo.
// [RELACIONES]: CU-04 → Liga.activar(boolean, boolean, boolean).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

public record DisponibilidadFuentes(
        boolean posiciones,
        boolean calendario,
        boolean cuotas) {
}