package com.bolsadeideas.springboot.webflux.client.app;

import com.bolsadeideas.springboot.webflux.client.app.handler.ProductoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import org.springframework.web.reactive.function.server.RouterFunction;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Configuración de las rutas utilizando funciones router en el contexto de un cliente web.
 * Define las rutas para el manejo de productos en un cliente WebFlux.
 */
@Configuration
public class RouterConfig {

    /**
     * Configura las rutas para las operaciones CRUD de productos en el cliente.
     *
     * @param handler Manejador que contiene la lógica para las operaciones CRUD.
     * @return Las funciones router con las rutas definidas.
     */
    @Bean
    public RouterFunction<ServerResponse> rutas(ProductoHandler handler) {
        return route(GET("/api/client"), handler::listar) // Ruta para listar todos los productos.
                .andRoute(GET("/api/client/{id}"), handler::ver) // Ruta para obtener un producto por su ID.
                .andRoute(POST("/api/client"), handler::crear) // Ruta para crear un nuevo producto.
                .andRoute(PUT("/api/client/{id}"), handler::editar) // Ruta para actualizar un producto existente por su ID.
                .andRoute(DELETE("/api/client/{id}"), handler::eliminar) // Ruta para eliminar un producto por su ID.
                .andRoute(POST("/api/client/upload/{id}"), handler::upload); // Ruta para subir un archivo asociado a un producto por su ID.
    }
}
