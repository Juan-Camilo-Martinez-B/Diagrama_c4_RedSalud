package com.redsaludandina.shared.infrastructure.web;

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
 * Seguridad HTTP. Con {@code redsalud.security.cognito=true} toda petición exige un JWT válido de
 * Cognito; en el perfil local no hay autenticación y el usuario llega en {@link Headers#USER}.
 */
@Configuration
public class SecurityConfiguration implements WebMvcConfigurer {

    private final boolean cognito;

    public SecurityConfiguration(@Value("${redsalud.security.cognito:false}") boolean cognito) {
        this.cognito = cognito;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        if (cognito) {
            http.authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                    .oauth2ResourceServer(server -> server.jwt(Customizer.withDefaults()));
        } else {
            http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        }
        return http.build();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthenticatedUserResolver(!cognito));
    }
}
