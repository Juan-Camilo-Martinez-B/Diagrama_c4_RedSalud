package com.redsaludandina.sincronizacion;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * La secuencia de sincronización contra PostgreSQL REAL (Testcontainers), donde la restricción
 * única, el bloqueo de filas y las transacciones se comportan como en Aurora. Se omite si la
 * máquina no tiene Docker.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SecuenciaSincronizacionPostgresTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry propiedades) {
        propiedades.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        propiedades.add("spring.datasource.username", POSTGRES::getUsername);
        propiedades.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JdbcClient jdbc;

    private MockMvc http;

    @BeforeEach
    void preparar() {
        http = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private static String lote(String episodio) {
        return """
                {"centroId":"centro-pg","items":[
                  {"cambioId":"c1","agregadoId":"%1$s","secuencia":1,"tipo":"APERTURA_EPISODIO","episodioId":"%1$s",
                   "pacienteId":"pac-1"},
                  {"cambioId":"c2","agregadoId":"%1$s","secuencia":2,"tipo":"REGISTRO_ASISTENCIA","episodioId":"%1$s",
                   "campo":"MOTIVO","texto":"Fiebre","selloInstante":"2026-11-20T10:00:00Z"}]}"""
                .formatted(episodio);
    }

    private MvcResult enviar(String clave, String cuerpo) throws Exception {
        return http.perform(post("/sync/episodios").header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
    }

    @Test
    void intento1Da201YElReintentoDa200ConLaMismaRespuesta() throws Exception {
        String episodio = id(7000).valor();

        MvcResult intento1 = enviar("K1-pg", lote(episodio));
        MvcResult intento2 = enviar("K1-pg", lote(episodio));

        assertThat(intento1.getResponse().getStatus()).isEqualTo(201);
        assertThat(intento2.getResponse().getStatus()).isEqualTo(200);
        assertThat(intento2.getResponse().getContentAsString()).isEqualTo(intento1.getResponse().getContentAsString());
        assertThat(jdbc.sql("SELECT COUNT(*) FROM episodio.episodio WHERE id = :id").param("id", episodio)
                .query(Integer.class).single()).isEqualTo(1);
    }

    /**
     * Carrera extrema: el mismo episodio (mismo ULID) llega a la vez en dos lotes con claves
     * distintas, así que ninguno encuentra la clave del otro. La restricción única deja pasar uno y
     * convierte el otro en YA_APLICADO: nunca hay dos episodios ni un error 5xx.
     */
    @Test
    void enLaCarreraExtremaLaRestriccionUnicaDejaUnSoloEpisodio() throws Exception {
        String episodio = id(7100).valor();
        String cuerpo = """
                {"centroId":"centro-pg","items":[
                  {"cambioId":"c1","agregadoId":"%1$s","secuencia":1,"tipo":"APERTURA_EPISODIO","episodioId":"%1$s",
                   "pacienteId":"pac-1"}]}""".formatted(episodio);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Future<MvcResult>> intentos = new ArrayList<>();
        for (String clave : List.of("K-carrera-a", "K-carrera-b")) {
            Callable<MvcResult> intento = () -> {
                salida.await();
                return enviar(clave, cuerpo);
            };
            intentos.add(hilos.submit(intento));
        }
        salida.countDown();

        List<String> estados = new ArrayList<>();
        for (Future<MvcResult> intento : intentos) {
            MvcResult resultado = intento.get();
            assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
            estados.add(resultado.getResponse().getContentAsString());
        }
        hilos.shutdown();

        assertThat(jdbc.sql("SELECT COUNT(*) FROM episodio.episodio WHERE id = :id").param("id", episodio)
                .query(Integer.class).single()).isEqualTo(1);
        assertThat(String.join(" ", estados)).doesNotContain("PENDIENTE");
    }

    /** Cero pérdida: varios lotes reenviados tras cortes terminan con exactamente un episodio cada uno. */
    @Test
    void ceroPerdidaNiDuplicadosConReintentos() throws Exception {
        int lotes = 10;
        for (int i = 0; i < lotes; i++) {
            String episodio = id(7200 + i).valor();
            String clave = "K-perdida-" + i;
            enviar(clave, lote(episodio));
            enviar(clave, lote(episodio));
        }

        Integer episodios = jdbc.sql("SELECT COUNT(*) FROM episodio.episodio WHERE centro_id = 'centro-pg'"
                        + " AND id BETWEEN :desde AND :hasta")
                .param("desde", id(7200).valor()).param("hasta", id(7200 + lotes - 1).valor())
                .query(Integer.class).single();
        assertThat(episodios).isEqualTo(lotes);
    }
}
