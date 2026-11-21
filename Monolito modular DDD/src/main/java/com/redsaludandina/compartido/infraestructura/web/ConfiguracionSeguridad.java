package com.redsaludandina.compartido.infraestructura.web;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Seguridad HTTP. Con {@code redsalud.seguridad.cognito=true} (perfil aws) toda la API exige un
 * JWT emitido por el user pool de Cognito y el usuario sale de su {@code sub}. Sin Cognito (perfil
 * local y pruebas) la API queda abierta y el usuario llega en {@link Encabezados#USUARIO}.
 *
 * <p>La autorización por rol y atributo NO vive aquí: se evalúa en el punto de acceso al dato,
 * en los casos de uso (Caso §5).
 */
@Configuration
public class ConfiguracionSeguridad implements WebMvcConfigurer {

    private final boolean cognito;

    public ConfiguracionSeguridad(@Value("${redsalud.seguridad.cognito:false}") boolean cognito) {
        this.cognito = cognito;
    }

    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        if (cognito) {
            http.authorizeHttpRequests(peticiones -> peticiones.anyRequest().authenticated())
                    .oauth2ResourceServer(servidor -> servidor.jwt(Customizer.withDefaults()));
        } else {
            http.authorizeHttpRequests(peticiones -> peticiones.anyRequest().permitAll());
        }
        return http.build();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolutores) {
        resolutores.add(new ResolutorUsuarioAutenticado(!cognito));
    }
}
