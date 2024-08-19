package com.bolsadeideas.springboot.webflux.app.handler;

import com.bolsadeideas.springboot.webflux.app.models.documents.Categoria;
import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.stereotype.Component;
import static org.springframework.web.reactive.function.BodyInserters.*;

import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

/**
 * Clase manejadora que define las operaciones CRUD para productos.
 * Utiliza la programación reactiva con Spring WebFlux.
 */
@Component
public class ProductoHandler {

    private final ProductoService service;

    @Value("${config.uploads.path}")
    private String path;

    private final Validator validator;

    @Autowired
    public ProductoHandler(ProductoService service, Validator validator) {
        this.service = service;
        this.validator = validator;
    }

    /**
     * Maneja la solicitud para subir una foto de un producto existente.
     *
     * @param request La solicitud del servidor que contiene el ID del producto y el archivo de foto.
     * @return Mono<ServerResponse> con el estado de la operación.
     */
    public Mono<ServerResponse> upload(ServerRequest request) {
        //Cuando se utiliza request.pathvariable es porque se pasa como parámetro ServerRequest
        String id = request.pathVariable("id");
        //Se obtiene el cuerpo de la solicitud multipart, formato utilizado para
        //enviar arcvhios junto con otros datos en una solicitud HTTP
        //A su vez, se mapea este cuerpo para extraer el archivo específico con clave "file"
        return request.multipartData().map(multipart -> multipart.toSingleValueMap().get("file"))
                //Se convierte el archivo extraído a un objeto FilePart,
                // que representa una parte del archivo en una solicitud multipart en WebFlux
                .cast(FilePart.class)
                //flatMap para buscar el producto en bd con el id
                // `service.findById(id)` devuelve Mono<Producto>
                .flatMap(file -> service.findById(id)
                        .flatMap(p -> {
                            p.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                                    .replace(" ", "-")
                                    .replace(":", "")
                                    .replace("\\", ""));
                            // `file.transferTo()` devuelve Mono<Void>
                            // `service.save(p)` devuelve Mono<Producto>
                            return file.transferTo(new File(path + p.getFoto())).then(service.save(p));
                //La respuesta incluye la URI del recurso actualizado y el objeto del producto en formato JSON.
                })).flatMap(p -> ServerResponse.created(URI.create("/api/v2/productos/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fromObject(p))
                        //Si no se encuentra el recurso solictado, tener lo siguiente
                        //en caso de que 'Mono' o 'Flux' no emita ningún valor.
                        .switchIfEmpty(ServerResponse.notFound().build()));
    }

    /**
     * Maneja la solicitud para crear un nuevo producto junto con una foto.
     *
     * @param request La solicitud del servidor que contiene los datos del producto y el archivo de foto.
     * @return Mono<ServerResponse> con el estado de la operación.
     */
    public Mono<ServerResponse> crearConFoto(ServerRequest request) {
        //Se extraen los datos del producto desde la solicitud multipart
        Mono<Producto> producto = request.multipartData().map(multipart -> {
            //Se mapean los datos del multipart para obtener los campos del producto
            FormFieldPart nombre = (FormFieldPart) multipart.toSingleValueMap().get("nombre");
            FormFieldPart precio = (FormFieldPart) multipart.toSingleValueMap().get("precio");
            FormFieldPart categoriaId = (FormFieldPart) multipart.toSingleValueMap().get("categoria.id");
            FormFieldPart categoriaNombre = (FormFieldPart) multipart.toSingleValueMap().get("categoria.nombre");

            Categoria categoria = new Categoria(categoriaNombre.value());
            categoria.setId(categoriaId.value());
            return new Producto(nombre.value(), Double.parseDouble(precio.value()), categoria);
        });

        //Se extrae el archivo adjunto desde la solicitud multipart.
        return request.multipartData().map(multipart -> multipart.toSingleValueMap().get("file"))
                //Se convierte el archivo a un objeto FilePart, que representa una parte de un archivo en una solicitud multipart.
                .cast(FilePart.class)
                //Se usa flatMap para combinar el archivo extraído con los datos del producto (producto).
                .flatMap(file -> producto
                        .flatMap(p -> {
                            p.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                                    .replace(" ", "-")
                                    .replace(":", "")
                                    .replace("\\", ""));
                            p.setCreateAt(new Date());
                            return file.transferTo(new File(path + p.getFoto())).then(service.save(p));
                        })).flatMap(p -> ServerResponse.created(URI.create("/api/v2/productos/".concat(p.getId())))
                        //Establece el tipo de contenido de respuesta HTTP
                        .contentType(MediaType.APPLICATION_JSON)
                        //Define el cuerpo o body de la respuesta HTTP
                        .body(fromObject(p)));
    }

