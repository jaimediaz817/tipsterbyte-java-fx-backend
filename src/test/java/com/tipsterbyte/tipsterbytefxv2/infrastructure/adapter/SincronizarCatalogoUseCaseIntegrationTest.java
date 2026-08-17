// ─────────────────────────────────────────────
// [QUÉ]: Test de integración de CU-10 (SincronizarCatalogoUseCase) con el wiring
//        real: use case + adapters JPA contra PostgreSQL (Testcontainers). Los
//        proveedores externos (#1/#5) se mockean para no depender del servicio
//        Python en :8001 durante la ejecución de tests.
// [POR QUÉ]: Cierra la cadena application → port → adapter JPA → PostgreSQL con el
//            caso de uso real (no con repositorios aislados): valida que el catálogo
//            persiste de verdad y que la segunda ejecución no duplica (idempotencia).
// [ALTERNATIVAS]: Levantar el servicio Python real; se descarta porque introduce una
//                 dependencia externa frágil en la suite. Usar H2; se descarta porque
//                 no replica PostgreSQL (constraints, tipos).
// [RELACIONES]: CU-10 → ProveedorPaises + ProveedorLigasPorPais (mocks) +
//               PaisRepositoryJpaAdapter + LigaRepositoryJpaAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.LigaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorLigasPorPais;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.LigaJpaRepository;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.PaisJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class SincronizarCatalogoUseCaseIntegrationTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private PaisRepository paisRepository;

    @Autowired
    private LigaRepository ligaRepository;

    @Autowired
    private PaisJpaRepository paisJpaRepository;

    @Autowired
    private LigaJpaRepository ligaJpaRepository;

    @MockitoBean
    private ProveedorPaises proveedorPaises;

    @MockitoBean
    private ProveedorLigasPorPais proveedorLigasPorPais;

    private SincronizarCatalogoUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        // [POR QUÉ]: Los tests comparten el contenedor Testcontainers; se limpia para
        //            que los conteos de idempotencia sean estables entre tests.
        ligaJpaRepository.deleteAll();
        paisJpaRepository.deleteAll();

        casoDeUso = new SincronizarCatalogoUseCase(
                proveedorPaises, proveedorLigasPorPais, paisRepository, ligaRepository);
    }

    @Test
    void debe_persistir_paises_y_ligas_en_postgresql() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("España", "/espana/", "81", "ES", "Europa", true)));
        when(proveedorLigasPorPais.obtenerLigasPorPais("España", 0)).thenReturn(List.of(
                new LigaFuente("LaLiga EA Sports", "League", "", null,
                        "https://co.soccerway.com/espana/laliga-ea-sports/", "2026/2027")));

        casoDeUso.ejecutar();

        assertEquals(1, paisRepository.buscarTodos().size());
        assertEquals("España", paisRepository.buscarPorIsoAlpha2("ES").orElseThrow().nombre());
        assertEquals(1, ligaJpaRepository.findAll().size());
        assertEquals("LaLiga EA Sports", ligaJpaRepository.findAll().get(0).getNombre());
    }

    @Test
    void debe_ser_idempotente_entre_ejecuciones() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("España", "/espana/", "81", "ES", "Europa", true)));
        when(proveedorLigasPorPais.obtenerLigasPorPais("España", 0)).thenReturn(List.of(
                new LigaFuente("LaLiga EA Sports", "League", "", null,
                        "https://co.soccerway.com/espana/laliga-ea-sports/", "2026/2027")));

        casoDeUso.ejecutar();
        casoDeUso.ejecutar();

        assertEquals(1, paisRepository.buscarTodos().size());
        assertEquals(1, ligaJpaRepository.findAll().size());
    }

    @Test
    void debe_omitir_liga_con_temporada_invalida_y_continuar_con_el_catalogo() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("España", "/espana/", "81", "ES", "Europa", true),
                new PaisFuente("Colombia", "/colombia/", "81", "CO", "Sudamérica", true)));
        when(proveedorLigasPorPais.obtenerLigasPorPais("España", 0)).thenReturn(List.of(
                new LigaFuente("Liga Rara", "League", "", null, "https://url-rara", "Grupo 1")));
        when(proveedorLigasPorPais.obtenerLigasPorPais("Colombia", 0)).thenReturn(List.of(
                new LigaFuente("Liga Válida", "League", "", null, "https://url-valida", "2026/2027")));

        // El caso de uso omite la liga con `anio` inválido (log + continuar) y no aborta el catálogo.
        casoDeUso.ejecutar();

        assertEquals(2, paisRepository.buscarTodos().size(), "ambos países se persisten");
        assertEquals(1, ligaJpaRepository.findAll().size(), "la liga inválida se omite");
        assertEquals("Liga Válida", ligaJpaRepository.findAll().get(0).getNombre());
    }
}
