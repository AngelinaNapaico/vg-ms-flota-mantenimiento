package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("JwtDecoderConfigTest - Cryptographic Expiration, Issuer & Production Guardrails")
class JwtDecoderConfigTest {

    private static HttpServer jwksServer;
    private static String jwksUri;

    @BeforeAll
    static void startJwksServer() throws IOException {
        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwksServer.createContext("/.well-known/jwks.json", exchange -> {
            byte[] body = TestJwtTokens.JWKS_JSON.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        jwksServer.start();
        jwksUri = "http://localhost:" + jwksServer.getAddress().getPort() + "/.well-known/jwks.json";
    }

    @AfterAll
    static void stopJwksServer() {
        if (jwksServer != null) {
            jwksServer.stop(0);
        }
    }

    @Test
    @DisplayName("Should accept valid JWT and reject expired, wrong-issuer, and untrusted-key JWTs")
    void shouldValidateTokensUsingRealDecoderBean() {
        JwtDecoderConfig config = new JwtDecoderConfig();
        ReflectionTestUtils.setField(config, "issuerUri", TestJwtTokens.EXPECTED_ISSUER);
        ReflectionTestUtils.setField(config, "jwkSetUri", jwksUri);

        ReactiveJwtDecoder decoder = config.jwtDecoderDevelopment();

        StepVerifier.create(decoder.decode(TestJwtTokens.VALID_TOKEN))
                .expectNextMatches(jwt -> "test-operator".equals(jwt.getSubject()))
                .verifyComplete();

        StepVerifier.create(decoder.decode(TestJwtTokens.EXPIRED_TOKEN_WITH_VALID_SIGNATURE))
                .expectError(BadCredentialsException.class)
                .verify();

        StepVerifier.create(decoder.decode(TestJwtTokens.WRONG_ISSUER_TOKEN_WITH_VALID_SIGNATURE))
                .expectError(BadCredentialsException.class)
                .verify();

        StepVerifier.create(decoder.decode(TestJwtTokens.UNTRUSTED_SIGNATURE_TOKEN))
                .expectError(BadCredentialsException.class)
                .verify();
    }

    @Test
    @DisplayName("Should enforce HTTPS non-local issuer and JWKS URIs in production validation")
    void shouldValidateProductionIssuerGuardrails() {
        JwtDecoderConfig invalidLocal = new JwtDecoderConfig();
        ReflectionTestUtils.setField(invalidLocal, "issuerUri", "http://localhost:8090/realms/PRS");
        ReflectionTestUtils.setField(invalidLocal, "jwkSetUri", "https://auth.example.com/certs");
        assertThrows(IllegalStateException.class, invalidLocal::validateProductionIssuer);

        JwtDecoderConfig invalidHttp = new JwtDecoderConfig();
        ReflectionTestUtils.setField(invalidHttp, "issuerUri", "https://auth.example.com/realms/PRS");
        ReflectionTestUtils.setField(invalidHttp, "jwkSetUri", "http://auth.example.com/certs");
        assertThrows(IllegalStateException.class, invalidHttp::validateProductionIssuer);

        JwtDecoderConfig validProd = new JwtDecoderConfig();
        ReflectionTestUtils.setField(validProd, "issuerUri", "https://auth.example.com/realms/PRS");
        ReflectionTestUtils.setField(validProd, "jwkSetUri", "https://auth.example.com/realms/PRS/protocol/openid-connect/certs");
        assertDoesNotThrow(validProd::validateProductionIssuer);
    }
}
