package com.redsaludandina.compartido.infraestructura.web;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resuelve {@link UsuarioAutenticado}:
 *
 * <ul>
 *   <li>Con un token de Cognito válido: su {@code sub}.</li>
 *   <li>Sin Cognito (perfil local y pruebas): el encabezado {@link Encabezados#USUARIO}.</li>
 *   <li>Con Cognito activo el encabezado se IGNORA: nadie puede declararse otro usuario.</li>
 * </ul>
 */
public final class ResolutorUsuarioAutenticado implements HandlerMethodArgumentResolver {

    private final boolean aceptarEncabezadoLocal;

    public ResolutorUsuarioAutenticado(boolean aceptarEncabezadoLocal) {
        this.aceptarEncabezadoLocal = aceptarEncabezadoLocal;
    }

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return parametro.hasParameterAnnotation(UsuarioAutenticado.class) && parametro.getParameterType() == String.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parametro, ModelAndViewContainer modelo, NativeWebRequest peticion,
                                  WebDataBinderFactory fabrica) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken token && token.getToken().getSubject() != null) {
            return token.getToken().getSubject();
        }
        if (aceptarEncabezadoLocal) {
            String encabezado = peticion.getHeader(Encabezados.USUARIO);
            if (encabezado != null && !encabezado.isBlank()) {
                return encabezado.strip();
            }
        }
        throw new NoAutenticadoException();
    }
}
