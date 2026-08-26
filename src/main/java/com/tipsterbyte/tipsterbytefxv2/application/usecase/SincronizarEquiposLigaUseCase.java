// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-16: sincroniza la plantilla de equipos de UNA liga desde la
//        fuente #6 (ext-soccerway-teams-by-league), sobre la temporada vigente.
// [POR QUÉ]: Permite (re)poblar los equipos de una liga ya registrada de forma
//            independiente — p. ej. cuando el scraper estaba caído durante el
//            poblamiento geográfico, o para refrescar escudos — sin re-ejecutar el
//            catálogo mundial. Es también la implementación canónica que CU-10 usa
//            encadenado al crear cada liga de país de interés (una sola regla de
//            matching/persistencia, cero duplicación).
// [ALTERNATIVAS]: Duplicar la lógica en CU-10 y en el controller; se descarta porque
//                 dos copias derivarían (regla única en un solo lugar).
// [RELACIONES]: HU-11 → CU-16 → ProveedorEquiposPorLiga (#6) + CacheLecturas; usado por
//               SincronizarCatalogoUseCase (CU-10) y por LigaController (POST sincronizar-equipos).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.EquipoFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheClaves;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorEquiposPorLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.PoblamientoEnCursoException;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.service.NormalizadorNombresEquipos;

import java.util.Optional;
import java.util.UUID;

public final class SincronizarEquiposLigaUseCase {

    // [QUÉ]: Resultado del poblamiento para feedback de UI (badges "28/30").
    //        desdePlantillaExistente=true indica que NO se consultó la fuente #6:
    //        la plantilla ya tenía equipos y no se pidió forzar (ruta rápida).
    public record ResultadoPoblacionEquipos(int creados, int actualizados, int totalPlantilla,
                                            boolean desdePlantillaExistente) {
    }

    private final ProveedorEquiposPorLiga proveedorEquiposPorLiga;
    private final CacheLecturas cacheLecturas;
    private final LigaRepository ligaRepository;

    // [QUÉ]: Anti-solapamiento por liga (HU-FRONT-05): evita dos scrapes #6 simultáneos
    //        del mismo botón (el scrape tarda minutos; dobles clicks disparaban carreras).
    private final ConcurrentHashMap<UUID, AtomicBoolean> enCursoPorLiga = new ConcurrentHashMap<>();

    public SincronizarEquiposLigaUseCase(ProveedorEquiposPorLiga proveedorEquiposPorLiga,
                                         CacheLecturas cacheLecturas,
                                         LigaRepository ligaRepository) {
        this.proveedorEquiposPorLiga = proveedorEquiposPorLiga;
        this.cacheLecturas = cacheLecturas;
        this.ligaRepository = ligaRepository;
    }

    // [QUÉ]: Ejecuta CU-16 por id de liga SIN forzar: si la plantilla ya tiene equipos,
    //        devuelve el conteo actual sin golpear la fuente #6 (ruta rápida, respuesta
    //        en milisegundos). Para refrescar escudos/datos desde la fuente usar
    //        ejecutar(ligaId, true).
    // [POR QUÉ]: HU-FRONT-05: el botón "cargar equipos" sobre una plantilla ya poblada
    //            re-scrapeaba Python (~minutos con spinner eterno). La BD es fuente de
    //            verdad del catálogo; la fuente #6 solo aporta datos nuevos/cambios.
    public ResultadoPoblacionEquipos ejecutar(UUID ligaId) {
        return ejecutar(ligaId, false);
    }

    // [QUÉ]: Ejecuta CU-16 con control explícito: forzar=false usa ruta rápida si hay
    //        plantilla; forzar=true invalida cache y re-scrapea la fuente (#6).
    // [POR QUÉ]: Entrada REST con query param opcional (?forzar=true) para "actualizar
    //            escudos" explícito. Anti-solapamiento por liga con liberación en finally.
    // [RELACIONES]: CU-10 sigue usando ejecutar(Liga) encadenado (plantilla vacía ahí).
    public ResultadoPoblacionEquipos ejecutar(UUID ligaId, boolean forzar) {
        AtomicBoolean flag = enCursoPorLiga.computeIfAbsent(ligaId, k -> new AtomicBoolean(false));
        if (!flag.compareAndSet(false, true)) {
            throw new PoblamientoEnCursoException(
                    "Ya hay una sincronización de equipos en curso para la liga " + ligaId);
        }
        try {
            Liga liga = ligaRepository.buscarPorId(ligaId)
                    .orElseThrow(() -> new DomainException("Liga no encontrada: " + ligaId));
            if (!forzar && !liga.equipos().isEmpty()) {
                return new ResultadoPoblacionEquipos(0, 0, liga.equipos().size(), true);
            }
            ResultadoPoblacionEquipos resultado = ejecutar(liga);
            ligaRepository.guardar(liga);
            return resultado;
        } finally {
            flag.set(false);
        }
    }

    // [QUÉ]: Puebla/mutua la plantilla de la temporada vigente del aggregate dado (sin
    //        guardar): matching normalizado, actualiza escudo si cambió, nunca elimina.
    // [POR QUÉ]: CU-10 lo invoca ANTES de su propio guardar (un solo save por liga).
    //            Devuelve conteos para trazabilidad/logs.
    // [ALTERNATIVAS]: Eliminar no coincidentes; se descarta hasta tener fuzzy matching
    //                 (FASE 17): borrar por no-coincidencia eliminaría equipos legítimos.
    public ResultadoPoblacionEquipos ejecutar(Liga liga) {
        if (liga.getTemporadas().isEmpty()) {
            throw new DomainException("La liga no tiene temporadas registradas: " + liga.id());
        }
        // Invalidación previa: patrón consistente con CU-01/02/03/10-países.
        cacheLecturas.eliminar(CacheClaves.equipos(liga.pais(), liga.nombre()));
        List<EquipoFuente> equiposFuente =
                proveedorEquiposPorLiga.obtenerEquipos(liga.pais(), liga.nombre());
        if (equiposFuente.isEmpty()) {
            throw new DomainException("La fuente #6 no devolvió equipos para '"
                    + liga.pais() + "' / '" + liga.nombre() + "'");
        }
        var temporada = liga.getTemporadas().iterator().next();
        int creados = 0;
        int actualizados = 0;
        for (EquipoFuente fuente : equiposFuente) {
            String buscado = NormalizadorNombresEquipos.normalizar(fuente.nombre());
            Optional<Equipo> existente = temporada.equipos().stream()
                    .filter(e -> NormalizadorNombresEquipos.normalizar(e.nombre()).equals(buscado))
                    .findFirst();
            if (existente.isPresent()) {
                temporada.actualizarEscudo(existente.get().id(), fuente.logoUrl());
                actualizados++;
            } else {
                temporada.agregarEquipo(new Equipo(fuente.nombre(), fuente.logoUrl()));
                creados++;
            }
        }
        return new ResultadoPoblacionEquipos(creados, actualizados,
                temporada.equipos().size(), false);
    }
}
