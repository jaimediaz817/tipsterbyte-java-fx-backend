// ─────────────────────────────────────────────
// [QUÉ]: Estado del ciclo de vida del aggregate Pronostico.
// [POR QUÉ]: Conjunto cerrado del negocio: un pronóstico nace en BORRADOR (no
//            visible para clientes) y se publica cuando la cuota es vigente
//            (BR-004). Publicado no se edita; solo se anula (BR-005).
// [ALTERNATIVAS]: String libre; se descarta por permitir estados inconsistentes.
// [RELACIONES]: Usado por el aggregate Pronostico (CU-06, CU-07).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

public enum EstadoPronostico {
    BORRADOR,
    PUBLICADO,
    ANULADO
}