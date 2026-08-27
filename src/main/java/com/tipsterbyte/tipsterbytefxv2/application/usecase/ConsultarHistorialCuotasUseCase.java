// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-22 (HU-15): devuelve el historial cronológico de cuotas de un
//        partido específico, agrupado por mercado/selección, para gráfico/drill-down.
// [POR QUÉ]: El frontend expande un partido para ver cómo se movieron las cuotas
//            hora a hora. La serie se agrupa por mercado para alimentar un gráfico
//            de líneas (una línea por selección: local, empate, visitante).
// [ALTERNATIVAS]: Devolver la lista plana; se descarta porque el frontend necesita
//                 agrupar por mercado para renderizar líneas separadas.
// [RELACIONES]: HU-15 AC2 → CuotaHistorialRepository.buscarPorPartidoYRango.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.CuotaHistorialRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.CuotaHistorial;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.HistorialCuotaResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ConsultarHistorialCuotasUseCase {

    private final CuotaHistorialRepository cuotaHistorialRepository;

    public ConsultarHistorialCuotasUseCase(CuotaHistorialRepository cuotaHistorialRepository) {
        this.cuotaHistorialRepository = cuotaHistorialRepository;
    }

    // [QUÉ]: Devuelve el historial de cuotas de un partido agrupado por mercado/selección.
    //        HU-15 AC2: filtros opcionales de horas y mercado.
    public List<HistorialCuotaResponse> ejecutar(UUID partidoId, int horas, String mercadoFiltro) {
        Instant hasta = Instant.now();
        Instant desde = hasta.minus(horas, ChronoUnit.HOURS);

        List<CuotaHistorial> historial = cuotaHistorialRepository.buscarPorPartidoYRango(partidoId, desde, hasta);

        if (historial.isEmpty()) {
            return List.of();
        }

        // Agrupar por (mercado, selección).
        Map<String, List<CuotaHistorial>> agrupado = historial.stream()
                .filter(h -> mercadoFiltro == null || h.mercado().name().equals(mercadoFiltro))
                .collect(Collectors.groupingBy(
                        h -> h.mercado().name() + "|" + nullSafe(h.seleccion()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<HistorialCuotaResponse> resultado = new ArrayList<>();
        for (Map.Entry<String, List<CuotaHistorial>> entry : agrupado.entrySet()) {
            String[] partes = entry.getKey().split("\\|", 2);
            String mercado = partes[0];
            String seleccion = partes[1].isEmpty() ? null : partes[1];

            List<HistorialCuotaResponse.Captura> capturas = entry.getValue().stream()
                    .sorted(Comparator.comparing(CuotaHistorial::capturadaEn))
                    .map(h -> new HistorialCuotaResponse.Captura(h.valor(), h.capturadaEn()))
                    .toList();

            resultado.add(new HistorialCuotaResponse(mercado, seleccion, capturas));
        }
        return resultado;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
