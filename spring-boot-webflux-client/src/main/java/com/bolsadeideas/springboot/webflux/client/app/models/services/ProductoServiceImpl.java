package com.bolsadeideas.springboot.webflux.client.app.models.services;

import com.bolsadeideas.springboot.webflux.client.app.models.Producto;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.http.MediaType.*;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementación del servicio de productos para un cliente WebFlux.
 * Provee métodos para realizar operaciones CRUD sobre productos,
 * incluyendo la carga de archivos.
 */
@Service
public class ProductoServiceImpl implements ProductoService {

    private final WebClient.Builder client;

    /**
     * Constructor que inyecta el cliente WebClient para realizar
     * las peticiones HTTP hacia el servidor.
     *
     * @param client Cliente WebClient configurado para realizar peticiones.
     */
    @Autowired
    public ProductoServiceImpl(WebClient.Builder client) {
        this.client = client;
    }

    /**
     * Obtiene todos los productos del servidor.
     *
     * @return Un Flux que emite los productos encontrados.
     */
    @Override
    public Flux<Producto> findAll() {
        return client.build().get().accept(APPLICATION_JSON)
                //.exchange(): para enviar una solicitud HTTP y obtener una respuesta de forma asíncrona, devuelve Mono<ClientResponse>
                .exchange()
                //.flatMapMany(): es útil cuando tienes un Mono que emite un Publisher (como Flux)
                // y deseas "aplanarlo" para trabajar con los elementos individuales emitidos por ese Publisher.
                //Son muchos productos, se utiliza .flatMapMany
                .flatMapMany(response -> response.bodyToFlux(Producto.class));
    }

    /**
     * Busca un producto por su ID.
     *
     * @param id El ID del producto a buscar.
     * @return Un Mono que emite el producto encontrado, o vacío si no existe.
     */
    @Override
    public Mono<Producto> findById(String id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        return client.build().get().uri("/{id}", params)
                // Indica que esperas una respuesta en JSON
                .accept(APPLICATION_JSON)
                //.retrieve(): se utiliza para enviar una solicitud HTTP y recuperar la respuesta
                // de manera más simple y directa en comparación con exchange() (manejas solo el cuerpo de la respuesta)
                .retrieve()
                .bodyToMono(Producto.class);
//                .exchange()
//                .flatMap(response -> response.bodyToMono(Producto.class));
    }

    /**
     * Guarda un nuevo producto en el servidor.
     *
     * @param producto El producto a guardar.
     * @return Un Mono que emite el producto guardado.
     */
    @Override
    public Mono<Producto> save(Producto producto) {
        return client.build().post()
                // Indica que esperas una respuesta en JSON
                .accept(APPLICATION_JSON)
                // Indica que el cuerpo de la solicitud es JSON
                .contentType(APPLICATION_JSON)
//                .body(fromObject(producto))
                //.syncBody(): establecer el cuerpo de la solicitud de manera sincrónica.
                .syncBody(producto)
                .retrieve()
                // Procesa la respuesta como un Mono<Producto>
                .bodyToMono(Producto.class);
    }

    /**
     * Actualiza un producto existente en el servidor.
     *
     * @param producto El producto actualizado.
     * @param id El ID del producto a actualizar.
     * @return Un Mono que emite el producto actualizado.
     */
    @Override
    public Mono<Producto> update(Producto producto, String id) {

        return client.build().put()
                .uri("/{id}", Collections.singletonMap("id", id))
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
//                .body(fromObject(producto))
                .syncBody(producto)
                .retrieve()
                .bodyToMono(Producto.class);
    }

    /**
     * Elimina un producto del servidor por su ID.
     *
     * @param id El ID del producto a eliminar.
     * @return Un Mono que completa cuando el producto es eliminado.
     */
    @Override
    public Mono<Void> delete(String id) {
        return client.build().delete().uri("/{id}", Collections.singletonMap("id", id))
                .retrieve()
                .bodyToMono(Void.class);
    }

    /**
     * Sube un archivo asociado a un producto por su ID.
     *
     * @param file El archivo a subir.
     * @param id El ID del producto al que se asocia el archivo.
     * @return Un Mono que emite el producto con el archivo asociado.
     */
    @Override
    public Mono<Producto> upload(FilePart file, String id) {
        MultipartBodyBuilder parts = new MultipartBodyBuilder();
        parts.asyncPart("file", file.content(), DataBuffer.class).headers(h -> {
            h.setContentDispositionFormData("file", file.filename());
        });

        return client.build().post()
                .uri("/upload/{id}", Collections.singletonMap("id", id))
                .contentType(MULTIPART_FORM_DATA)
                .syncBody(parts.build())
                .retrieve()
                .bodyToMono(Producto.class);
    }
}
