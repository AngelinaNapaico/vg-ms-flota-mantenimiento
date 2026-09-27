package vg_ms.flota.shared.security;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Decodificador dual compatible con:
 * 1. Tokens emitidos por Keycloak (RS256 con JWKS).
 * 2. Tokens emitidos por vg-ms-auth / legacy (HMAC HS512 / HS256).
 */
@Slf4j
@Configuration
public class DualJwtDecoderConfig {

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String jwtSecret,
            @Value("${keycloak.token-mode:KEYCLOAK}") String tokenMode,
            @Value("${keycloak.enabled:true}") boolean keycloakEnabled) {

        if (!keycloakEnabled || "LEGACY".equalsIgnoreCase(tokenMode)) {
            return buildLegacyDecoder(jwtSecret);
        }

        AtomicReference<ReactiveJwtDecoder> keycloakDecoder = new AtomicReference<>();
        ReactiveJwtDecoder legacyDecoder = buildLegacyDecoder(jwtSecret);

        return token -> {
            try {
                String alg = readAlgorithm(token);
                if (isAsymmetricAlgorithm(alg)) {
                    return resolveKeycloakDecoder(keycloakDecoder, issuerUri, jwkSetUri).decode(token);
                }
                if (isHmacAlgorithm(alg)) {
                    return legacyDecoder.decode(token);
                }
                return resolveKeycloakDecoder(keycloakDecoder, issuerUri, jwkSetUri)
                        .decode(token)
                        .onErrorResume(ex -> legacyDecoder.decode(token));
            } catch (Exception e) {
                return legacyDecoder.decode(token)
                        .onErrorResume(ex -> Mono.error(new JwtException("No se pudo validar el JWT ni por Keycloak ni por HMAC", e)));
            }
        };
    }

    private ReactiveJwtDecoder buildLegacyDecoder(String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    private ReactiveJwtDecoder resolveKeycloakDecoder(
            AtomicReference<ReactiveJwtDecoder> cache,
            String issuerUri,
            String jwkSetUri) {

        if (cache.get() == null) {
            synchronized (cache) {
                if (cache.get() == null) {
                    if (jwkSetUri != null && !jwkSetUri.isBlank()) {
                        cache.set(NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build());
                    } else if (issuerUri != null && !issuerUri.isBlank()) {
                        cache.set(NimbusReactiveJwtDecoder.withIssuerLocation(issuerUri).build());
                    } else {
                        throw new IllegalStateException(
                                "Debe configurar jwk-set-uri o issuer-uri para validación de JWT de Keycloak");
                    }
                }
            }
        }
        return cache.get();
    }

    private String readAlgorithm(String token) throws ParseException {
        JWT jwt = JWTParser.parse(token);
        JWSHeader header = (JWSHeader) jwt.getHeader();
        return header.getAlgorithm().getName();
    }

    private boolean isAsymmetricAlgorithm(String alg) {
        return alg != null && (alg.startsWith("RS") || alg.startsWith("ES") || alg.startsWith("PS"));
    }

    private boolean isHmacAlgorithm(String alg) {
        return alg != null && alg.startsWith("HS");
    }
}
