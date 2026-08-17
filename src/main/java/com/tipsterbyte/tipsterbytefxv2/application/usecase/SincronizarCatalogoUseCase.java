// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-10: sincroniza el catálogo de países y ligas desde las
//        fuentes reales #1 (países) y #5 (ligas por país).
// [POR QUÉ]: Puebla la base del sistema: países y ligas en BORRADOR para que luego
//            puedan activarse (CU-04) y sincronizarse (CU-01/02/03). Orquesta la
//            resolución de la Temporada desde el campo `anio` de la fuente #5 sin
//            conocer el formato de la API (los DTOs de fuente lo aíslan).
// [ALTERNATIVAS]: Cargar catálogo en la activación de liga (CU-04); se descarta porque
//                 el catálogo es un paso previo e independiente de la activación.
// [RELACIONES]: HU-10 → CU-10 → ProveedorPaises + ProveedorLigasPorPais + PaisRepository + LigaRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.LigaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorLigasPorPais;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pais;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class SincronizarCatalogoUseCase {

    private static final Logger log = LoggerFactory.getLogger(SincronizarCatalogoUseCase.class);

    private final ProveedorPaises proveedorPaises;
    private final ProveedorLigasPorPais proveedorLigasPorPais;
    private final PaisRepository paisRepository;
    private final LigaRepository ligaRepository;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public SincronizarCatalogoUseCase(ProveedorPaises proveedorPaises,
                                      ProveedorLigasPorPais proveedorLigasPorPais,
                                      PaisRepository paisRepository,
                                      LigaRepository ligaRepository) {
        this.proveedorPaises = proveedorPaises;
        this.proveedorLigasPorPais = proveedorLigasPorPais;
        this.paisRepository = paisRepository;
        this.ligaRepository = ligaRepository;
    }

    // [QUÉ]: Ejecuta CU-10: obtiene los países (#1), los persiste si son nuevos,
    //        y por cada país obtiene sus ligas (#5) persistiéndolas en BORRADOR
    //        si no existen. Devuelve los eventos (ninguno en el catálogo).
    public List<DomainEvent> ejecutar() {
        List<DomainEvent> eventos = new ArrayList<>();
        List<PaisFuente> paisesFuente = proveedorPaises.obtenerPaises();

        for (PaisFuente paisFuente : paisesFuente) {
            Pais pais = persistirPaisSiNuevo(paisFuente);
            sincronizarLigasDePais(pais.nombre());
        }
        return eventos;
    }

    // [QUÉ]: Persiste el país solo si no existe por su ISO alfa-2 (clave natural #1).
    private Pais persistirPaisSiNuevo(PaisFuente fuente) {
        return paisRepository.buscarPorIsoAlpha2(fuente.isoAlpha2())
                .orElseGet(() -> {
                    Pais nuevo = new Pais(
                            fuente.nombre(), fuente.isoAlpha2(), fuente.continente(),
                            fuente.code(), fuente.href(), fuente.mapeado());
                    paisRepository.guardar(nuevo);
                    return nuevo;
                });
    }

    // [QUÉ]: Obtiene las ligas del país (#5) y persiste las que aún no existen.
    // [POR QUÉ]: Solo se crean ligas en BORRADOR; la activación real es CU-04. Una liga
    //            con temporada inválida se omite con log (no aborta el poblamiento del
    //            catálogo: el scraper devuelve filas sueltas mal formadas en países reales).
    // [ALTERNATIVAS]: Abortar todo el catálogo ante la primera liga inválida (comportamiento
    //                 original); se descarta porque un país mal formado dejaba el catálogo
    //                 a medio poblar (ej: solo Albania de 176 países).
    private void sincronizarLigasDePais(String pais) {
        List<LigaFuente> ligasFuente = proveedorLigasPorPais.obtenerLigasPorPais(pais, 0);
        for (LigaFuente ligaFuente : ligasFuente) {
            try {
                if (ligaRepository.buscarPorUrlSoccerway(ligaFuente.urlSoccerway()).isEmpty()) {
                    ligaRepository.guardar(new Liga(
                            ligaFuente.nombre(), pais, parsearTemporada(ligaFuente.anio()),
                            ligaFuente.urlSoccerway(), ligaFuente.apiId()));
                }
            } catch (DomainException e) {
                log.warn("CU-10: se omite la liga '{}' de '{}' por temporada inválida ({}). Se continúa con el catálogo.",
                        ligaFuente.nombre(), pais, e.getMessage());
            }
        }
    }

    // [QUÉ]: Convierte el campo `anio` ("AAAA/AAAA") de la fuente #5 en Temporada.
    // [POR QUÉ]: El campo `semestre` es inconsistente (a veces temporada, a veces
    //            categoría como "Grupo 1"), por eso se usa `anio`. Un `anio` vacío
    //            o mal formado impide construir la liga (Liga requiere temporada).
    private Temporada parsearTemporada(String anio) {
        if (anio == null || anio.isBlank() || !anio.contains("/")) {
            throw new DomainException("Liga de catálogo sin temporada válida (anio): " + anio);
        }
        String[] partes = anio.split("/");
        try {
            return new Temporada(Integer.parseInt(partes[0].trim()), Integer.parseInt(partes[1].trim()));
        } catch (NumberFormatException e) {
            throw new DomainException("Liga de catálogo con anio inválido: " + anio, e);
        }
    }
}