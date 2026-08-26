// ─────────────────────────────────────────────
// [QUÉ]: Configuración de infraestructura para los adapters de fuentes externas:
//        bean RestClient con base URL del proyecto Python de extracción.
// [POR QUÉ]: Centraliza el cliente HTTP de los adapters (FASE 8.5). El RestClient se
//            construye con la URL base configurable (app.fuentes.base-url) para que
//            los adapters solo apunten a sus recursos relativos.
// [ALTERNATIVAS]: Crear un RestClient por adapter; se descarta porque duplica la
//                 configuración de base URL y timeouts.
// [RELACIONES]: Configura los beans usados por SoccerwayPaisesAdapter y
//               SoccerwayLigasPorPaisAdapter (infrastructure.adapter).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FuentesExternasConfig {

    // [QUÉ]: Construye el RestClient con la URL base del servicio de extracción.
    // [POR QUÉ]: Los adapters consumen los endpoints ext-* de un solo host; exponer
    //            el RestClient como bean permite inyectarlo y reutilizarlo.
    //            HU-12 diagnóstico: se agregan timeouts porque el scraper Python puede
    //            tardar ~3 min por request (Selenium); sin read-timeout una caída del
    //            servicio dejaba tareas async en RUNNING eterno (sin ERROR visible).
    @Bean
    public RestClient restClientFuentes(@Value("${app.fuentes.base-url}") String baseUrl) {
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        org.springframework.http.client.JdkClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(java.time.Duration.ofMinutes(6));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}