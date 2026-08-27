// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-24 (HU-16): evalúa una estrategia contra los partidos programados
//        de las ligas configuradas, calculando score de confianza y generando sugerencias.
// [POR QUÉ]: El motor de evaluación recorre los criterios de la estrategia contra cada
//            partido, computa un score ponderado y guarda como PronosticoSugerido los
//            partidos que superan la confianza mínima.
// [ALTERNATIVAS]: Evaluar en tiempo real sin persistir; se descarta porque el tipster
//                 quiere ver sugerencias acumuladas.
// [RELACIONES]: HU-16 AC7-AC11 → EstrategiaRepository, PartidoRepository,
//               CuotaHistorialRepository, PronosticoSugeridoRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.*;
import com.tipsterbyte.tipsterbytefxv2.domain.model.*;
import com.tipsterbyte.tipsterbytefxv2.domain.service.EvaluadorCriterios;
import com.tipsterbyte.tipsterbytefxv2.domain.service.EvaluadorCriterios.SenalCriterio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public final class EvaluarEstrategiaUseCase {

    private static final Logger log = LoggerFactory.getLogger(EvaluarEstrategiaUseCase.class);

    private final EstrategiaRepository estrategiaRepository;
    private final PartidoRepository partidoRepository;
    private final CuotaHistorialRepository cuotaHistorialRepository;
    private final ZonaDescensoRepository zonaDescensoRepository;
    private final PronosticoSugeridoRepository pronosticoSugeridoRepository;

    public EvaluarEstrategiaUseCase(EstrategiaRepository estrategiaRepository,
                                    PartidoRepository partidoRepository,
                                    CuotaHistorialRepository cuotaHistorialRepository,
                                    ZonaDescensoRepository zonaDescensoRepository,
                                    PronosticoSugeridoRepository pronosticoSugeridoRepository) {
        this.estrategiaRepository = estrategiaRepository;
        this.partidoRepository = partidoRepository;
        this.cuotaHistorialRepository = cuotaHistorialRepository;
        this.zonaDescensoRepository = zonaDescensoRepository;
        this.pronosticoSugeridoRepository = pronosticoSugeridoRepository;
    }

    // [QUÉ]: Evalúa una estrategia contra todos los partidos programados de sus ligas configuradas.
    //        HU-16 AC10: genera PronosticoSugerido para partidos que superan confianzaMinima.
    public List<PronosticoSugerido> ejecutar(UUID estrategiaId) {
        Estrategia estrategia = estrategiaRepository.buscarPorId(estrategiaId)
                .orElseThrow(() -> new com.tipsterbyte.tipsterbytefxv2.domain.DomainException(
                        "Estrategia no encontrada: " + estrategiaId));

        if (!estrategia.activa()) {
            log.warn("Estrategia {} está inactiva, se omite evaluación", estrategiaId);
            return List.of();
        }

        // 1. Obtener partidos programados de las ligas configuradas.
        List<Partido> partidos = obtenerPartidosElegibles(estrategia);

        // 2. Eliminar sugerencias anteriores de esta estrategia.
        pronosticoSugeridoRepository.eliminarPorEstrategiaId(estrategiaId);

        // 3. Evaluar cada partido.
        List<PronosticoSugerido> sugerencias = new ArrayList<>();
        for (Partido partido : partidos) {
            Optional<PronosticoSugerido> sugerido = evaluarPartido(estrategia, partido);
            sugerido.ifPresent(sugerencias::add);
        }

        // 4. Persistir sugerencias en lote.
        if (!sugerencias.isEmpty()) {
            pronosticoSugeridoRepository.guardarLote(sugerencias);
        }

        log.info("Estrategia {} evaluada: {} partidos analizados, {} sugerencias generadas",
                estrategiaId, partidos.size(), sugerencias.size());
        return sugerencias;
    }

    // [QUÉ]: Evalúa un partido individual contra la estrategia.
    private Optional<PronosticoSugerido> evaluarPartido(Estrategia estrategia, Partido partido) {
        // Recopilar datos del partido para los criterios.
        Map<String, BigDecimal> cuotas = obtenerCuotas(partido.id(), estrategia.mercado());
        Map<String, Integer> posiciones = obtenerPosiciones(partido);
        Map<String, List<String>> forma = obtenerForma(partido);
        boolean enZonaDescenso = verificarZonaDescenso(partido);

        // Evaluar cada criterio.
        List<SenalCriterio> senales = new ArrayList<>();
        for (Criterio criterio : estrategia.criterios()) {
            SenalCriterio senal = EvaluadorCriterios.evaluar(criterio, cuotas, posiciones, forma, enZonaDescenso);
            senales.add(senal);
        }

        // Calcular score: Σ(peso * valor) / Σ(pesos), donde valor = 1.0 si pass, 0.0 si fail.
        BigDecimal pesoTotal = senales.stream()
                .map(s -> s.criterio().peso())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (pesoTotal.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }

        BigDecimal scoreNumerador = senales.stream()
                .map(s -> s.criterio().peso().multiply(s.pass() ? BigDecimal.ONE : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal score = scoreNumerador.divide(pesoTotal, 3, RoundingMode.HALF_UP);

        // Verificar confianza mínima.
        BigDecimal confianzaMinima = estrategia.confianzaMinima() != null
                ? estrategia.confianzaMinima()
                : BigDecimal.ZERO;

        if (score.compareTo(confianzaMinima) < 0) {
            return Optional.empty();
        }

        int cumplidos = (int) senales.stream().filter(SenalCriterio::pass).count();
        int fallidos = senales.size() - cumplidos;

        return Optional.of(new PronosticoSugerido(
                estrategia.id(), partido.id(), score, cumplidos, fallidos));
    }

    // [QUÉ]: Obtiene las cuotas más recientes del partido para el mercado de la estrategia.
    private Map<String, BigDecimal> obtenerCuotas(UUID partidoId, Mercado mercado) {
        Instant hasta = Instant.now();
        Instant desde = hasta.minus(24, ChronoUnit.HOURS);
        List<CuotaHistorial> historial = cuotaHistorialRepository.buscarPorPartidoYRango(partidoId, desde, hasta);

        return historial.stream()
                .filter(h -> h.mercado() == mercado)
                .collect(Collectors.toMap(
                        h -> h.seleccion() != null ? h.seleccion() : h.mercado().name(),
                        CuotaHistorial::valor,
                        (a, b) -> a)); // última si hay duplicados
    }

    // [QUÉ]: Obtiene posiciones del equipo en la tabla de la liga.
    private Map<String, Integer> obtenerPosiciones(Partido partido) {
        // Placeholder: en FASE 2 se conecta con PosicionTablaRepository.
        return Map.of();
    }

    // [QUÉ]: Obtiene la forma reciente del equipo (últimos 5 resultados).
    private Map<String, List<String>> obtenerForma(Partido partido) {
        // Placeholder: en FASE 2 se conecta con PosicionTablaRepository.ultimosResultados.
        return Map.of();
    }

    // [QUÉ]: Verifica si el equipo local o visitante está en zona de descenso.
    private boolean verificarZonaDescenso(Partido partido) {
        Optional<ZonaDescenso> zonaOpt = zonaDescensoRepository.buscarPorTemporadaId(partido.temporadaId());
        if (zonaOpt.isEmpty()) return false;
        // Placeholder: se necesita la posición del equipo para comparar.
        return false;
    }

    // [QUÉ]: Obtiene partidos elegibles de las ligas configuradas en la estrategia.
    private List<Partido> obtenerPartidosElegibles(Estrategia estrategia) {
        List<UUID> ligaIds = estrategia.ligaIds();
        if (ligaIds == null || ligaIds.isEmpty()) {
            // Si no hay filtro de ligas, tomar todas las ligas con partidos programados.
            // Placeholder: se necesita un método que obtenga todos los partidos PROGRAMADO.
            return List.of();
        }

        List<Partido> todos = new ArrayList<>();
        for (UUID ligaId : ligaIds) {
            todos.addAll(partidoRepository.buscarProximosPorLiga(ligaId).stream()
                    .filter(p -> p.estado() == EstadoPartido.PROGRAMADO)
                    .filter(p -> p.fechaProgramada().fechaHora().isAfter(LocalDateTime.now()))
                    .toList());
        }
        return todos;
    }
}
