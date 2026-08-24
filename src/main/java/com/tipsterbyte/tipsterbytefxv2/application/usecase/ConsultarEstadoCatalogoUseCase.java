// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso que consulta el estado del catálogo geográfico (CU-10): deriva
//        EstadoCatalogo a partir de los conteos reales de países y ligas persistidos.
// [POR QUÉ]: El frontend (SUPERADMIN) necesita saber si el catálogo ya está poblado
//            para habilitar el botón de activación y mostrar el estado del panel.
//            Derivar de los datos garantiza que el estado siempre refleja la verdad
//            persistida (sin una tabla de estado que pueda quedar inconsistente).
// [ALTERNATIVAS]: Persistir el estado en una tabla; se descarta porque el conteo es
//                 la fuente de verdad y evita duplicar información de estado.
// [RELACIONES]: CU-10 → PaisRepository + LigaRepository → CatalogoController
//               (GET /api/v1/catalogo/estado).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.CatalogoEstadoDto;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoCatalogo;

public final class ConsultarEstadoCatalogoUseCase {

    private final PaisRepository paisRepository;
    private final LigaRepository ligaRepository;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public ConsultarEstadoCatalogoUseCase(PaisRepository paisRepository,
                                          LigaRepository ligaRepository) {
        this.paisRepository = paisRepository;
        this.ligaRepository = ligaRepository;
    }

    // [QUÉ]: Devuelve el estado del catálogo derivado de los conteos reales.
    // [POR QUÉ]: Tres estados posibles: VACIO (sin países), PARCIAL (países sin ligas), POBLADO (países y ligas).
    public CatalogoEstadoDto ejecutar() {
        long totalPaises = paisRepository.contar();
        long totalLigas = ligaRepository.contar();
        EstadoCatalogo estado;
        if (totalPaises == 0) {
            estado = EstadoCatalogo.VACIO;
        } else if (totalLigas == 0) {
            estado = EstadoCatalogo.PARCIAL;
        } else {
            estado = EstadoCatalogo.POBLADO;
        }
        return new CatalogoEstadoDto(estado, (int) totalPaises, (int) totalLigas);
    }
}
