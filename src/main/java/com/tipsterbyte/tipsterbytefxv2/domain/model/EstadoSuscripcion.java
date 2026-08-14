// ─────────────────────────────────────────────
// [QUÉ]: Estado del ciclo de vida del aggregate Suscripcion.
// [POR QUÉ]: Conjunto cerrado del negocio. Una suscripción ACTIVA expira al
//            llegar fechaFin (regla del aggregate); CANCELADA la termina el cliente.
// [ALTERNATIVAS]: String libre; se descarta por permitir estados inválidos.
// [RELACIONES]: Usado por el aggregate Suscripcion (CU-09). Con BR-006 (consumo).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

public enum EstadoSuscripcion {
    ACTIVA,
    CANCELADA,
    EXPIRADA
}