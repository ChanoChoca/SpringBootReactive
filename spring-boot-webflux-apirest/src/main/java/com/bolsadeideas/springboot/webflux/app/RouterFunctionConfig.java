package com.bolsadeideas.springboot.webflux.app;

import com.bolsadeideas.springboot.webflux.app.handler.ProductoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * Configuración de las rutas utilizando funciones router.
 * Define las rutas para el manejo de productos.
 */
@Configuration
public class RouterFunctionConfig {

    /**
     * Configura las rutas para las operaciones CRUD de productos.
     *
     * @param handler Manejador que contiene la lógica para las operaciones CRUD.
     * @return Las funciones router con las rutas definidas.
     */
    @Bean
    public RouterFunction<ServerResponse> routes(ProductoHandler handler) {

        return route(GET("/api/v2/productos")
                .or(GET("/api/v3/productos")), handler::listar) //request -> handler.listar(request));
                .andRoute(GET("/api/v2/productos/{id}"), handler::ver)
                .andRoute(POST("/api/v2/productos"), handler::crear)
                .andRoute(PUT("/api/v2/productos/{id}"), handler::editar)
                .andRoute(DELETE("/api/v2/productos/{id}"), handler::eliminar)
                .andRoute(POST("/api/v2/productos/upload/{id}"), handler::upload)
                .andRoute(POST("/api/v2/productos/crear"), handler::crearConFoto);
    }
}
