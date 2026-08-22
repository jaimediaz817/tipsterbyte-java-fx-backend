// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-04 (HU-04): activa una liga asociando las URLs reales de
//        sus fuentes de extracción y verificando que las tres estén operativas.
// [POR QUÉ]: Aplica BR-001 delegando en el aggregate Liga. El caso de uso crea los
//            DetalleFuenteExtraccion (temporada ↔ fuente ↔ URL) para cada URL
//            suministrada por el usuario en un solo paso, de modo que los adapters de
//            sincronización tengan dónde resolver la URL de cada endpoint.
//            La asociación es por TEMPORADA (Bridge Fix Torneos/Temporadas): se usa la
//            temporada activa de la liga o, si no hay ninguna activa, la primera
//            registrada (catálogo recién poblado está PLANIFICADA).
// [ALTERNATIVAS]: Preguntar la disponibilidad al proveedor; se descarta porque la
//                 disponibilidad se deriva de las URLs reales aportadas (decisión FASE 8.5).
//                 Asociar por liga genérica; se descarta porque con múltiples temporadas
//                 por liga las URLs son específicas de una temporada.
// [RELACIONES]: HU-04 → CU-04 → LigaRepository + FuenteExtraccionRepository +
//               DetalleFuenteExtraccionRepository + TemporadaRepository +
//               DTO ActivarLigaComando.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.ActivarLigaComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.FuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TemporadaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;

import java.util.List;
import java.util.UUID;

public final class ActivarLigaUseCase {

    private final LigaRepository ligaRepository;
    private final FuenteExtraccionRepository fuenteRepository;
    private final DetalleFuenteExtraccionRepository detalleRepository;
    private final TemporadaRepository temporadaRepository;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public ActivarLigaUseCase(LigaRepository ligaRepository,
                              FuenteExtraccionRepository fuenteRepository,
                              DetalleFuenteExtraccionRepository detalleRepository,
                              TemporadaRepository temporadaRepository) {
        this.ligaRepository = ligaRepository;
        this.fuenteRepository = fuenteRepository;
        this.detalleRepository = detalleRepository;
        this.temporadaRepository = temporadaRepository;
    }

    // [QUÉ]: Ejecuta CU-04: asocia cada URL suministrada a la temporada vigente de la
    //        liga como DetalleFuenteExtraccion, deriva la disponibilidad y activa (BR-001).
    public List<DomainEvent> ejecutar(UUID ligaId, ActivarLigaComando comando) {
        Liga liga = ligaRepository.buscarPorId(ligaId)
                .orElseThrow(() -> new DomainException("Liga no encontrada: " + ligaId));
        UUID temporadaId = resolverTemporadaVigente(ligaId).id();

        boolean posiciones = asociarSiUrlPresente(temporadaId, TipoFuenteExtraccion.STANDINGS, comando.urlPosiciones());
        boolean calendario = asociarSiUrlPresente(temporadaId, TipoFuenteExtraccion.CALENDAR, comando.urlCalendario());
        boolean cuotas = asociarSiUrlPresente(temporadaId, TipoFuenteExtraccion.ODDS_WPLAY, comando.urlCuotas());

        liga.activar(posiciones, calendario, cuotas);
        ligaRepository.guardar(liga);
        return liga.pullEventos();
    }

    // [QUÉ]: Resuelve la temporada a la que aplican las URLs: la ACTIVA o, en su defecto,
    //        la primera registrada (liga recién poblada por CU-10 está PLANIFICADA).
    private Temporada resolverTemporadaVigente(UUID ligaId) {
        return temporadaRepository.buscarActivaPorLigaId(ligaId)
                .or(() -> temporadaRepository.buscarPorLigaId(ligaId).stream().findFirst())
                .orElseThrow(() -> new DomainException(
                        "La liga no tiene temporadas registradas: " + ligaId));
    }

    // [QUÉ]: Si la URL viene presente, asocia (o actualiza) el DetalleFuenteExtraccion
    //        de esa fuente para la temporada y devuelve true (fuente disponible).
    // [POR QUÉ]: La disponibilidad de una fuente para BR-001 es "tiene URL asociada".
    private boolean asociarSiUrlPresente(UUID temporadaId, TipoFuenteExtraccion tipo, String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        FuenteExtraccion fuente = fuenteRepository.buscarPorTipo(tipo)
                .orElseThrow(() -> new DomainException("No existe fuente registrada para el tipo: " + tipo));
        detalleRepository.buscarPorTemporadaYTipo(temporadaId, tipo)
                .ifPresentOrElse(
                        detalle -> detalleRepository.guardar(new DetalleFuenteExtraccion(
                                detalle.id(), temporadaId, detalle.fuente(), url, true)),
                        () -> detalleRepository.guardar(new DetalleFuenteExtraccion(temporadaId, fuente, url, true)));
        return true;
    }
}
