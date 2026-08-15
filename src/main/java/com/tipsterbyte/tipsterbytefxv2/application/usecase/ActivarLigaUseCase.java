// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-04 (HU-04): activa una liga asociando las URLs reales de
//        sus fuentes de extracción y verificando que las tres estén operativas.
// [POR QUÉ]: Aplica BR-001 delegando en el aggregate Liga. El caso de uso crea los
//            DetalleFuenteExtraccion (liga ↔ fuente ↔ URL) para cada URL suministrada
//            por el usuario en un solo paso, de modo que los adapters de sincronización
//            tengan dónde resolver la URL de cada endpoint.
// [ALTERNATIVAS]: Preguntar la disponibilidad al proveedor; se descarta porque la
//                 disponibilidad se deriva de las URLs reales aportadas (decisión FASE 8.5).
// [RELACIONES]: HU-04 → CU-04 → LigaRepository + FuenteExtraccionRepository +
//               DetalleFuenteExtraccionRepository + DTO ActivarLigaComando.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.ActivarLigaComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.FuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;

import java.util.List;
import java.util.UUID;

public final class ActivarLigaUseCase {

    private final LigaRepository ligaRepository;
    private final FuenteExtraccionRepository fuenteRepository;
    private final DetalleFuenteExtraccionRepository detalleRepository;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public ActivarLigaUseCase(LigaRepository ligaRepository,
                              FuenteExtraccionRepository fuenteRepository,
                              DetalleFuenteExtraccionRepository detalleRepository) {
        this.ligaRepository = ligaRepository;
        this.fuenteRepository = fuenteRepository;
        this.detalleRepository = detalleRepository;
    }

    // [QUÉ]: Ejecuta CU-04: asocia cada URL suministrada a la liga como
    //        DetalleFuenteExtraccion, deriva la disponibilidad y activa (BR-001).
    public List<DomainEvent> ejecutar(UUID ligaId, ActivarLigaComando comando) {
        Liga liga = ligaRepository.buscarPorId(ligaId)
                .orElseThrow(() -> new DomainException("Liga no encontrada: " + ligaId));

        boolean posiciones = asociarSiUrlPresente(ligaId, TipoFuenteExtraccion.STANDINGS, comando.urlPosiciones());
        boolean calendario = asociarSiUrlPresente(ligaId, TipoFuenteExtraccion.CALENDAR, comando.urlCalendario());
        boolean cuotas = asociarSiUrlPresente(ligaId, TipoFuenteExtraccion.ODDS_WPLAY, comando.urlCuotas());

        liga.activar(posiciones, calendario, cuotas);
        ligaRepository.guardar(liga);
        return liga.pullEventos();
    }

    // [QUÉ]: Si la URL viene presente, asocia (o actualiza) el DetalleFuenteExtraccion
    //        de esa fuente para la liga y devuelve true (fuente disponible).
    // [POR QUÉ]: La disponibilidad de una fuente para BR-001 es "tiene URL asociada".
    private boolean asociarSiUrlPresente(UUID ligaId, TipoFuenteExtraccion tipo, String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        FuenteExtraccion fuente = fuenteRepository.buscarPorTipo(tipo)
                .orElseThrow(() -> new DomainException("No existe fuente registrada para el tipo: " + tipo));
        detalleRepository.buscarPorLigaYTipo(ligaId, tipo)
                .ifPresentOrElse(
                        detalle -> detalleRepository.guardar(new DetalleFuenteExtraccion(
                                detalle.id(), ligaId, detalle.fuente(), url, true)),
                        () -> detalleRepository.guardar(new DetalleFuenteExtraccion(ligaId, fuente, url, true)));
        return true;
    }
}
