package com.redsaludandina.compartido.infraestructura.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Con Cognito activo: sin token no hay acceso, el usuario sale del {@code sub} del JWT y el
 * encabezado X-Usuario-Id se ignora (nadie puede declararse otro usuario).
 */
@SpringBootTest(properties = {
        // Base propia: este contexto no debe dejar datos (caché del historial, denegaciones) a otras pruebas.
        "spring.datasource.url=jdbc:h2:mem:redsalud-cognito;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "redsalud.seguridad.cognito=true",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://cognito-idp.us-east-1.amazonaws.com/pool-prueba/.well-known/jwks.json"
})
@ActiveProfiles("test")
class SeguridadCognitoTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc http;

    @BeforeEach
    void preparar() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    void sinTokenDevuelve401AunqueTraigaElEncabezadoLocal() throws Exception {
        http.perform(get("/historial/pac-1").param("centroId", "centro-a").param("motivo", "Control")
                        .header(Encabezados.USUARIO, "med-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenElUsuarioEsElSubDelJwt() throws Exception {
        http.perform(get("/historial/pac-1").param("centroId", "centro-a").param("motivo", "Control")
                        .with(jwt().jwt(token -> token.subject("med-1"))))
                .andExpect(status().isOk());
    }

    @Test
    void elEncabezadoNoSuplantaAlUsuarioDelToken() throws Exception {
        http.perform(get("/historial/pac-1").param("centroId", "centro-a").param("motivo", "Control")
                        .header(Encabezados.USUARIO, "med-1")
                        .with(jwt().jwt(token -> token.subject("sin-perfil"))))
                .andExpect(status().isForbidden());
    }
}
