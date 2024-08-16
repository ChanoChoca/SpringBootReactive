package com.bolsadeideas.springboot.webflux.app;

import com.bolsadeideas.springboot.webflux.app.models.documents.Categoria;
import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Clase de prueba para el servicio WebFlux de productos.
 */
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SpringBootWebfluxApirestApplicationTests {

	private final WebTestClient client;
	private final ProductoService service;

	@Value("${config.base.endpoint}")
	private String url;

	/**
	 * Constructor que inyecta el WebTestClient y el ProductoService.
	 *
	 * @param client  el WebTestClient para realizar solicitudes HTTP.
	 * @param service el servicio de productos para acceder a los datos.
	 */
	@Autowired
	public SpringBootWebfluxApirestApplicationTests(WebTestClient client, ProductoService service) {
		this.client = client;
		this.service = service;
	}

	/**
	 * Prueba para listar todos los productos.
	 */
	@Test
	public void listarTest() {

		client.get()
				.uri(url)
				.accept(MediaType.APPLICATION_JSON)
				//Enviar la petición con .exchange()
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBodyList(Producto.class)
				.consumeWith(response -> {
					List<Producto> productos = response.getResponseBody();
					productos.forEach(p -> {
						System.out.println(p.getNombre());
					});

					Assertions.assertThat(productos.size() > 0).isTrue();
				});
//				.hasSize(9);
	}

	/**
	 * Prueba para obtener un producto por su ID.
	 */
	@Test
	public void verTest() {
		//.block() para esperar y obtener el resultado de un Mono de forma asíncrona
		Producto producto = service.findByNombre("TV Panasonic Pantalla LCD").block();

		client.get()
				.uri(url + "/{id}", Collections.singletonMap("id", producto.getId()))
				.accept(MediaType.APPLICATION_JSON)
				//Enviar la petición con .exchange()
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(Producto.class)
				//La respuesta es consumida
				//Por lo que no va a permitir usar el .expectBody(), lo mismo a la viceversa
				.consumeWith(response -> {
					Producto p = response.getResponseBody();

					Assertions.assertThat(p.getId()).isNotEmpty();
					Assertions.assertThat(p.getId().length() > 0).isTrue();
					Assertions.assertThat(p.getNombre()).isEqualTo("TV Panasonic Pantalla LCD");
				});
		//.expectBody() para hacer aserciones directas sobre el contenido del cuerpo de la respuesta
//				.expectBody()
//				.jsonPath("$.id").isNotEmpty()
//				.jsonPath("$.nombre").isEqualTo("TV Panasonic Pantalla LCD");
	}

	/**
	 * Prueba para crear un nuevo producto.
	 */
	@Test
	public void crearTest() {

		Categoria categoria = service.findCategoriaByNombre("Mubles").block();

		Producto producto = new Producto("Mesa comedor", 100.00, categoria);

		client.post().uri(url)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(producto), Producto.class)
				//Enviar la petición con .exchange()
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.producto.id").isNotEmpty()
				.jsonPath("$.producto.nombre").isEqualTo("Mesa comedor")
				.jsonPath("$.producto.categoria.nombre").isEqualTo("Muebles");
	}

	/**
	 * Prueba alternativa para crear un nuevo producto y verificar la respuesta
	 * detallada.
	 */
	@Test
	public void crear2Test() {

		Categoria categoria = service.findCategoriaByNombre("Mubles").block();

		Producto producto = new Producto("Mesa comedor", 100.00, categoria);

		client.post().uri(url)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(producto), Producto.class)
				//Enviar con .exchange()
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(new ParameterizedTypeReference<LinkedHashMap<String, Object>>() {
				})
				.consumeWith(response -> {
					Object o = response.getResponseBody().get("producto");
					Producto p = new ObjectMapper().convertValue(o, Producto.class);
					Assertions.assertThat(p.getId()).isNotEmpty();
					Assertions.assertThat(p.getNombre()).isEqualTo("Mesa comedor");
					Assertions.assertThat(p.getCategoria().getNombre()).isEqualTo("Muebles");
				});
	}

	/**
	 * Prueba para editar un producto existente.
	 */
	@Test
	public void editarTest() {

		Producto producto = service.findByNombre("Sony Notebook").block();
		Categoria categoria = service.findCategoriaByNombre("Electrónico").block();

		Producto productoEditado = new Producto("Asus Notebook", 700.00, categoria);

		client.put().uri(url + "/{id}", Collections.singletonMap("id", producto.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(productoEditado), Producto.class)
				//Enviar con .exchange()
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.id").isNotEmpty()
				.jsonPath("$.nombre").isEqualTo("Asus Notebook")
				.jsonPath("$.categoria.nombre").isEqualTo("Electrónico");
	}

	/**
	 * Prueba para eliminar un producto por su ID.
	 */
	@Test
	public void eliminarTest() {
		Producto producto = service.findByNombre("Mica Cómoda 5 Cajones").block();
		client.delete()
				.uri(url + "/{id}", Collections.singletonMap("id", producto.getId()))
				.exchange()
				.expectStatus().isNoContent()
				.expectBody()
				.isEmpty();

		//Opcional, comprobar que retorna un NotFound
		client.get()
				.uri(url + "/{id}", Collections.singletonMap("id", producto.getId()))
				.exchange()
				.expectStatus().isNotFound()
				.expectBody()
				.isEmpty();
	}
}
