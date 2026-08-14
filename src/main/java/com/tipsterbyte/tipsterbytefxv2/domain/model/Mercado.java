// ─────────────────────────────────────────────
// [QUÉ]: Mercado de apuesta sobre el que se emite una cuota o un pronóstico.
//        Conjunto cerrado del negocio de apuestas.
// [POR QUÉ]: Limita los mercados soportados a los que las fuentes de odds pueden
//            entregar de forma consistente (1X2, doble oportunidad, over/under).
//            Un enum impide mercados inválidos sin decisión explícita del negocio.
// [ALTERNATIVAS]: Clase VO con catálogo en BD; se descarta en esta fase porque
//                 agrega persistencia sin necesidad (los mercados son fijos hoy).
// [RELACIONES]: Usado por SeleccionPronostico y por adapters de ProveedorCuotas.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

public enum Mercado {

    UNO_X_DOS("1X2"),
    DOBLE_OPORTUNIDAD("Doble oportunidad"),
    OVER_UNDER("Over/Under");

    private final String descripcion;

    Mercado(String descripcion) {
        this.descripcion = descripcion;
    }

    // [QUÉ]: Devuelve la descripción legible del mercado (ej: "1X2").
    public String descripcion() {
        return descripcion;
    }
}