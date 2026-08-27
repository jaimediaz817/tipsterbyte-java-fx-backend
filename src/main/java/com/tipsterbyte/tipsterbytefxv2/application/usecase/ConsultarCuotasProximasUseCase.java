// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-21 (HU-15): consulta los partidos próximos de una liga con sus
//        cuotas más recientes y un indicador de volatilidad calculado server-side.
// [POR QUÉ]: El frontend necesita una vista resumida para que el tipster decida rápido
//            qué partidos analizar, sin revisar históricos manualmente. La volatilidad
//            se calcula aquí (no en el frontend) para que los umbrales sean
//            configurables desde properties.
// [ALTERNATIVAS]: Consultar directamente cuota_historial por cada partido; se descarta
//                 porque genera N+1 queries. Se prefiere un batch por liga.
// [RELACIONES]: HU-15 AC1 → PartidoRepository + CuotaHistorialRepository;
//               VolatilidadCuota (dominio) calcula la señal.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.CuotaHistorialRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.CuotaHistorial;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.VolatilidadCuota;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.CuotaProximaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ConsultarCuotasProximasUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConsultarCuotasProximasUseCase.class);

    private final PartidoRepository partidoRepository;
    private final CuotaHistorialRepository cuotaHistorialRepository;

    public ConsultarCuotasProximasUseCase(PartidoRepository partidoRepository,
                                           CuotaHistorialRepository cuotaHistorialRepository) {
        this.partidoRepository = partidoRepository;
        this.cuotaHistorialRepository = cuotaHistorialRepository;
    }

    // [QUÉ]: Devuelve el snapshot de cuotas próximas con volatilidad para una liga.
    //        HU-15 AC1: partidos PROGRAMADO con fecha futura, cuota más reciente
    //        por mercado/selección, volatilidad calculada contra baseline.
    public List<CuotaProximaResponse> ejecutar(UUID ligaId, int ventanaHoras) {
        List<Partido> partidos = partidoRepository.buscarProximosPorLiga(ligaId).stream()
                .filter(p -> p.estado() == EstadoPartido.PROGRAMADO)
                .filter(p -> p.fechaProgramada().fechaHora().isAfter(java.time.LocalDateTime.now()))
                .toList();

        if (partidos.isEmpty()) {
            return List.of();
        }

        Instant hasta = Instant.now();
        Instant desde = hasta.minus(ventanaHoras, ChronoUnit.HOURS);
        UUID[] partidoIds = partidos.stream().map(Partido::id).toArray(UUID[]::new);

        // Batch: obtener todas las cuotas de la ventana de una sola vez (sin N+1).
        Map<UUID, List<CuotaHistorial>> cuotasPorPartido = cuotaHistorialRepository
                .buscarPorPartidosYRango(List.of(partidoIds), desde, hasta).stream()
                .collect(Collectors.groupingBy(CuotaHistorial::partidoId));

        List<CuotaProximaResponse> resultado = new ArrayList<>();
        for (Partido partido : partidos) {
            List<CuotaHistorial> historial = cuotasPorPartido.getOrDefault(partido.id(), List.of());
            resultado.add(construirResponse(partido, historial));
        }
        return resultado;
    }

    // [QUÉ]: Construye el response de un partido con sus cuotas y volatilidad.
    private CuotaProximaResponse construirResponse(Partido partido, List<CuotaHistorial> historial) {
        // Cuotas más recientes por (mercado, selección).
        Map<String, CuotaHistorial> ultimas = historial.stream()
                .collect(Collectors.toMap(
                        h -> h.mercado().name() + "|" + Optional_str(h.seleccion()),
                        h -> h,
                        (a, b) -> a.capturadaEn().isAfter(b.capturadaEn()) ? a : b));

        List<CuotaProximaResponse.CuotaMercado> cuotas = ultimas.values().stream()
                .map(h -> new CuotaProximaResponse.CuotaMercado(
                        h.mercado().name(),
                        h.seleccion(),
                        h.valor()))
                .sorted(Comparator.comparing(CuotaProximaResponse.CuotaMercado::mercado)
                        .thenComparing(CuotaProximaResponse.CuotaMercado::seleccion, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // Volatilidad: comparar baseline (primera) vs última para el mercado UNO_X_DOS.
        VolatilidadCuota volatilidad = calcularVolatilidad(historial);

        return new CuotaProximaResponse(
                partido.id(),
                partido.equipoLocal().nombre(),
                partido.equipoVisitante().nombre(),
                partido.fechaProgramada().fechaHora().atZone(java.time.ZoneId.of("America/Bogota")).toInstant(),
                partido.jornada(),
                cuotas,
                volatilidad);
    }

    // [QUÉ]: Calcula volatilidad del mercado UNO_X_DOS (selección LOCAL) usando
    //        la primera y última captura de la ventana.
    private VolatilidadCuota calcularVolatilidad(List<CuotaHistorial> historial) {
        List<CuotaHistorial> unoxdos = historial.stream()
                .filter(h -> h.mercado() == Mercado.UNO_X_DOS)
                .sorted(Comparator.comparing(CuotaHistorial::capturadaEn))
                .toList();

        if (unoxdos.size() < 2) {
            return new VolatilidadCuota(VolatilidadCuota.ClaseVolatilidad.SIN_BASELINE, null);
        }

        BigDecimal baseline = unoxdos.getFirst().valor();
        BigDecimal ultima = unoxdos.getLast().valor();
        return VolatilidadCuota.calcular(baseline, ultima);
    }

    private static String Optional_str(String value) {
        return value == null ? "" : value;
    }
}
