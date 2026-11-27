package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.math.BigInteger;
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
 * the matching RSA public key and JWKS JSON payload for real signature,
 * expiration, issuer, and role validation tests.
 */
public final class TestJwtTokens {

    public static final String EXPECTED_ISSUER = "http://localhost:8090/realms/PRS";
    public static final String KEY_ID = "test-rsa-key-1";
    public static final RSAPublicKey TEST_PUBLIC_KEY;
    public static final String JWKS_JSON;
    public static final String VALID_TOKEN;
    public static final String VALID_CITIZEN_TOKEN;
    public static final String VALID_ANALYST_TOKEN;
    public static final String VALID_MUNICIPALITY_ADMIN_TOKEN;
    public static final String EXPIRED_TOKEN_WITH_VALID_SIGNATURE;
    public static final String WRONG_ISSUER_TOKEN_WITH_VALID_SIGNATURE;
    public static final String UNTRUSTED_SIGNATURE_TOKEN;
    public static final String MALFORMED_TOKEN_TWO_PARTS;
    public static final String INVALID_BASE64_TOKEN;
    public static final String NO_SIGNATURE_TOKEN;

    private static String b64Url(String input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String b64UrlUnsigned(BigInteger value) {
        byte[] raw = value.toByteArray();
        if (raw.length > 1 && raw[0] == 0) {
            byte[] trimmed = new byte[raw.length - 1];
            System.arraycopy(raw, 1, trimmed, 0, trimmed.length);
            raw = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
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

            KeyPair untrustedKp = kpg.generateKeyPair();
            PrivateKey untrustedPrivateKey = untrustedKp.getPrivate();

            JWKS_JSON = String.format(
                    "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"%s\",\"n\":\"%s\",\"e\":\"%s\"}]}",
                    KEY_ID,
                    b64UrlUnsigned(TEST_PUBLIC_KEY.getModulus()),
                    b64UrlUnsigned(TEST_PUBLIC_KEY.getPublicExponent()));

            Date futureDate = new Date(System.currentTimeMillis() + 3_600_000L);

            VALID_TOKEN = Jwts.builder()
                    .setHeaderParam("kid", KEY_ID)
                    .setSubject("test-operator")
                    .setIssuer(EXPECTED_ISSUER)
                    .claim("preferred_username", "test-operator")
                    .claim("eid", "65f1a2b3c4d5e6f7a8b9c001")
                    .claim("roles", List.of("OPERADOR_ENTIDAD"))
                    .setIssuedAt(new Date())
                    .setExpiration(futureDate)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            VALID_CITIZEN_TOKEN = Jwts.builder()
                    .setHeaderParam("kid", KEY_ID)
                    .setSubject("test-citizen")
                    .setIssuer(EXPECTED_ISSUER)
                    .claim("preferred_username", "test-citizen")
                    .claim("eid", "65f1a2b3c4d5e6f7a8b9c001")
                    .claim("roles", List.of("CIUDADANO"))
                    .setIssuedAt(new Date())
                    .setExpiration(futureDate)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            VALID_ANALYST_TOKEN = Jwts.builder()
                    .setHeaderParam("kid", KEY_ID)
                    .setSubject("test-analyst")
                    .setIssuer(EXPECTED_ISSUER)
                    .claim("preferred_username", "test-analyst")
                    .claim("eid", "65f1a2b3c4d5e6f7a8b9c001")
                    .claim("roles", List.of("ANALISTA"))
                    .setIssuedAt(new Date())
                    .setExpiration(futureDate)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            VALID_MUNICIPALITY_ADMIN_TOKEN = Jwts.builder()
                    .setHeaderParam("kid", KEY_ID)
                    .setSubject("test-municipality-admin")
                    .setIssuer(EXPECTED_ISSUER)
                    .claim("preferred_username", "test-municipality-admin")
                    .claim("eid", "65f1a2b3c4d5e6f7a8b9c001")
                    .claim("roles", List.of("MUNICIPALIDAD_ADMIN"))
                    .setIssuedAt(new Date())
                    .setExpiration(futureDate)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            Date expiredDate = new Date(1609459200L * 1000);
            Date issuedDate = new Date(1609372800L * 1000);

            EXPIRED_TOKEN_WITH_VALID_SIGNATURE = Jwts.builder()
                    .setHeaderParam("kid", KEY_ID)
                    .setSubject("test-operator")
                    .setIssuer(EXPECTED_ISSUER)
                    .claim("preferred_username", "test-operator")
                    .claim("roles", List.of("OPERADOR_ENTIDAD"))
                    .setIssuedAt(issuedDate)
                    .setExpiration(expiredDate)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            WRONG_ISSUER_TOKEN_WITH_VALID_SIGNATURE = Jwts.builder()
                    .setHeaderParam("kid", KEY_ID)
                    .setSubject("test-operator")
                    .setIssuer("http://unauthorized-issuer:8080/realms/OTHER")
                    .claim("preferred_username", "test-operator")
                    .claim("roles", List.of("OPERADOR_ENTIDAD"))
                    .setIssuedAt(new Date())
                    .setExpiration(futureDate)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            UNTRUSTED_SIGNATURE_TOKEN = Jwts.builder()
                    .setHeaderParam("kid", KEY_ID)
                    .setSubject("test-operator")
                    .setIssuer(EXPECTED_ISSUER)
                    .claim("preferred_username", "test-operator")
                    .claim("eid", "65f1a2b3c4d5e6f7a8b9c001")
                    .claim("roles", List.of("OPERADOR_ENTIDAD"))
                    .setIssuedAt(new Date())
                    .setExpiration(futureDate)
                    .signWith(untrustedPrivateKey, SignatureAlgorithm.RS256)
                    .compact();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test RSA keys", e);
        }
    }

    private TestJwtTokens() {}
}
