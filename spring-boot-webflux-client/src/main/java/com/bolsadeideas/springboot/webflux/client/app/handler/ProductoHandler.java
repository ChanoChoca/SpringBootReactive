package com.bolsadeideas.springboot.webflux.client.app.handler;

import com.bolsadeideas.springboot.webflux.client.app.models.Producto;
import com.bolsadeideas.springboot.webflux.client.app.models.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.http.MediaType.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler para gestionar las operaciones relacionadas con productos en una API reactiva.
 * Este componente maneja las solicitudes HTTP utilizando funciones servidoras y delega la lógica
 * del negocio al servicio de productos {@link ProductoService}.
 */
@Component
public class ProductoHandler {

    private final ProductoService service;

    /**
     * Constructor que inyecta el servicio de productos.
     *
     * @param service Servicio de productos utilizado para realizar operaciones CRUD.
     */
    @Autowired
    public ProductoHandler(ProductoService service) {
        this.service = service;
    }

    /**
     * Maneja la solicitud para listar todos los productos.
     *
     * @param request La solicitud del servidor.
     * @return Una respuesta del servidor con la lista de productos en formato JSON.
     */
    public Mono<ServerResponse> listar(ServerRequest request) {
        return ServerResponse.ok().contentType(APPLICATION_JSON)
                .body(service.findAll(), Producto.class);
    }

    /**
     * Maneja la solicitud para obtener un producto específico por su ID.
     *
     * @param request La solicitud del servidor que contiene el ID del producto.
     * @return Una respuesta del servidor con el producto encontrado o un 404 si no se encuentra.
     */
    public Mono<ServerResponse> ver(ServerRequest request) {
        String id = request.pathVariable("id");
        return errorHandler(
                //flatMap toma cada elemento emitido por un Mono o Flux y lo transforma en otro Mono o Flux. Luego, se aplana (flat)
                // el resultado para emitir los elementos del Mono o Flux interno directamente al suscriptor.
                //flatMap se usa para transformar el resultado de service.findById(id) en una respuesta del servidor (ServerResponse)
                service.findById(id).flatMap(p -> ServerResponse.ok()
                .contentType(APPLICATION_JSON) // Indica que el cuerpo de la solicitud es JSON
                .syncBody(p)) //.syncBody(): establecer el cuerpo de la solicitud de manera sincrónica.
                .switchIfEmpty(ServerResponse.notFound().build())//Devolver 404 en caso de no se encuentre
        );
    }

    /**
     * Maneja la solicitud para crear un nuevo producto.
     *
     * @param request La solicitud del servidor que contiene los datos del producto a crear.
     * @return Una respuesta del servidor con el producto creado o un error en caso de que ocurra.
     */
    public Mono<ServerResponse> crear(ServerRequest request) {
        Mono<Producto> producto = request.bodyToMono(Producto.class);

        return producto.flatMap(p -> {
            if (p.getCreateAt() == null) {
                p.setCreateAt(new Date());
            }
            return service.save(p);
        }).flatMap(p -> ServerResponse.created(URI.create("/api/client/".concat(p.getId())))
                .contentType(APPLICATION_JSON)
                .syncBody(p))
                //.onErrorResume(): se utiliza para manejar errores que pueden ocurrir en un flujo reactivo (Mono o Flux)
                // Permite interceptar un error en un flujo reactivo y, en lugar de dejar que el error termine el flujo,
                // puedes continuar con otro flujo alternativo o emitir un valor de reemplazo.
                .onErrorResume(error -> {
                    WebClientResponseException errorResponse = (WebClientResponseException) error;
                            if (errorResponse.getStatusCode() == HttpStatus.BAD_REQUEST) {
                                return ServerResponse.badRequest()
                                        .contentType(APPLICATION_JSON)
                                        //cuerpo de la respuesta que indica qué salió mal de forma sincrónica.
                                        .syncBody(errorResponse.getResponseBodyAsString());
                            }
                            return Mono.error(errorResponse);
                });
    }

    /**
     * Maneja la solicitud para editar un producto existente.
     *
     * @param request La solicitud del servidor que contiene los datos del producto a editar y su ID.
     * @return Una respuesta del servidor con el producto editado o un error en caso de que ocurra.
     */
    public Mono<ServerResponse> editar(ServerRequest request) {
        Mono<Producto> producto = request.bodyToMono(Producto.class);
        String id = request.pathVariable("id");

        return errorHandler(
                producto
                .flatMap(p -> service.update(p, id))
                .flatMap(p -> ServerResponse.created(URI.create("/api/client/".concat(p.getId())))
                .contentType(APPLICATION_JSON)
                .syncBody(p))
        );
    }

    /**
     * Maneja la solicitud para eliminar un producto existente.
     *
     * @param request La solicitud del servidor que contiene el ID del producto a eliminar.
     * @return Una respuesta del servidor confirmando la eliminación o un error en caso de que ocurra.
     */
    public Mono<ServerResponse> eliminar(ServerRequest request) {
        String id = request.pathVariable("id");
        return errorHandler(
                service.delete(id).then(ServerResponse.noContent().build())
        );
    }

    /**
     * Maneja la solicitud para subir un archivo y asociarlo a un producto existente.
     *
     * @param request La solicitud del servidor que contiene el archivo y el ID del producto.
     * @return Una respuesta del servidor con el producto actualizado o un error en caso de que ocurra.
     */
    public Mono<ServerResponse> upload(ServerRequest request) {
        String id = request.pathVariable("id");
        return errorHandler(
                request.multipartData().map(multipart -> multipart.toSingleValueMap().get("file"))
                .cast(FilePart.class) //Convertir de Mono<Part> a Mono<FilePart>
                .flatMap(file -> service.upload(file, id))
                .flatMap(p -> ServerResponse.created(URI.create("/api/client/".concat(p.getId())))
                        .contentType(APPLICATION_JSON)
                        .syncBody(p))
        );
    }

    /**
     * Maneja los errores que puedan ocurrir durante la ejecución de una operación.
     *
     * @param response La respuesta del servidor que puede contener un error.
     * @return Una respuesta de error personalizada o el error original.
     */
    private Mono<ServerResponse> errorHandler(Mono<ServerResponse> response) {
        return response.onErrorResume(error -> {
            WebClientResponseException errorResponse = (WebClientResponseException) error;
            if (errorResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                Map<String, Object> body = new HashMap<>();
                body.put("error", "No existe el producto: ".concat(errorResponse.getMessage()));
                body.put("timestamp", new Date());
                body.put("status", errorResponse.getStatusCode().value());
                return ServerResponse.status(HttpStatus.NOT_FOUND).syncBody(body);
            }
            return Mono.error(errorResponse);
        });
    }
}
