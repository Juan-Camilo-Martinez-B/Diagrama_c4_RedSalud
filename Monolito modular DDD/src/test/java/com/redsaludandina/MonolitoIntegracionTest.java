package com.redsaludandina;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.redsaludandina.compartido.infraestructura.mensajeria.RelevoProgramado;
import com.redsaludandina.compartido.infraestructura.web.Encabezados;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * El monolito completo (Spring, Flyway, H2 en modo PostgreSQL, ACL contra un legado simulado)
 * recorrido por HTTP. Cada prueba usa sus propios ids y centros para no depender del orden.
 */
@SpringBootTest
@ActiveProfiles("test")
class MonolitoIntegracionTest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private RelevoProgramado relevo;

    @Autowired
    private JdbcClient jdbc;

    private MockMvc http;

    @BeforeEach
    void preparar() {
        http = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder solicitud,
                                 String cuerpo) throws Exception {
        return http.perform(solicitud.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Test
    void unaReservaLlegaPorElOutboxAInterconsultaYUnaDobleReservaDevuelve409() throws Exception {
        String interconsulta = id(1).valor();
        String cupo = id(3).valor();
        Instant manana = Instant.now().plus(Duration.ofDays(1));
        enviar(post("/interconsultas"), """
                {"interconsultaId":"%s","episodioOrigenId":"%s","pacienteId":"pac-it1","centroId":"centro-it1",
                 "especialidad":"Cardiología","motivo":"Soplo a estudio"}""".formatted(interconsulta, id(2).valor()))
                .andExpect(status().isNoContent());
        enviar(post("/agenda/cupos"), """
                {"cupoId":"%s","especialistaId":"esp-1","inicio":"%s","fin":"%s"}"""
                .formatted(cupo, manana, manana.plus(Duration.ofMinutes(30))))
                .andExpect(status().isNoContent());
        enviar(post("/agenda/cupos/" + cupo + "/reservas"), """
                {"reservaId":"%s","pacienteId":"pac-it1","centroId":"centro-it1"}""".formatted(id(4).valor()))
                .andExpect(status().isNoContent());

        enviar(post("/agenda/cupos/" + cupo + "/reservas"), """
                {"reservaId":"%s","pacienteId":"pac-otro","centroId":"centro-it1"}""".formatted(id(5).valor()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CUPO_YA_RESERVADO"));

        http.perform(get("/interconsultas/alertas").param("centroId", "centro-it1"))
                .andExpect(jsonPath("$[0].alertas[0]").value("PENDIENTE_AGENDAR"));
        relevo.relevarTodo();
        http.perform(get("/interconsultas/alertas").param("centroId", "centro-it1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("AGENDADA"))
                .andExpect(jsonPath("$[0].alertas[0]").value("PENDIENTE_ATENCION"));
    }

    @Test
    void elHistorialPasaPorLaAclSeCacheaYCadaLecturaQuedaAuditada() throws Exception {
        http.perform(get("/historial/pac-1").param("centroId", "centro-a").param("motivo", "Interconsulta")
                        .header(Encabezados.USUARIO, "med-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origen").value("LEGADO_DIRECTO"))
                .andExpect(jsonPath("$.vigente").value(true))
                .andExpect(jsonPath("$.episodios[0].referencia").value("AT-1002"))
                .andExpect(jsonPath("$.episodios[0].descripcion").value("(sin glosa)"))
                .andExpect(jsonPath("$.documentos[0].tipo").value("Laboratorio"))
                .andExpect(jsonPath("$.documentos[1].tipo").value("Radiografía"));

        http.perform(get("/historial/pac-1").param("centroId", "centro-a").param("motivo", "Control")
                        .header(Encabezados.USUARIO, "esp-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origen").value("READ_MODEL"));

        relevo.relevarTodo();
        http.perform(get("/auditoria/accesos").param("recurso", "historial/pac-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].usuarioId").value("med-1"))
                .andExpect(jsonPath("$[0].motivo").value("Interconsulta"))
                .andExpect(jsonPath("$[1].usuarioId").value("esp-1"));
    }

    @Test
    void unAccesoDenegadoDevuelve403YQuedaEnElOutboxDeIdentidad() throws Exception {
        http.perform(get("/historial/pac-1").param("centroId", "centro-a").param("motivo", "Curiosidad")
                        .header(Encabezados.USUARIO, "sin-perfil"))
                .andExpect(status().isForbidden());

        Integer denegaciones = jdbc.sql("SELECT COUNT(*) FROM identidad.outbox WHERE tipo = 'AccesoDenegado'"
                        + " AND agregado_id = 'sin-perfil'")
                .query(Integer.class).single();
        assertThat(denegaciones).isEqualTo(1);
    }

    @Test
    void unaTeleconsultaOfflineSeSincronizaUnaSolaVezYLlegaAAnalitica() throws Exception {
        String episodio = id(10).valor();
        String lote = """
                {"centroId":"centro-a","items":[
                  {"cambioId":"c3","agregadoId":"%s","secuencia":3,"tipo":"CIERRE_TELECONSULTA","episodioId":"%s"},
                  {"cambioId":"c1","agregadoId":"%s","secuencia":1,"tipo":"APERTURA_EPISODIO","episodioId":"%s",
                   "pacienteId":"pac-1"},
                  {"cambioId":"c2","agregadoId":"%s","secuencia":2,"tipo":"REGISTRO_ASISTENCIA","episodioId":"%s",
                   "campo":"MOTIVO","texto":"Fiebre","selloInstante":"2026-11-20T10:00:00Z"}]}"""
                .formatted(episodio, episodio, episodio, episodio, episodio, episodio);

        enviar(post("/sync/episodios").header("Idempotency-Key", "K-integracion-1"), lote)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[*].estado").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("APLICADO"))));
        enviar(post("/sync/episodios").header("Idempotency-Key", "K-integracion-1"), lote)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].estado").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("APLICADO"))));

        relevo.relevarTodo();
        relevo.relevarTodo();
        String periodo = YearMonth.now(ZoneOffset.UTC).toString();
        http.perform(get("/analitica/indicadores/centro-a/" + periodo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teleconsultas").value(1));

        http.perform(get("/episodios/" + episodio).param("motivo", "Control").header(Encabezados.USUARIO, "med-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ATENDIDO"))
                .andExpect(jsonPath("$.asistencia.MOTIVO").value("Fiebre"));
    }

    @Test
    void unConflictoClinicoEntreCentrosQuedaAuditadoConAmbosValores() throws Exception {
        String episodio = id(20).valor();
        enviar(post("/episodios"), """
                {"episodioId":"%s","pacienteId":"pac-1","centroId":"centro-a"}""".formatted(episodio))
                .andExpect(status().isNoContent());
        enviar(put("/episodios/" + episodio + "/asistencia/hallazgos"), """
                {"texto":"Tos seca","centroId":"centro-a","selloInstante":"2026-11-20T10:00:00Z"}""")
                .andExpect(status().isNoContent());
        enviar(put("/episodios/" + episodio + "/asistencia/hallazgos"), """
                {"texto":"Tos productiva","centroId":"centro-b","selloInstante":"2026-11-20T10:05:00Z"}""")
                .andExpect(status().isNoContent());

        relevo.relevarTodo();

        String detalle = jdbc.sql("SELECT detalle FROM auditoria.evento_acceso WHERE recurso = :recurso"
                        + " AND tipo = 'VALOR_CLINICO_DESCARTADO'")
                .param("recurso", "episodio/" + episodio)
                .query(String.class).single();
        assertThat(detalle).contains("Tos productiva").contains("Tos seca");
    }

    @Test
    void unaRecetaFirmadaSeEnviaALaFarmaciaUnaSolaVezYSoloLaFirmaSuPrescriptor() throws Exception {
        String receta = id(30).valor();
        enviar(post("/recetas").header(Encabezados.USUARIO, "esp-1"), """
                {"recetaId":"%s","episodioId":"%s"}""".formatted(receta, id(31).valor()))
                .andExpect(status().isNoContent());
        enviar(post("/recetas/" + receta + "/medicamentos"), """
                {"nombre":"Amoxicilina","dosis":"500 mg","indicacion":"Cada 8 horas por 7 días"}""")
                .andExpect(status().isNoContent());

        enviar(post("/recetas/" + receta + "/firma").header(Encabezados.USUARIO, "esp-2"), """
                {"valorFirma":"firma-ajena"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("FIRMANTE_NO_ES_PRESCRIPTOR"));
        enviar(post("/recetas/" + receta + "/firma").header(Encabezados.USUARIO, "esp-1"), """
                {"valorFirma":"firma-esp-1"}""")
                .andExpect(status().isNoContent());

        relevo.relevarTodo();
        relevo.relevarTodo();

        Integer envios = jdbc.sql("SELECT COUNT(*) FROM receta.envio_farmacia WHERE receta_id = :id")
                .param("id", receta).query(Integer.class).single();
        assertThat(envios).isEqualTo(1);
    }

    @Test
    void reservarUnCupoQueNoExisteDevuelve404YUnIdInvalido400() throws Exception {
        enviar(post("/agenda/cupos/" + id(40).valor() + "/reservas"), """
                {"reservaId":"%s","pacienteId":"pac-1","centroId":"centro-a"}""".formatted(id(41).valor()))
                .andExpect(status().isNotFound());
        enviar(post("/agenda/cupos/no-es-un-ulid/reservas"), """
                {"reservaId":"%s","pacienteId":"pac-1","centroId":"centro-a"}""".formatted(id(41).valor()))
                .andExpect(status().isBadRequest());
    }
}
