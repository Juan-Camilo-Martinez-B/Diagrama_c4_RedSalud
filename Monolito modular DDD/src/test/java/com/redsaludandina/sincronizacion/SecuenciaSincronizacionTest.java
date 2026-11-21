package com.redsaludandina.sincronizacion;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.redsaludandina.compartido.aplicacion.RegistroDuplicadoException;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.infraestructura.mensajeria.RelevoProgramado;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Diagrama de secuencia de sincronización, paso a paso, sobre el monolito completo:
 * intento 1 → 201 R; reintento con la misma clave → 200 y la MISMA R sin nuevos inserts; la clave
 * está PERSISTIDA (no en memoria); el evento sale por el outbox de la misma transacción; el bus
 * entrega «al menos una vez» y los consumidores descartan el duplicado por id de evento; cupos y
 * recetas firmadas se rechazan en el borde; la restricción única vuelve manejable la carrera.
 */
@SpringBootTest
@ActiveProfiles("test")
class SecuenciaSincronizacionTest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private RelevoProgramado relevo;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private UnidadDeTrabajo unidadDeTrabajo;

    private MockMvc http;

    @BeforeEach
    void preparar() {
        http = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private ResultActions sincronizar(String clave, String lote) throws Exception {
        return http.perform(post("/sync/episodios").header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON).content(lote));
    }

    /** Lote L1: episodio ULID-42 abierto, con motivo y cerrado sin conexión en centro-sec. */
    private static String loteL1(String episodio) {
        return """
                {"centroId":"centro-sec","items":[
                  {"cambioId":"c1","agregadoId":"%1$s","secuencia":1,"tipo":"APERTURA_EPISODIO","episodioId":"%1$s",
                   "pacienteId":"pac-1"},
                  {"cambioId":"c2","agregadoId":"%1$s","secuencia":2,"tipo":"REGISTRO_ASISTENCIA","episodioId":"%1$s",
                   "campo":"MOTIVO","texto":"Fiebre","selloInstante":"2026-11-20T10:00:00Z"},
                  {"cambioId":"c3","agregadoId":"%1$s","secuencia":3,"tipo":"CIERRE_TELECONSULTA","episodioId":"%1$s"}]}"""
                .formatted(episodio);
    }

    private int contar(String sql, Object valor) {
        return jdbc.sql(sql).param("v", valor).query(Integer.class).single();
    }

    @Test
    void intento1Da201YLosReintentosConLaMismaClaveDevuelvenLaMismaRespuestaSinNuevosInserts() throws Exception {
        String episodio = id(4200).valor();

        MvcResult intento1 = sincronizar("K1-secuencia", loteL1(episodio))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult intento2 = sincronizar("K1-secuencia", loteL1(episodio))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult intento3 = sincronizar("K1-secuencia", loteL1(episodio))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(intento2.getResponse().getContentAsString()).isEqualTo(intento1.getResponse().getContentAsString());
        assertThat(intento3.getResponse().getContentAsString()).isEqualTo(intento1.getResponse().getContentAsString());
        assertThat(contar("SELECT COUNT(*) FROM episodio.episodio WHERE id = :v", episodio)).isEqualTo(1);
        assertThat(contar("SELECT COUNT(*) FROM episodio.outbox WHERE agregado_id = :v AND tipo = 'PacienteAtendido'",
                episodio)).isEqualTo(1);
        assertThat(contar("SELECT COUNT(*) FROM sincronizacion.idempotencia WHERE clave = :v", "K1-secuencia"))
                .as("la clave está persistida en la base, no en memoria").isEqualTo(1);
    }

    @Test
    void laMismaClaveConOtroLoteDevuelve422() throws Exception {
        sincronizar("K-reutilizada", loteL1(id(4300).valor())).andExpect(status().isCreated());

        sincronizar("K-reutilizada", loteL1(id(4301).valor()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("IDEMPOTENCY_KEY_REUTILIZADA"));
        assertThat(contar("SELECT COUNT(*) FROM episodio.episodio WHERE id = :v", id(4301).valor())).isZero();
    }

    @Test
    void elConsumidorDescartaLaEntregaDuplicadaPorIdDeEvento() throws Exception {
        String episodio = id(4400).valor();
        sincronizar("K-duplicado", loteL1(episodio)).andExpect(status().isCreated());
        relevo.relevarTodo();
        long antes = teleconsultasDeCentroSec();

        // Simula la caída entre publicar y marcar: el relevo vuelve a publicar el mismo evento.
        int reabiertos = jdbc.sql("UPDATE episodio.outbox SET publicado_en = NULL WHERE agregado_id = :v")
                .param("v", episodio).update();
        relevo.relevarTodo();

        assertThat(reabiertos).isEqualTo(1);
        assertThat(teleconsultasDeCentroSec()).as("PacienteAtendido entregado dos veces, contado una").isEqualTo(antes);
    }

    private long teleconsultasDeCentroSec() {
        return jdbc.sql("SELECT teleconsultas FROM analitica.indicador WHERE centro_id = 'centro-sec' AND periodo = :v")
                .param("v", YearMonth.now(ZoneOffset.UTC).toString()).query(Long.class).single();
    }

    @Test
    void cuposYRecetasFirmadasSeRechazanEnElBordeParaReintentarContraElServidor() throws Exception {
        String lote = """
                {"centroId":"centro-sec","items":[
                  {"cambioId":"r1","agregadoId":"%s","secuencia":1,"tipo":"RESERVA_CUPO","cupoId":"%s","reservaId":"%s",
                   "pacienteId":"pac-1"},
                  {"cambioId":"f1","agregadoId":"%s","secuencia":1,"tipo":"FIRMA_RECETA","recetaId":"%s"}]}"""
                .formatted(id(4500).valor(), id(4500).valor(), id(4501).valor(), id(4502).valor(), id(4502).valor());

        sincronizar("K-servidor", lote)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.items[0].codigo").value("REQUIERE_SERVIDOR"))
                .andExpect(jsonPath("$.items[1].estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.items[1].codigo").value("REQUIERE_SERVIDOR"));
    }

    @Test
    void laRestriccionUnicaConvierteUnDuplicadoEnUnErrorManejable() {
        String episodio = id(4600).valor();
        jdbc.sql("INSERT INTO episodio.episodio (id, paciente_id, centro_id, estado) VALUES (:v, 'pac-1', 'centro-sec', 'ABIERTO')")
                .param("v", episodio).update();

        assertThatThrownBy(() -> unidadDeTrabajo.ejecutarSinResultado(() ->
                jdbc.sql("INSERT INTO episodio.episodio (id, paciente_id, centro_id, estado)"
                                + " VALUES (:v, 'pac-1', 'centro-sec', 'ABIERTO')")
                        .param("v", episodio).update()))
                .isInstanceOf(RegistroDuplicadoException.class);
        assertThat(contar("SELECT COUNT(*) FROM episodio.episodio WHERE id = :v", episodio)).isEqualTo(1);
    }
}
