// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-01 (HU-01): sincroniza la tabla de posiciones de una liga
//        activa desde la fuente externa y la persiste.
// [POR QUÉ]: Orquesta la regla de negocio BR-002 (no extraer de ligas inactivas)
//            delegando en el aggregate Liga, sin conocer el formato de la API. Desde
//            FASE 12 invalida el cache de posiciones de la liga antes de consultar la
//            fuente, de modo que la sincronización siempre traiga datos frescos y
//            re-pueble Redis (cache-aside).
// [ALTERNATIVAS]: Lógica de mapeo dentro del adapter; se descarta porque la resolución
//                 de Equipo por nombre pertenece a la orquestación de aplicación.
// [RELACIONES]: HU-01 → CU-01 → ProveedorPosiciones + LigaRepository + CacheLecturas.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PosicionFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheClaves;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPosiciones;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PosicionTabla;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SincronizarPosicionesUseCase {

    private final LigaRepository ligaRepository;
    private final ProveedorPosiciones proveedorPosiciones;
    private final CacheLecturas cacheLecturas;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public SincronizarPosicionesUseCase(LigaRepository ligaRepository,
                                        ProveedorPosiciones proveedorPosiciones,
                                        CacheLecturas cacheLecturas) {
        this.ligaRepository = ligaRepository;
        this.proveedorPosiciones = proveedorPosiciones;
        this.cacheLecturas = cacheLecturas;
    }

    // [QUÉ]: Ejecuta CU-01: obtiene la liga, consulta posiciones, las mapea a
    //        PosicionTabla y las guarda en el aggregate. Devuelve los eventos.
    public List<DomainEvent> ejecutar(UUID ligaId) {
        cacheLecturas.eliminar(CacheClaves.posiciones(ligaId));
        Liga liga = ligaRepository.buscarPorId(ligaId)
                .orElseThrow(() -> new DomainException("Liga no encontrada: " + ligaId));

        List<PosicionFuente> fuentes = proveedorPosiciones.obtenerPosiciones(ligaId);
        List<PosicionTabla> posiciones = new ArrayList<>();
        for (PosicionFuente fuente : fuentes) {
            Equipo equipo = resolverEquipo(liga, fuente.equipoNombre());
            posiciones.add(new PosicionTabla(
                    equipo, fuente.posicion(), fuente.jugados(), fuente.ganados(), fuente.empatados(),
                    fuente.perdidos(), fuente.golesFavor(), fuente.golesContra(), fuente.puntos(),
                    fuente.ultimosResultados()));
        }

        liga.actualizarPosiciones(posiciones); // BR-002 exigido aquí (liga debe estar ACTIVA)
        ligaRepository.guardar(liga);
        return liga.pullEventos();
    }

    // [QUÉ]: Resuelve el Equipo de la liga por nombre; si no existe, lo crea y lo agrega.
    // [POR QUÉ]: Las posiciones vienen de la fuente con el nombre del equipo; el caso de
    //            uso lo reconecta con la entidad Equipo que el aggregate ya conoce.
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