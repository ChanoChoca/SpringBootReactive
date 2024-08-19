package com.bolsadeideas.springboot.webflux.client.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración de la aplicación para definir beans y propiedades relacionadas con WebClient.
 * Esta clase permite configurar y personalizar el cliente WebClient utilizado para realizar
 * solicitudes HTTP en la aplicación.
 */
@Configuration
public class AppConfig {

    /**
     * URL base del endpoint configurada en el archivo de propiedades.
     * Se inyecta automáticamente a partir del valor definido en la propiedad `config.base.endpoint`.
     */
    @Value("${config.base.endpoint}")
    private String url;

    /** AVISO: MODIFICAR XD
     * Define un bean para registrar una instancia de WebClient.
     * Este WebClient se configurará con la URL base proporcionada, lo que permite que todas las solicitudes
     * realizadas a través de este cliente utilicen dicha URL como punto de partida.
     *
     * @return Una instancia configurada de WebClient.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder registrarWebClient() {
        return WebClient.builder().baseUrl(url);
    }
}
