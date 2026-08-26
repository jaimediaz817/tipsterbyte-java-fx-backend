// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-18 (sync): sincroniza ligas de un país concreto desde #5.
// [POR QUÉ]: Paso 2 del poblamiento granular HU-12. Replica la lógica de
//            SincronizarCatalogoUseCase.sincronizarLigasDePais pero aislada por
//            isoAlpha2, para que el SUPERADMIN controle el flujo por pasos. Respeta
//            maxLigasPorPais (CU-14) y puebla equipos #6 solo si el país es de interés.
//            Es tolerante: una liga con anio inválido se omite con WARN y sigue.
// [ALTERNATIVAS]: Duplicar lógica en el controller; se descarta para mantener la
//                 regla única en application.
// [RELACIONES]: HU-12 → CU-18 → ProveedorLigasPorPais (#5) + SincronizarEquiposLigaUseCase
//               (#6) + PaisRepository + LigaRepository + PaisInteresRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.LigaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisInteresRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorLigasPorPais;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoTemporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pais;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public final class SincronizarLigasPorPaisUseCase {

    private static final Logger log = LoggerFactory.getLogger(SincronizarLigasPorPaisUseCase.class);

    public record ResultadoLigasPorPais(String isoAlpha2, String paisNombre, int ligasCreadas, int totalLigasPais) {}

    private final ProveedorLigasPorPais proveedorLigasPorPais;
    private final SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase;
    private final PaisRepository paisRepository;
    private final LigaRepository ligaRepository;
    private final PaisInteresRepository paisInteresRepository;

    public SincronizarLigasPorPaisUseCase(ProveedorLigasPorPais proveedorLigasPorPais,
                                          SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase,
                                          PaisRepository paisRepository,
                                          LigaRepository ligaRepository,
                                          PaisInteresRepository paisInteresRepository) {
        this.proveedorLigasPorPais = proveedorLigasPorPais;
        this.sincronizarEquiposLigaUseCase = sincronizarEquiposLigaUseCase;
        this.paisRepository = paisRepository;
        this.ligaRepository = ligaRepository;
        this.paisInteresRepository = paisInteresRepository;
    }

    // [QUÉ]: Ejecuta CU-18 para el país indicado por isoAlpha2 (case-insensitive).
    // [POR QUÉ]: La fuente #1 incluye GB-ENG/GB-SCT/GB-WLS/GB-NIR (6 chars): validar
    //            2 letras o 2+guion+3 (GB-ENG) para no rechazar esos países sin abrir a "COL".
    public ResultadoLigasPorPais ejecutar(String isoAlpha2Raw) {
        if (isoAlpha2Raw == null || !isoAlpha2Raw.trim().matches("(?i)^[a-z]{2}(-[a-z]{3})?$")) {
            throw new DomainException("isoAlpha2 inválido: " + isoAlpha2Raw);
        }
        String isoAlpha2 = isoAlpha2Raw.trim().toUpperCase();
        Pais pais = paisRepository.buscarPorIsoAlpha2(isoAlpha2)
                .orElseThrow(() -> new DomainException("País no encontrado para isoAlpha2: " + isoAlpha2
                        + ". Ejecuta primero POST /catalogo/poblar-paises"));

        boolean esDeInteres = paisInteresRepository.buscarPorIsoAlpha2(isoAlpha2).isPresent();
        Integer maxLigas = esDeInteres
                ? paisInteresRepository.buscarPorIsoAlpha2(isoAlpha2).map(p -> p.maxLigasPorPais()).orElse(null)
                : null;
        int limiteFuente = (maxLigas != null && maxLigas > 0) ? maxLigas : 0;

        System.out.println("[CU-18] >>> ANTES de llamar fuente #5: pais='" + pais.nombre() + "' limite=" + limiteFuente + " esDeInteres=" + esDeInteres + " (thread=" + Thread.currentThread() + ")");
        long t0 = System.currentTimeMillis();
        List<LigaFuente> ligasFuente = proveedorLigasPorPais.obtenerLigasPorPais(pais.nombre(), limiteFuente);
        System.out.println("[CU-18] <<< DESPUES de fuente #5: recibidas=" + ligasFuente.size() + " ligas en " + (System.currentTimeMillis() - t0) + " ms");
        if (limiteFuente > 0) {
            ligasFuente = ligasFuente.stream().limit(limiteFuente).toList();
        }

        int creadas = 0;
        for (LigaFuente ligaFuente : ligasFuente) {
            try {
                if (ligaRepository.buscarPorUrlSoccerway(ligaFuente.urlSoccerway()).isEmpty()) {
                    Liga liga = new Liga(
                            ligaFuente.nombre(), pais.nombre(), pais.id(),
                            ligaFuente.urlSoccerway(), ligaFuente.apiId());
                    liga.addTemporada(parsearTemporada(ligaFuente.anio(), liga.id()));
                    if (esDeInteres) {
                        poblarEquipos(liga);
                    }
                    ligaRepository.guardar(liga);
                    creadas++;
                }
            } catch (DomainException e) {
                log.warn("CU-18: se omite liga '{}' de '{}' por temporada inválida ({}).",
                        ligaFuente.nombre(), pais.nombre(), e.getMessage());
            }
        }
        int totalPais = ligaRepository.buscarPorPais(pais.nombre()).size();
        return new ResultadoLigasPorPais(isoAlpha2, pais.nombre(), creadas, totalPais);
    }

    private void poblarEquipos(Liga liga) {
        try {
            var resultado = sincronizarEquiposLigaUseCase.ejecutar(liga);
            log.info("CU-18: plantilla poblada para '{}' / '{}' -> {} nuevos, {} actualizados",
                    liga.pais(), liga.nombre(), resultado.creados(), resultado.actualizados());
        } catch (RuntimeException e) {
            log.warn("CU-18: no se pudo poblar equipos de '{}' / '{}' ({}).",
                    liga.pais(), liga.nombre(), e.getMessage());
        }
    }

    private Temporada parsearTemporada(String anio, UUID ligaId) {
        if (anio == null || anio.isBlank()) {
            throw new DomainException("Liga de catálogo sin temporada válida (anio): " + anio);
        }
        // Acepta ambos formatos: "YYYY/YYYY" o solo "YYYY"
        if (anio.contains("/")) {
            String[] partes = anio.split("/");
            try {
                return new Temporada(ligaId, anio.trim(), null,
                        Integer.parseInt(partes[0].trim()), Integer.parseInt(partes[1].trim()),
                        EstadoTemporada.PLANIFICADA);
            } catch (NumberFormatException e) {
                throw new DomainException("Liga de catálogo con anio inválido: " + anio, e);
            }
        } else {
            // Formato solo año: asumimos que es el año de inicio de la temporada
            // (ej: "2026" -> temporada 2026/2027)
            String anioLimpio = anio.trim();
            try {
                int anioInicio = Integer.parseInt(anioLimpio);
                return new Temporada(ligaId, anioLimpio + "/" + (anioInicio + 1), null,
                        anioInicio, anioInicio + 1,
                        EstadoTemporada.PLANIFICADA);
            } catch (NumberFormatException e) {
                throw new DomainException("Liga de catálogo con anio inválido: " + anio, e);
            }
        }
    }
}
