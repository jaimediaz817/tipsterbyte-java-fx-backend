// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-03 (HU-03/HU-14): sincroniza las cuotas de los partidos próximos
//        de una liga desde Wplay, con resolución multi-fuente de equipos (AC4.2),
//        creación de partidos faltantes (AC4.3) y escritura append-only al historial
//        de cuotas (AC4.5).
// [POR QUÉ]: Orquesta la actualización de cuotas por partido, delegando la validación
//            de BR-007 (cuota > 1.0) al VO Cuota. Emite CuotaActualizada por partido.
//            Desde FASE 12 invalida el cache de cuotas por partido antes de consultar
//            la fuente (cache-aside con Redis). HU-14 AC4.2/4.3/4.5 extienden el flujo
//            para resolver nombres de equipo de Wplay contra la plantilla y crear
//            partidos faltantes cuando ambos equipos casan.
// [ALTERNATIVAS]: Obtener cuotas de todos los partidos históricos; se descarta porque
//                 solo los próximos (PROGRAMADO/EN_VIVO) requieren odds vigentes.
// [RELACIONES]: HU-03 → CU-03 → ProveedorCuotas + PartidoRepository + CacheLecturas;
//               HU-14 AC4.2 → ResolutorEquipoExtraccion + EquiposAliasRepository;
//               HU-14 AC4.3 → PartidoRepository (buscarPorTemporadaYEquipos);
//               HU-14 AC4.5 → CuotaHistorialRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.CuotaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.dto.PartidoWplay;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheClaves;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.CuotaHistorialRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.EquiposAliasRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCuotas;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPartidosProximosWplay;
import com.tipsterbyte.tipsterbytefxv2.application.port.TemporadaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.CuotaHistorial;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EquiposAlias;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FechaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.service.ResolutorEquipoExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.service.ResultadoMatchEquipo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SincronizarCuotasUseCase {

    private static final Logger log = LoggerFactory.getLogger(SincronizarCuotasUseCase.class);

    private final PartidoRepository partidoRepository;
    private final ProveedorCuotas proveedorCuotas;
    private final CacheLecturas cacheLecturas;
    private final ProveedorPartidosProximosWplay proveedorPartidosWplay;
    private final TemporadaRepository temporadaRepository;
    private final EquiposAliasRepository equiposAliasRepository;
    private final CuotaHistorialRepository cuotaHistorialRepository;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public SincronizarCuotasUseCase(PartidoRepository partidoRepository,
                                    ProveedorCuotas proveedorCuotas,
                                    CacheLecturas cacheLecturas,
                                    ProveedorPartidosProximosWplay proveedorPartidosWplay,
                                    TemporadaRepository temporadaRepository,
                                    EquiposAliasRepository equiposAliasRepository,
                                    CuotaHistorialRepository cuotaHistorialRepository) {
        this.partidoRepository = partidoRepository;
        this.proveedorCuotas = proveedorCuotas;
        this.cacheLecturas = cacheLecturas;
        this.proveedorPartidosWplay = proveedorPartidosWplay;
        this.temporadaRepository = temporadaRepository;
        this.equiposAliasRepository = equiposAliasRepository;
        this.cuotaHistorialRepository = cuotaHistorialRepository;
    }

    // [QUÉ]: Ejecuta CU-03: para cada partido próximo de la liga, consulta cuotas, las
    //        mapea a Cuota y actualiza el partido. Devuelve los eventos CuotaActualizada.
    public List<DomainEvent> ejecutar(UUID ligaId) {
        List<Partido> partidos = partidoRepository.buscarProximosPorLiga(ligaId);

        List<DomainEvent> eventos = new ArrayList<>();
        for (Partido partido : partidos) {
            cacheLecturas.eliminar(CacheClaves.cuotas(partido.id()));
            List<CuotaFuente> fuentes = proveedorCuotas.obtenerCuotas(partido.id());
            List<Cuota> cuotas = fuentes.stream()
                    .map(f -> new Cuota(f.mercado(), f.valor())) // BR-007 validado en el VO
                    .toList();
            partido.actualizarCuotas(cuotas);
            partidoRepository.guardar(partido);
            eventos.addAll(partido.pullEventos()); // CuotaActualizada
        }
        return eventos;
    }

    // [QUÉ]: Ejecuta CU-03 con resolución multi-fuente (HU-14 AC4.2/4.3/4.5):
    //        1. Obtiene partidos próximos de Wplay para la temporada activa de la liga.
    //        2. Para cada partido Wplay, resuelve los nombres de equipo contra la plantilla.
    //        3. Si ambos equipos casan, busca/crea el partido y actualiza cuotas.
    //        4. Registra cada cuota en el historial append-only.
    public List<DomainEvent> ejecutarConResolucion(UUID ligaId) {
        Temporada temporada = temporadaRepository.buscarActivaPorLigaId(ligaId)
                .orElseThrow(() -> new com.tipsterbyte.tipsterbytefxv2.domain.DomainException(
                        "Liga sin temporada activa: " + ligaId));

        List<PartidoWplay> partidosWplay = proveedorPartidosWplay.obtenerPartidosProximos(temporada.id());
        if (partidosWplay.isEmpty()) {
            log.info("[CU-03] No hay partidos próximos en Wplay para liga {}", ligaId);
            return List.of();
        }

        List<EquiposAlias> aliases = equiposAliasRepository.buscarPorTemporadaYFuente(
                temporada.id(), TipoFuenteExtraccion.ODDS_WPLAY);
        List<String> nombresAliases = aliases.stream().map(EquiposAlias::nombreExterno).toList();

        List<DomainEvent> eventos = new ArrayList<>();
        int procesados = 0;

        for (PartidoWplay wplay : partidosWplay) {
            ResultadoMatchEquipo matchLocal = ResolutorEquipoExtraccion.resolver(
                    wplay.teamLocal(), temporada.equipos(), nombresAliases);
            ResultadoMatchEquipo matchVisitante = ResolutorEquipoExtraccion.resolver(
                    wplay.teamVisitante(), temporada.equipos(), nombresAliases);

            if (matchLocal instanceof ResultadoMatchEquipo.SinMatch sin) {
                log.warn("[CU-03 AC4.2] Equipo local sin match: '{}' → partido omitido", sin.nombreExterno());
                continue;
            }
            if (matchVisitante instanceof ResultadoMatchEquipo.SinMatch sin) {
                log.warn("[CU-03 AC4.2] Equipo visitante sin match: '{}' → partido omitido", sin.nombreExterno());
                continue;
            }
            if (matchLocal instanceof ResultadoMatchEquipo.Ambiguo amb) {
                log.warn("[CU-03 AC4.2] Equipo local ambiguo: '{}' → {} candidatos → partido omitido",
                        amb.nombreExterno(), amb.candidatos().size());
                continue;
            }
            if (matchVisitante instanceof ResultadoMatchEquipo.Ambiguo amb) {
                log.warn("[CU-03 AC4.2] Equipo visitante ambiguo: '{}' → {} candidatos → partido omitido",
                        amb.nombreExterno(), amb.candidatos().size());
                continue;
            }

            Equipo equipoLocal = ((ResultadoMatchEquipo.Casado) matchLocal).equipo();
            Equipo equipoVisitante = ((ResultadoMatchEquipo.Casado) matchVisitante).equipo();

            // [POR QUÉ]: AC4.3 — buscar partido existente o crear uno nuevo.
            Partido partido = partidoRepository.buscarPorTemporadaYEquipos(
                    temporada.id(), equipoLocal.id(), equipoVisitante.id()).orElse(null);

            if (partido == null) {
                LocalDateTime fechaPartido = wplay.fechaPartido()
                        .atZone(ZoneId.of("America/Bogota")).toLocalDateTime();
                FechaProgramada fecha = new FechaProgramada(fechaPartido);
                partido = new Partido(temporada.id(), equipoLocal, equipoVisitante, fecha);
                log.info("[CU-03 AC4.3] Partido creado: {} vs {} ({})",
                        equipoLocal.nombre(), equipoVisitante.nombre(), fechaPartido.toLocalDate());
            }

            cacheLecturas.eliminar(CacheClaves.cuotas(partido.id()));

            List<Cuota> cuotas = new ArrayList<>();
            cuotas.add(new Cuota(Mercado.UNO_X_DOS, wplay.cuotaLocal()));
            cuotas.add(new Cuota(Mercado.UNO_X_DOS, wplay.cuotaEmpate()));
            cuotas.add(new Cuota(Mercado.UNO_X_DOS, wplay.cuotaVisitante()));
            if (wplay.dobleOportunidad() != null) {
                for (PartidoWplay.CuotaDobleOportunidad dc : wplay.dobleOportunidad()) {
                    cuotas.add(new Cuota(Mercado.DOBLE_OPORTUNIDAD, dc.valor()));
                }
            }

            partido.actualizarCuotas(cuotas);
            partidoRepository.guardar(partido);

            // [POR QUÉ]: AC4.5 — escritura append-only al historial (incondicional).
            List<CuotaHistorial> historial = new ArrayList<>();
            historial.add(new CuotaHistorial(partido.id(), Mercado.UNO_X_DOS,
                    equipoLocal.nombre(), wplay.cuotaLocal(), "ODDS_WPLAY"));
            historial.add(new CuotaHistorial(partido.id(), Mercado.UNO_X_DOS,
                    "Empate", wplay.cuotaEmpate(), "ODDS_WPLAY"));
            historial.add(new CuotaHistorial(partido.id(), Mercado.UNO_X_DOS,
                    equipoVisitante.nombre(), wplay.cuotaVisitante(), "ODDS_WPLAY"));
            if (wplay.dobleOportunidad() != null) {
                for (PartidoWplay.CuotaDobleOportunidad dc : wplay.dobleOportunidad()) {
                    historial.add(new CuotaHistorial(partido.id(), Mercado.DOBLE_OPORTUNIDAD,
                            dc.nombre(), dc.valor(), "ODDS_WPLAY"));
                }
            }
            cuotaHistorialRepository.guardarLote(historial);

            eventos.addAll(partido.pullEventos());
            procesados++;
        }

        log.info("[CU-03] Sincronización completada: {}/{} partidos procesados para liga {}",
                procesados, partidosWplay.size(), ligaId);
        return eventos;
    }
}
