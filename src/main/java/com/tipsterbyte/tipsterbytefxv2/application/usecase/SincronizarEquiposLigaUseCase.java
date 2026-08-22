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

import java.util.List;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.service.NormalizadorNombresEquipos;

import java.util.Optional;
import java.util.UUID;

public final class SincronizarEquiposLigaUseCase {

    // [QUÉ]: Resultado del poblamiento para feedback de UI (badges "28/30").
    public record ResultadoPoblacionEquipos(int creados, int actualizados, int totalPlantilla) {
    }

    private final ProveedorEquiposPorLiga proveedorEquiposPorLiga;
    private final CacheLecturas cacheLecturas;
    private final LigaRepository ligaRepository;

    public SincronizarEquiposLigaUseCase(ProveedorEquiposPorLiga proveedorEquiposPorLiga,
                                         CacheLecturas cacheLecturas,
                                         LigaRepository ligaRepository) {
        this.proveedorEquiposPorLiga = proveedorEquiposPorLiga;
        this.cacheLecturas = cacheLecturas;
        this.ligaRepository = ligaRepository;
    }

    // [QUÉ]: Ejecuta CU-16 por id de liga: carga el aggregate, pobla su plantilla y guarda.
    // [POR QUÉ]: Entrada desde REST (botón manual). La liga puede estar BORRADOR o ACTIVA:
    //            los equipos son dato de catálogo/temporada, no requieren activación.
    public ResultadoPoblacionEquipos ejecutar(UUID ligaId) {
        Liga liga = ligaRepository.buscarPorId(ligaId)
                .orElseThrow(() -> new DomainException("Liga no encontrada: " + ligaId));
        ResultadoPoblacionEquipos resultado = ejecutar(liga);
        ligaRepository.guardar(liga);
        return resultado;
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
                temporada.equipos().size());
    }
}
