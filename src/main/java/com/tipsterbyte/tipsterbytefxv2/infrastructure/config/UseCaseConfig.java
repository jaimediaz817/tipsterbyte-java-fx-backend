// ─────────────────────────────────────────────
// [QUÉ]: Configuración de infraestructura que registra los casos de uso de application
//        como beans de Spring (wiring REST de FASE 8.5).
// [POR QUÉ]: Los casos de uso son POJOs de la capa application (sin anotaciones de
//            Spring) para mantener la Dependency Rule. Al activar app.api.rest.enabled
//            los controllers exigen estos beans; esta configuración los declara
//            explícitamente con sus dependencias (puertos adaptados por infrastructure).
// [ALTERNATIVAS]: Anotar cada use case con @Component; se descarta porque acoplaría la
//                 capa application a Spring. Un @Configuration central concentra el
//                 wiring en infraestructura.
// [RELACIONES]: Crea los beans que inyectan LigaController, PronosticoController,
//               SuscripcionController, PartidoController y FuenteExtraccionController.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.config;

import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.FuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PasswordHasher;
import com.tipsterbyte.tipsterbytefxv2.application.port.PronosticoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCalendario;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCuotas;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorLigasPorPais;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPosiciones;
import com.tipsterbyte.tipsterbytefxv2.application.port.SuscripcionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TokenEmisor;
import com.tipsterbyte.tipsterbytefxv2.application.port.UsuarioRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ActivarLigaUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.AutenticarUsuarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarEstadoCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarPronosticosUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ObtenerJornadaActualUseCase;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    // [QUÉ]: Bean de CU-04 (activar liga con URLs de fuentes).
    @Bean
    public ActivarLigaUseCase activarLigaUseCase(LigaRepository ligaRepository,
                                                 FuenteExtraccionRepository fuenteRepository,
                                                 DetalleFuenteExtraccionRepository detalleRepository) {
        return new ActivarLigaUseCase(ligaRepository, fuenteRepository, detalleRepository);
    }

    // [QUÉ]: Bean de CU-01 (sincronizar posiciones, con invalidación de cache FASE 12).
    @Bean
    public SincronizarPosicionesUseCase sincronizarPosicionesUseCase(LigaRepository ligaRepository,
                                                                     ProveedorPosiciones proveedorPosiciones,
                                                                     CacheLecturas cacheLecturas) {
        return new SincronizarPosicionesUseCase(ligaRepository, proveedorPosiciones, cacheLecturas);
    }

    // [QUÉ]: Bean de CU-02 (sincronizar calendario, con invalidación de cache FASE 12).
    @Bean
    public SincronizarCalendarioUseCase sincronizarCalendarioUseCase(LigaRepository ligaRepository,
                                                                     PartidoRepository partidoRepository,
                                                                     ProveedorCalendario proveedorCalendario,
                                                                     CacheLecturas cacheLecturas) {
        return new SincronizarCalendarioUseCase(ligaRepository, partidoRepository, proveedorCalendario, cacheLecturas);
    }

    // [QUÉ]: Bean de CU-03 (sincronizar cuotas, con invalidación de cache FASE 12).
    @Bean
    public SincronizarCuotasUseCase sincronizarCuotasUseCase(PartidoRepository partidoRepository,
                                                             ProveedorCuotas proveedorCuotas,
                                                             CacheLecturas cacheLecturas) {
        return new SincronizarCuotasUseCase(partidoRepository, proveedorCuotas, cacheLecturas);
    }

    // [QUÉ]: Bean de CU-05 (registrar resultado de partido).
    @Bean
    public RegistrarResultadoUseCase registrarResultadoUseCase(PartidoRepository partidoRepository) {
        return new RegistrarResultadoUseCase(partidoRepository);
    }

    // [QUÉ]: Bean de CU-06 (crear pronóstico).
    @Bean
    public CrearPronosticoUseCase crearPronosticoUseCase(PronosticoRepository pronosticoRepository,
                                                         PartidoRepository partidoRepository) {
        return new CrearPronosticoUseCase(pronosticoRepository, partidoRepository);
    }

    // [QUÉ]: Bean de CU-07 (publicar pronóstico).
    @Bean
    public PublicarPronosticoUseCase publicarPronosticoUseCase(PronosticoRepository pronosticoRepository,
                                                               PartidoRepository partidoRepository) {
        return new PublicarPronosticoUseCase(pronosticoRepository, partidoRepository);
    }

    // [QUÉ]: Bean de CU-08 (consultar pronósticos públicos).
    @Bean
    public ConsultarPronosticosUseCase consultarPronosticosUseCase(SuscripcionRepository suscripcionRepository,
                                                                   PartidoRepository partidoRepository,
                                                                   PronosticoRepository pronosticoRepository) {
        return new ConsultarPronosticosUseCase(suscripcionRepository, partidoRepository, pronosticoRepository);
    }

    // [QUÉ]: Bean de CU-09 (crear suscripción).
    @Bean
    public CrearSuscripcionUseCase crearSuscripcionUseCase(SuscripcionRepository suscripcionRepository) {
        return new CrearSuscripcionUseCase(suscripcionRepository);
    }

    // [QUÉ]: Bean de CU-10 (sincronizar catálogo de países y ligas).
    @Bean
    public SincronizarCatalogoUseCase sincronizarCatalogoUseCase(ProveedorPaises proveedorPaises,
                                                                 ProveedorLigasPorPais proveedorLigasPorPais,
                                                                 PaisRepository paisRepository,
                                                                 LigaRepository ligaRepository) {
        return new SincronizarCatalogoUseCase(proveedorPaises, proveedorLigasPorPais, paisRepository, ligaRepository);
    }

    // [QUÉ]: Bean de consulta del estado del catálogo (CU-10): deriva VACIO/POBLADO
    //        de los conteos reales de países y ligas para el panel del SUPERADMIN.
    @Bean
    public ConsultarEstadoCatalogoUseCase consultarEstadoCatalogoUseCase(PaisRepository paisRepository,
                                                                          LigaRepository ligaRepository) {
        return new ConsultarEstadoCatalogoUseCase(paisRepository, ligaRepository);
    }

    // [QUÉ]: Bean de la jornada actual de una liga (CU-02): calcula la jornada del
    //        próximo partido por jugarse desde el calendario persistido.
    @Bean
    public ObtenerJornadaActualUseCase obtenerJornadaActualUseCase(PartidoRepository partidoRepository) {
        return new ObtenerJornadaActualUseCase(partidoRepository);
    }

    // [QUÉ]: Bean de CU-11 (gestionar catálogo de fuentes de extracción).
    @Bean
    public GestionarFuenteExtraccionUseCase gestionarFuenteExtraccionUseCase(
            FuenteExtraccionRepository fuenteRepository,
            DetalleFuenteExtraccionRepository detalleRepository) {
        return new GestionarFuenteExtraccionUseCase(fuenteRepository, detalleRepository);
    }

    // [QUÉ]: Bean de CU-12 (registro de usuario autenticable con BCrypt).
    @Bean
    public RegistrarUsuarioUseCase registrarUsuarioUseCase(UsuarioRepository usuarioRepository,
                                                           PasswordHasher passwordHasher) {
        return new RegistrarUsuarioUseCase(usuarioRepository, passwordHasher);
    }

    // [QUÉ]: Bean de CU-13 (login y emisión de JWT).
    @Bean
    public AutenticarUsuarioUseCase autenticarUsuarioUseCase(UsuarioRepository usuarioRepository,
                                                             PasswordHasher passwordHasher,
                                                             TokenEmisor tokenEmisor) {
        return new AutenticarUsuarioUseCase(usuarioRepository, passwordHasher, tokenEmisor);
    }
}
