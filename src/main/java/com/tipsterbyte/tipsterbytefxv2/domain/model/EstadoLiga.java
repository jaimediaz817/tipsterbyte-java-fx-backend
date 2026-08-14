// ─────────────────────────────────────────────
// [QUÉ]: Estado del ciclo de vida del aggregate Liga.
// [POR QUÉ]: Conjunto cerrado del negocio: una liga nace en BORRADOR y solo se
//            activa cuando sus fuentes de datos están operativas (BR-001).
// [ALTERNATIVAS]: String libre; se descarta porque permitiría estados inválidos.
// [RELACIONES]: Usado por el aggregate Liga (CU-04).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

public enum EstadoLiga {
    BORRADOR,
    ACTIVA,
    INACTIVA
}