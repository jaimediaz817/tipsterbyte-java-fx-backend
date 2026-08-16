// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-02 (HU-02): sincroniza el calendario de partidos (jugados y
//        pendientes) de una liga activa desde la fuente externa.
// [POR QUÉ]: Orquesta la creación de Partido por cada partido de la fuente, resolviendo
//            los Equipo por nombre y propagando la jornada (fuente #4), y recolecta los
//            eventos PartidoProgramado. Desde FASE 12 invalida el cache de calendario de
//            la liga antes de consultar la fuente (cache-aside con Redis).
// [ALTERNATIVAS]: Que el ProveedorCalendario devuelva List<Partido>; se descarta porque
//                 el adapter no conoce los ids de los Equipo del dominio.
// [RELACIONES]: HU-02 → CU-02 → ProveedorCalendario + PartidoRepository + CacheLecturas.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PartidoFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheClaves;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCalendario;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FechaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SincronizarCalendarioUseCase {

    private final LigaRepository ligaRepository;
    private final PartidoRepository partidoRepository;
    private final ProveedorCalendario proveedorCalendario;
    private final CacheLecturas cacheLecturas;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public SincronizarCalendarioUseCase(LigaRepository ligaRepository,
                                        PartidoRepository partidoRepository,
                                        ProveedorCalendario proveedorCalendario,
                                        CacheLecturas cacheLecturas) {
        this.ligaRepository = ligaRepository;
        this.partidoRepository = partidoRepository;
        this.proveedorCalendario = proveedorCalendario;
        this.cacheLecturas = cacheLecturas;
    }

    // [QUÉ]: Ejecuta CU-02: obtiene la liga activa, consulta el calendario, crea los
    //        partidos y los persiste. Devuelve los eventos PartidoProgramado emitidos.
    public List<DomainEvent> ejecutar(UUID ligaId) {
        cacheLecturas.eliminar(CacheClaves.calendario(ligaId));
        Liga liga = ligaRepository.buscarPorId(ligaId)
                .orElseThrow(() -> new DomainException("Liga no encontrada: " + ligaId));
        if (liga.estado() != EstadoLiga.ACTIVA) {
            throw new DomainException("No se puede sincronizar calendario de una liga inactiva");
        }

        List<PartidoFuente> fuentes = proveedorCalendario.obtenerCalendario(ligaId);

        List<DomainEvent> eventos = new ArrayList<>();
        for (PartidoFuente fuente : fuentes) {
            Equipo local = resolverEquipo(liga, fuente.equipoLocalNombre());
            Equipo visitante = resolverEquipo(liga, fuente.equipoVisitanteNombre());
            Partido partido = new Partido(ligaId, local, visitante,
                    new FechaProgramada(fuente.fechaHora()), fuente.jornada());
            partidoRepository.guardar(partido);
            eventos.addAll(partido.pullEventos()); // PartidoProgramado
        }
        return eventos;
    }

    // [QUÉ]: Resuelve el Equipo de la liga por nombre; si no existe, lo crea y lo agrega.
    private Equipo resolverEquipo(Liga liga, String nombre) {
        return liga.equipos().stream()
                .filter(e -> e.nombre().equals(nombre))
                .findFirst()
                .orElseGet(() -> {
                    Equipo nuevo = new Equipo(nombre);
                    liga.agregarEquipo(nuevo);
                    return nuevo;
                });
    }
}