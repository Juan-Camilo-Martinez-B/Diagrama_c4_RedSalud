package com.redsaludandina.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Cognito emite el JWT en producción. Aquí se firma uno localmente y se verifica con el mismo
 * decodificador Nimbus que usa el resource server ({@code oauth2ResourceServer().jwt()}).
 */
class JwtVerificationTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aSignedTokenVerifiesAndExposesTheSubject() throws Exception {
        RSAKey key = key();
        String token = sign(key, "med-1", Instant.now().plusSeconds(300));

        Jwt jwt = decoder(key).decode(token);

        assertThat(jwt.getSubject()).isEqualTo("med-1");
    }

    @Test
    void aTokenSignedByAnotherKeyIsRejected() throws Exception {
        String token = sign(key(), "med-1", Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> decoder(key()).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void theResolverReadsTheSubjectFromTheVerifiedJwt() throws Exception {
        RSAKey key = key();
        Jwt jwt = decoder(key).decode(sign(key, "med-1", Instant.now().plusSeconds(300)));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        AuthenticatedUserResolver resolver = new AuthenticatedUserResolver(false);

        Object userId = resolver.resolveArgument(userParameter(), null, null, null);

        assertThat(userId).isEqualTo("med-1");
    }

    private static RSAKey key() throws JOSEException {
        return new RSAKeyGenerator(2048).keyID("test-key").generate();
    }

    private static NimbusJwtDecoder decoder(RSAKey key) throws JOSEException {
        return NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
    }

    private static String sign(RSAKey key, String subject, Instant expiresAt) throws JOSEException {
        SignedJWT jwt = new SignedJWT(header(key), claims(subject, expiresAt));
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static JWSHeader header(RSAKey key) {
        return new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build();
    }

    private static JWTClaimsSet claims(String subject, Instant expiresAt) {
        return new JWTClaimsSet.Builder().subject(subject).expirationTime(Date.from(expiresAt)).build();
    }

    private static MethodParameter userParameter() throws NoSuchMethodException {
        return new MethodParameter(Probe.class.getDeclaredMethod("endpoint", String.class), 0);
    }

    private static final class Probe {
        @SuppressWarnings("unused")
        void endpoint(@AuthenticatedUser String userId) {
        }
    }
}