    /**
     * Maneja la solicitud para listar todos los productos.
     *
     * @param request La solicitud del servidor.
     * @return Mono<ServerResponse> con la lista de productos en formato JSON.
     */
    public Mono<ServerResponse> listar(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.findAll(), Producto.class);
    }

    /**
     * Maneja la solicitud para obtener un producto por su ID.
     *
     * @param request La solicitud del servidor que contiene el ID del producto.
     * @return Mono<ServerResponse> con el producto encontrado o un estado 404 si no se encuentra.
     */
    public Mono<ServerResponse> ver(ServerRequest request) {

        String id = request.pathVariable("id");
        return service.findById(id).flatMap(p -> ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromObject(p)))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Maneja la solicitud para crear un nuevo producto.
     *
     * @param request La solicitud del servidor que contiene los datos del producto.
     * @return Mono<ServerResponse> con el producto creado en formato JSON.
     */
    public Mono<ServerResponse> crear(ServerRequest request) {
        Mono<Producto> producto = request.bodyToMono(Producto.class);

        return producto.flatMap(p -> {

            Errors errors = new BeanPropertyBindingResult(p, Producto.class.getName());
            validator.validate(p, errors);

            if (errors.hasErrors()) {
                return Flux.fromIterable(errors.getFieldErrors())
                        .map(fieldError -> "El campo " + fieldError.getField() + " " + fieldError.getDefaultMessage())
                        .collectList()
                        .flatMap(list -> ServerResponse.badRequest().body(fromObject(list)));
            } else {
                if(p.getCategoria() == null) {
                    p.setCreateAt(new Date());
                }
                return service.save(p).flatMap(pdb -> ServerResponse
                        .created(URI.create("/api/v2/productos/".concat(pdb.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fromObject(pdb)));
            }
        });
    }

    /**
     * Maneja la solicitud para actualizar un producto existente.
     *
     * @param request La solicitud del servidor que contiene los datos del producto a actualizar.
     * @return Mono<ServerResponse> con el producto actualizado en formato JSON.
     */
    public Mono<ServerResponse> editar(ServerRequest request) {
        Mono<Producto> producto = request.bodyToMono(Producto.class);
        String id = request.pathVariable("id");

        Mono<Producto> productoDb = service.findById(id);

        //zipWith: combinar dos flujos en un solo flujo (productoDb con producto en este caso)
        //zipWith combina el producto recuperado de la bd con el producto enviado en la solicitud 'req'
        //El resultado es un nuevo Mono<Producto> que contiene el producto actualizado
        return productoDb.zipWith(producto, (db, req) -> {
            db.setNombre(req.getNombre());
            db.setPrecio(req.getPrecio());
            db.setCategoria(req.getCategoria());
            return db;
        }).flatMap(p -> ServerResponse.created(URI.create("/api/v2/productos/".concat(p.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.save(p), Producto.class))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Maneja la solicitud para eliminar un producto por su ID.
     *
     * @param request La solicitud del servidor que contiene el ID del producto.
     * @return Mono<ServerResponse> con el estado de la operación de eliminación.
     */
    public Mono<ServerResponse> eliminar(ServerRequest request) {
        String id = request.pathVariable("id");

        Mono<Producto> productoDb = service.findById(id);

        return productoDb.flatMap(p -> service.delete(p).then(ServerResponse.noContent().build())
                .switchIfEmpty(ServerResponse.notFound().build()));
    }
}
