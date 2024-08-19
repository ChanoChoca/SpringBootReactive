package com.bolsadeideas.springboot.webflux.app.controllers;

import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para gestionar productos.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService service;

    /**
     * Constructor que inyecta el servicio de productos.
     *
     * @param service El servicio de productos.
     */
    @Autowired
    public ProductoController(ProductoService service) {
        this.service = service;
    }

    // Ruta configurada para guardar los archivos subidos.
    @Value("${config.uploads.path}")
    private String path;

    /**
     * Crea un nuevo producto con una imagen asociada.
     *
     * @param producto El producto a crear.
     * @param file     El archivo de imagen asociado al producto.
     * @return Un Mono que emite la respuesta HTTP con el producto creado.
     */
    @PostMapping("/v2")
    public Mono<ResponseEntity<Producto>> crearConFoto(Producto producto, @RequestPart FilePart file) {

        // Establece la fecha de creación si no está definida.
        if (producto.getCreateAt() == null) {
            producto.setCreateAt(new Date());
        }

        // Genera un nombre único para la foto.
        producto.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                .replace(" ", "")
                .replace(":", "")
                .replace("\\", ""));

        // Crea el directorio de almacenamiento si no existe.
        File uploadDir = new File(path);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Transfiere el archivo al sistema de archivos y guarda el producto en la base de datos.
        return file.transferTo(new File(path + producto.getFoto())).then(service.save(producto))
                .map(p -> ResponseEntity
                        .created(URI.create("/api/productos/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(p)
                );
    }

    /**
     * Sube una imagen para un producto existente.
     *
     * @param id   El ID del producto.
     * @param file El archivo de imagen a subir.
     * @return Un Mono que emite la respuesta HTTP con el producto actualizado.
     */
    @PostMapping("/upload/{id}")
    public Mono<ResponseEntity<Producto>> upload(@PathVariable String id, @RequestPart FilePart file) {
        return service.findById(id).flatMap(p -> {
                    p.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                            .replace(" ", "")
                            .replace(":", "")
                            .replace("\\", ""));

                    // Crea el directorio de almacenamiento si no existe.
                    File uploadDir = new File(path);
                    if (!uploadDir.exists()) {
                        uploadDir.mkdirs();
                    }

                    // Transfiere el archivo al sistema de archivos y guarda el producto actualizado.
                    return file.transferTo(new File(path + p.getFoto())).then(service.save(p));
                }).map(p -> ResponseEntity.ok(p))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Lista todos los productos.
     *
     * @return Un Mono que emite la respuesta HTTP con la lista de productos.
     */
    @GetMapping
    public Mono<ResponseEntity<Flux<Producto>>> lista() {
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(service.findAll())
        );
    }

    /**
     * Obtiene los detalles de un producto por su ID.
     *
     * @param id El ID del producto.
     * @return Un Mono que emite la respuesta HTTP con el producto solicitado.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Producto>> ver(@PathVariable String id) {
        return service.findById(id).map(p -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(p))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Crea un nuevo producto.
     *
     * @param monoProducto El Mono que emite el producto a crear.
     * @return Un Mono que emite la respuesta HTTP con el producto creado y mensajes de éxito o error.
     */
    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> crear(@Valid @RequestBody Mono<Producto> monoProducto) {
        Map<String, Object> respuesta = new HashMap<>();

        return monoProducto.flatMap(producto -> {
            if (producto.getCreateAt() == null) {
                producto.setCreateAt(new Date());
            }

            // Guarda el producto en la base de datos y devuelve la respuesta.
            return service.save(producto).map(p -> {
                respuesta.put("producto", p);
                respuesta.put("mensaje", "Producto creado con éxito");
                respuesta.put("timestamp", new Date());
                return ResponseEntity
                        .created(URI.create("/api/productos/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(respuesta);
            });
        }).onErrorResume(t -> {
            // Maneja errores de validación y los devuelve en la respuesta.
            return Mono.just(t).cast(WebExchangeBindException.class)
                    .flatMap(e -> Mono.just(e.getFieldErrors()))
                    .flatMapMany(Flux::fromIterable)
                    .map(fieldError -> "El campo " + fieldError.getField() + " " + fieldError.getDefaultMessage())
                    .collectList()
                    .flatMap(list -> {
                        respuesta.put("errors", list);
                        respuesta.put("timestamp", new Date());
                        respuesta.put("status", HttpStatus.BAD_REQUEST.value());
                        return Mono.just(ResponseEntity.badRequest().body(respuesta));
                    });
        });
    }

    /**
     * Edita un producto existente por su ID.
     *
     * @param id       El ID del producto.
     * @param producto El producto con los nuevos datos.
     * @return Un Mono que emite la respuesta HTTP con el producto actualizado.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Producto>> editar(@PathVariable String id, @RequestBody Producto producto) {
        return service.findById(id).flatMap(p -> {
                    p.setNombre(producto.getNombre());
                    p.setPrecio(producto.getPrecio());
                    p.setCategoria(producto.getCategoria());
                    return service.save(p);
                }).map(p -> ResponseEntity.created(URI.create("/api/productos/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(p))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Elimina un producto por su ID.
     *
     * @param id El ID del producto a eliminar.
     * @return Un Mono que emite la respuesta HTTP con el estado de la eliminación.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> eliminar(@PathVariable String id) {
        return service.findById(id).flatMap(p -> {
            return service.delete(p).then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)));
        }).defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
    }
}
