package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Test helper for generating cryptographic RSA test tokens and exposing
 * the matching RSA public key for real signature, expiration, and issuer validation tests.
 */
public final class TestJwtTokens {

    public static final String EXPECTED_ISSUER = "http://localhost:8090/realms/PRS";
    public static final RSAPublicKey TEST_PUBLIC_KEY;
    public static final String VALID_TOKEN;
    public static final String EXPIRED_TOKEN_WITH_VALID_SIGNATURE;
    public static final String WRONG_ISSUER_TOKEN_WITH_VALID_SIGNATURE;
    public static final String MALFORMED_TOKEN_TWO_PARTS;
    public static final String INVALID_BASE64_TOKEN;
    public static final String NO_SIGNATURE_TOKEN;

    private static String b64Url(String input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    static {
        MALFORMED_TOKEN_TWO_PARTS = b64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}") + "." + b64Url("{\"sub\":\"user\"}");
        INVALID_BASE64_TOKEN = b64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}") + ".!!!invalid_base64!!!.signature";
        NO_SIGNATURE_TOKEN = b64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}") + "." + b64Url("{\"sub\":\"user\"}") + ".";

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            PrivateKey privateKey = kp.getPrivate();
            TEST_PUBLIC_KEY = (RSAPublicKey) kp.getPublic();

            Date futureDate = new Date(System.currentTimeMillis() + 3_600_000L);

            VALID_TOKEN = Jwts.builder()
                    .setSubject("test-operator")
                    .setIssuer(EXPECTED_ISSUER)
                    .claim("preferred_username", "test-operator")
                    .claim("eid", "65f1a2b3c4d5e6f7a8b9c001")
                    .claim("roles", List.of("OPERADOR_ENTIDAD"))
                    .setIssuedAt(new Date())
                    .setExpiration(futureDate)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            Date expiredDate = new Date(1609459200L * 1000);
            Date issuedDate = new Date(1609372800L * 1000);

            EXPIRED_TOKEN_WITH_VALID_SIGNATURE = Jwts.builder()
                    .setSubject("test-operator")
                    .setIssuer(EXPECTED_ISSUER)
                    .claim("preferred_username", "test-operator")
                    .claim("roles", List.of("OPERADOR_ENTIDAD"))
                    .setIssuedAt(issuedDate)
                    .setExpiration(expiredDate)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            WRONG_ISSUER_TOKEN_WITH_VALID_SIGNATURE = Jwts.builder()
                    .setSubject("test-operator")
                    .setIssuer("http://unauthorized-issuer:8080/realms/OTHER")
                    .claim("preferred_username", "test-operator")
                    .claim("roles", List.of("OPERADOR_ENTIDAD"))
                    .setIssuedAt(new Date())
                    .setExpiration(futureDate)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test RSA keys", e);
        }
    }

    private TestJwtTokens() {}
}
