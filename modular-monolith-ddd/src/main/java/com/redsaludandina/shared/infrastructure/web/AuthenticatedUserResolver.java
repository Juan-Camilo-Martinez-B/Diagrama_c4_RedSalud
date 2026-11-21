package com.redsaludandina.shared.infrastructure.web;

import java.util.Optional;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resuelve {@link AuthenticatedUser}: el {@code sub} del JWT de Cognito o, solo si se permite (perfil
 * local), el encabezado {@link Headers#USER}.
 */
public final class AuthenticatedUserResolver implements HandlerMethodArgumentResolver {

    private final boolean acceptLocalHeader;

    public AuthenticatedUserResolver(boolean acceptLocalHeader) {
        this.acceptLocalHeader = acceptLocalHeader;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedUser.class) && parameter.getParameterType() == String.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer model, NativeWebRequest request,
                                  WebDataBinderFactory binderFactory) {
        return fromJwt().or(() -> fromLocalHeader(request)).orElseThrow(UnauthenticatedException::new);
    }

    private static Optional<String> fromJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof JwtAuthenticationToken token
                ? Optional.ofNullable(token.getToken().getSubject())
                : Optional.empty();
    }

    private Optional<String> fromLocalHeader(NativeWebRequest request) {
        return acceptLocalHeader
                ? Optional.ofNullable(request.getHeader(Headers.USER)).map(String::strip).filter(user -> !user.isEmpty())
                : Optional.empty();
    }
}
