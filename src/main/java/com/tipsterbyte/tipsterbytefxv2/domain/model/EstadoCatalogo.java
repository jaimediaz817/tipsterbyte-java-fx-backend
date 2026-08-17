// ─────────────────────────────────────────────
// [QUÉ]: Estado del catálogo geográfico de países y ligas (CU-10).
// [POR QUÉ]: El frontend (SUPERADMIN) necesita saber si el catálogo ya fue poblado
//            para habilitar el botón de activación y mostrar el estado del panel.
//            El estado se deriva de los datos reales (conteos), no de una tabla extra:
//            si hay países y ligas persistidos, el catálogo está POBLADO.
// [ALTERNATIVAS]: Tabla de estado persistida; se descarta porque el estado derivado
//                 de los datos es siempre veraz y evita un estado inconsistente.
// [RELACIONES]: CU-10 → ConsultarEstadoCatalogoUseCase → CatalogoController.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

public enum EstadoCatalogo {

    VACIO,
    POBLADO
}