// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de ConsultarEstadoCatalogoUseCase (CU-10): deriva el estado
//        del catálogo (VACIO/POBLADO) a partir de los conteos reales de países y ligas.
// [POR QUÉ]: Valida la regla de derivación del estado (POBLADO solo con países Y
//            ligas) sin levantar Spring (regla testing.md: use cases con Mockito).
// [RELACIONES]: CU-10 → PaisRepository + LigaRepository → CatalogoController.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.CatalogoEstadoDto;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoCatalogo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarEstadoCatalogoUseCaseTest {

    @Mock
    private PaisRepository paisRepository;

    @Mock
    private LigaRepository ligaRepository;

    private ConsultarEstadoCatalogoUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new ConsultarEstadoCatalogoUseCase(paisRepository, ligaRepository);
    }

    @Test
    void debe_devolver_poblado_cuando_hay_paises_y_ligas() {
        when(paisRepository.contar()).thenReturn(176L);
        when(ligaRepository.contar()).thenReturn(620L);

        CatalogoEstadoDto dto = casoDeUso.ejecutar();

        assertEquals(EstadoCatalogo.POBLADO, dto.estado());
        assertEquals(176, dto.totalPaises());
        assertEquals(620, dto.totalLigas());
    }

    @Test
    void debe_devolver_vacio_cuando_no_hay_datos() {
        when(paisRepository.contar()).thenReturn(0L);
        when(ligaRepository.contar()).thenReturn(0L);

        CatalogoEstadoDto dto = casoDeUso.ejecutar();

        assertEquals(EstadoCatalogo.VACIO, dto.estado());
        assertEquals(0, dto.totalPaises());
        assertEquals(0, dto.totalLigas());
    }

    @Test
    void debe_devolver_vacio_si_faltan_paises_o_ligas() {
        when(paisRepository.contar()).thenReturn(176L);
        when(ligaRepository.contar()).thenReturn(0L);

        CatalogoEstadoDto dto = casoDeUso.ejecutar();

        assertEquals(EstadoCatalogo.VACIO, dto.estado());
        assertEquals(176, dto.totalPaises());
        assertEquals(0, dto.totalLigas());
    }
}