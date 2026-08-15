// ─────────────────────────────────────────────
// [QUÉ]: Test de integración del wiring REST (UseCaseConfig): verifica que los 10
//        casos de uso quedan registrados como beans de Spring con el contexto real.
// [POR QUÉ]: Los use cases son POJOs sin anotaciones; UseCaseConfig los declara con
//            sus dependencias. Este test garantiza que el wiring completo (controllers
//            → use cases → puertos → adapters) arranca sin beans faltantes.
// [ALTERNATIVAS]: Verificar solo por reflection en UseCaseConfig; se descarta porque
//                 no valida que Spring resuelva realmente las dependencias.
// [RELACIONES]: CU-01..11 → UseCaseConfig → adapters de infrastructure.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.config;

import com.tipsterbyte.tipsterbytefxv2.application.usecase.ActivarLigaUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.AutenticarUsuarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarPronosticosUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.CrearPronosticoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.CrearSuscripcionUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.GestionarFuenteExtraccionUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.PublicarPronosticoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.RegistrarResultadoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.RegistrarUsuarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCalendarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCuotasUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarPosicionesUseCase;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class UseCaseConfigTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void debe_registrar_los_12_casos_de_uso_como_beans() {
        assertNotNull(context.getBean(ActivarLigaUseCase.class));
        assertNotNull(context.getBean(SincronizarPosicionesUseCase.class));
        assertNotNull(context.getBean(SincronizarCalendarioUseCase.class));
        assertNotNull(context.getBean(SincronizarCuotasUseCase.class));
        assertNotNull(context.getBean(RegistrarResultadoUseCase.class));
        assertNotNull(context.getBean(CrearPronosticoUseCase.class));
        assertNotNull(context.getBean(PublicarPronosticoUseCase.class));
        assertNotNull(context.getBean(ConsultarPronosticosUseCase.class));
        assertNotNull(context.getBean(CrearSuscripcionUseCase.class));
        assertNotNull(context.getBean(SincronizarCatalogoUseCase.class));
        assertNotNull(context.getBean(GestionarFuenteExtraccionUseCase.class));
        assertNotNull(context.getBean(RegistrarUsuarioUseCase.class));
        assertNotNull(context.getBean(AutenticarUsuarioUseCase.class));
    }
}
