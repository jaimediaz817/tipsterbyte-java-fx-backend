// ─────────────────────────────────────────────
// [QUÉ]: Comando de entrada para CU-11 (gestionar fuentes): registrar una fuente
//        de extracción en el catálogo (nombre, tipo y activa).
// [POR QUÉ]: Encapsula los datos de entrada del caso de uso en un solo objeto,
//            desacoplado de la forma de la request HTTP.
// [ALTERNATIVAS]: Método con 3 parámetros; se descarta porque un comando nombra cada
//                 dato y simplifica la firma y los tests.
// [RELACIONES]: CU-11 → FuenteExtraccion (constructor) y TipoFuenteExtraccion.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;

public record RegistrarFuenteComando(
        String nombre,
        TipoFuenteExtraccion tipo,
        boolean activa) {
}
