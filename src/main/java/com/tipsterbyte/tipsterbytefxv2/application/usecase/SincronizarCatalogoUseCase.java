// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-10: sincroniza el catálogo de países y ligas desde las
//        fuentes reales #1 (países) y #5 (ligas por país), y — para los países de
//        interés — puebla la plantilla de equipos de cada liga desde la fuente #6.
// [POR QUÉ]: Puebla la base del sistema: países y ligas en BORRADOR (con su temporada
//            de catálogo derivada del campo `anio`) para que luego puedan activarse
//            (CU-04) y sincronizarse (CU-01/02/03). Los países de interés (CU-14) se
//            procesan primero; el resto del mundo nunca se omite, solo se pospone.
//            El límite maxLigasPorPais acota cuántas ligas se extraen por país de
//            interés. La fuente #6 (equipos por liga) completa el paso 2 del
//            poblamiento: la plantilla oficial (nombre + escudo) queda en la temporada
//            vigente ANTES de activar las fuentes operativas (HU-11).
// [ALTERNATIVAS]: Cargar catálogo en la activación de liga (CU-04); se descarta porque
//                 el catálogo es un paso previo e independiente de la activación.
// [RELACIONES]: HU-10/HU-11 → CU-10 → ProveedorPaises (#1) + ProveedorLigasPorPais (#5)
//               + ProveedorEquiposPorLiga (#6) + PaisRepository + LigaRepository +
//               PaisInteresRepository (prioridad, límite y alcance) + CacheLecturas.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.LigaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheClaves;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisInteresRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorLigasPorPais;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoTemporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pais;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PaisInteres;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SincronizarCatalogoUseCase {

    private static final Logger log = LoggerFactory.getLogger(SincronizarCatalogoUseCase.class);

    private final ProveedorPaises proveedorPaises;
    private final ProveedorLigasPorPais proveedorLigasPorPais;
    private final SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase;
    private final PaisRepository paisRepository;
    private final LigaRepository ligaRepository;
    private final PaisInteresRepository paisInteresRepository;
    private final CacheLecturas cacheLecturas;

    // [QUÉ]: Construye el caso de uso (puertos + CU-16 para la plantilla de equipos).
    // [POR QUÉ]: La regla de poblamiento de equipos vive UNA sola vez (CU-16); CU-10 la
    //            reutiliza encadenada al crear cada liga de país de interés.
    public SincronizarCatalogoUseCase(ProveedorPaises proveedorPaises,
                                      ProveedorLigasPorPais proveedorLigasPorPais,
                                      SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase,
                                      PaisRepository paisRepository,
                                      LigaRepository ligaRepository,
                                      PaisInteresRepository paisInteresRepository,
                                      CacheLecturas cacheLecturas) {
        this.proveedorPaises = proveedorPaises;
        this.proveedorLigasPorPais = proveedorLigasPorPais;
        this.sincronizarEquiposLigaUseCase = sincronizarEquiposLigaUseCase;
        this.paisRepository = paisRepository;
        this.ligaRepository = ligaRepository;
        this.paisInteresRepository = paisInteresRepository;
        this.cacheLecturas = cacheLecturas;
    }

    // [QUÉ]: Ejecuta CU-10: obtiene los países (#1), los persiste si son nuevos, y por
    //        cada país obtiene sus ligas (#5) persistiéndolas en BORRADOR con su
    //        temporada; para países de interés, además puebla la plantilla de equipos
    //        desde la fuente #6. Devuelve los eventos (ninguno en el catálogo).
    // [POR QUÉ]: Los países de interés (CU-14) se procesan primero (prioridad de
    //            poblamiento); el resto del mundo sigue en orden de fuente.
    public List<DomainEvent> ejecutar() {
        List<DomainEvent> eventos = new ArrayList<>();
        // Invalidación del cache-aside de países: la sincronización fuerza datos
        // frescos para GET /paises/disponibles y la validación de CU-14 (FASE 12.6).
        cacheLecturas.eliminar(CacheClaves.paises());
        List<PaisFuente> paisesFuente = proveedorPaises.obtenerPaises();
        Set<String> isoPreferidos = isoPaisesDeInteres();

        for (PaisFuente paisFuente : ordenarConPreferidos(paisesFuente)) {
            Pais pais = persistirPaisSiNuevo(paisFuente);
            boolean esDeInteres = isoPreferidos.contains(pais.isoAlpha2().toUpperCase());
            sincronizarLigasDePais(pais, esDeInteres);
        }
        return eventos;
    }

    // [QUÉ]: Conjunto de ISO alfa-2 (mayúsculas) de los países de interés registrados.
    // [POR QUÉ]: Define el ALCANCE del paso de equipos (decisión FASE T2): solo las
    //            ligas de estos países consultan la fuente #6.
    private Set<String> isoPaisesDeInteres() {
        Set<String> isos = new HashSet<>();
        for (PaisInteres preferido : paisInteresRepository.listarPorPrioridad()) {
            isos.add(preferido.isoAlpha2().toUpperCase());
        }
        return isos;
    }

    // [QUÉ]: Ordena los países de la fuente con los de interés primero (por prioridad
    //        de CU-14) y después el resto en orden de fuente. Nunca omite países.
    // [POR QUÉ]: Ante una interrupción/reintento del poblamiento, los países que
    //            importan al usuario quedan asegurados primero.
    // [ALTERNATIVAS]: Sincronizar solo los de interés; se descarta porque el usuario
    //                 quiere poblar el mundo completo, la prioridad solo define el orden.
    private List<PaisFuente> ordenarConPreferidos(List<PaisFuente> paisesFuente) {
        List<PaisInteres> preferidos = paisInteresRepository.listarPorPrioridad();
        if (preferidos.isEmpty()) {
            return paisesFuente;
        }
        Set<String> isoPreferidos = new HashSet<>();
        for (PaisInteres preferido : preferidos) {
            isoPreferidos.add(preferido.isoAlpha2());
        }
        List<PaisFuente> ordenados = new ArrayList<>();
        for (PaisInteres preferido : preferidos) {
            paisesFuente.stream()
                    .filter(pais -> pais.isoAlpha2().equalsIgnoreCase(preferido.isoAlpha2()))
                    .findFirst()
                    .ifPresent(ordenados::add);
        }
        for (PaisFuente pais : paisesFuente) {
            if (!isoPreferidos.contains(pais.isoAlpha2().toUpperCase())) {
                ordenados.add(pais);
            }
        }
        return ordenados;
    }

    // [QUÉ]: Persiste el país solo si no existe por su ISO alfa-2 (clave natural #1).
    private Pais persistirPaisSiNuevo(PaisFuente fuente) {
        return paisRepository.buscarPorIsoAlpha2(fuente.isoAlpha2())
                .orElseGet(() -> {
                    Pais nuevo = new Pais(
                            UUID.randomUUID(),
                            fuente.nombre(), fuente.isoAlpha2(), fuente.continente(),
                            fuente.code(), fuente.href(), fuente.mapeado());
                    paisRepository.guardar(nuevo);
                    return nuevo;
                });
    }

    // [QUÉ]: Obtiene las ligas del país (#5) y persiste las que aún no existen con su
    //        temporada de catálogo; para países de interés, puebla además la plantilla
    //        de equipos desde la fuente #6 antes de guardar.
    // [POR QUÉ]: Solo se crean ligas en BORRADOR; la activación real es CU-04. Una liga
    //            con temporada inválida se omite con log (no aborta el poblamiento).
    //            El límite viaja también a la fuente #5 (param limit); el corte local
    //            con .limit() se conserva como red de seguridad.
    // [ALTERNATIVAS]: Abortar todo el catálogo ante la primera liga inválida (comportamiento
    //                 original); se descarta porque un país mal formado dejaba el catálogo
    //                 a medio poblar (ej: solo Albania de 176 países).
    private void sincronizarLigasDePais(Pais pais, boolean esDeInteres) {
        String nombrePais = pais.nombre();
        Integer maxLigas = esDeInteres ? resolverMaxLigasPorPais(pais.isoAlpha2()) : null;
        int limiteFuente = (maxLigas != null && maxLigas > 0) ? maxLigas : 0;
        List<LigaFuente> ligasFuente = proveedorLigasPorPais.obtenerLigasPorPais(nombrePais, limiteFuente);

        if (limiteFuente > 0) {
            ligasFuente = ligasFuente.stream()
                    .limit(limiteFuente)
                    .toList();
        }

        for (LigaFuente ligaFuente : ligasFuente) {
            try {
                if (ligaRepository.buscarPorUrlSoccerway(ligaFuente.urlSoccerway()).isEmpty()) {
                    // La liga de catálogo nace con FK real a su país (pais_id) y con la
                    // temporada derivada del campo anio (PLANIFICADA).
                    Liga liga = new Liga(
                            ligaFuente.nombre(), nombrePais, pais.id(),
                            ligaFuente.urlSoccerway(), ligaFuente.apiId());
                    liga.addTemporada(parsearTemporada(ligaFuente.anio(), liga.id()));
                    if (esDeInteres) {
                        // Paso 2 del poblamiento (HU-11): tolerante a fallos internamente.
                        poblarEquipos(liga);
                    }
                    ligaRepository.guardar(liga);
                }
            } catch (DomainException e) {
                log.warn("CU-10: se omite la liga '{}' de '{}' por temporada inválida ({}). Se continuó con el catálogo.",
                        ligaFuente.nombre(), nombrePais, e.getMessage());
            }
        }
    }

    // [QUÉ]: Puebla la plantilla de equipos de la temporada vigente de la liga desde la
    //        fuente #6: matching por nombre NORMALIZADO (sin tildes/case/espacios);
    //        existentes conservan id (y actualizan escudo si cambió), nuevos se crean.
    //        NUNCA elimina: los equipos que no aparezcan en #6 se conservan tal cual.
    // [POR QUÉ]: Decisión FASE T2 — sin fuzzy matching (FASE 17), eliminar por
    //            no-coincidencia borraría equipos legítimos escritos distinto. Un fallo
    //            de la fuente NO aborta el poblamiento: se registra con contexto
    //            país+liga (MDC) y continúa con la siguiente liga.
    // [ALTERNATIVAS]: Eliminar no coincidentes; se descartado hasta tener fuzzy matching.
    //                 Lanzar hacia arriba; se descarta porque tumbaría todo el poblamiento
    //                 mundial por un scraper caído puntual.
    private void poblarEquipos(Liga liga) {
        try {
            var resultado = sincronizarEquiposLigaUseCase.ejecutar(liga);
            log.info("CU-10: plantilla poblada para '{}' / '{}' → {} equipos nuevos, {} actualizados",
                    liga.pais(), liga.nombre(), resultado.creados(), resultado.actualizados());
        } catch (RuntimeException e) {
            // [POR QUÉ]: Tolerancia a fallos por liga (DomainException de negocio — ej.
            //            fuente vacía — o fallo HTTP de infraestructura): el poblamiento
            //            mundial continúa con la siguiente liga.
            log.warn("CU-10: no se pudo poblar equipos de '{}' / '{}' ({}). Se continuó con el catálogo.",
                    liga.pais(), liga.nombre(), e.getMessage());
        }
    }

    // [QUÉ]: Resuelve el límite de ligas a extraer para un país desde su país de
    //        interés (CU-14); vacío o sin límite configurado devuelve null (= sin tope).
    private Integer resolverMaxLigasPorPais(String isoAlpha2) {
        return paisInteresRepository.buscarPorIsoAlpha2(isoAlpha2)
                .map(PaisInteres::maxLigasPorPais)
                .orElse(null);
    }

    // [QUÉ]: Convierte el campo `anio` ("AAAA/AAAA") de la fuente #5 en Temporada de
    //        catálogo (PLANIFICADA), vinculada a la liga recién creada. El nombre de la
    //        temporada es el propio `anio` ("2025/2026"): identifica la temporada y
    //        satisface la unicidad (liga_id, nombre).
    // [POR QUÉ]: El campo `semestre` es inconsistente (a veces temporada, a veces
    //            categoría como "Grupo 1"), por eso se usa `anio`. Un `anio` vacío
    //            o mal formado impide construir la liga (sin temporada no hay catálogo).
    private Temporada parsearTemporada(String anio, UUID ligaId) {
        if (anio == null || anio.isBlank() || !anio.contains("/")) {
            throw new DomainException("Liga de catálogo sin temporada válida (anio): " + anio);
        }
        String[] partes = anio.split("/");
        try {
            return new Temporada(ligaId, anio.trim(), null,
                    Integer.parseInt(partes[0].trim()), Integer.parseInt(partes[1].trim()),
                    EstadoTemporada.PLANIFICADA);
        } catch (NumberFormatException e) {
            throw new DomainException("Liga de catálogo con anio inválido: " + anio, e);
        }
    }
}
