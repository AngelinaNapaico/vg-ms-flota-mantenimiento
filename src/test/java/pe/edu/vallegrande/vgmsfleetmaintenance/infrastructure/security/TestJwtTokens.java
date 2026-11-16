package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Date;

/**
 * Test JWT token fixtures for offline security integration testing.
 * SEC-023-A, SEC-023-B compliant:
 * - Expired token with valid RS256 signature.
 * - Token with untrusted issuer.
 * - Malformed tokens (missing signature, corrupted payload).
 */
public final class TestJwtTokens {

    public static final String EXPIRED_TOKEN_WITH_VALID_SIGNATURE;
    public static final String WRONG_ISSUER_TOKEN_WITH_VALID_SIGNATURE;
    public static final String MALFORMED_TOKEN_TWO_PARTS = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyIn0";
    public static final String INVALID_BASE64_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.!!!invalid_base64!!!.signature";
    public static final String NO_SIGNATURE_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyIn0.";

    static {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            PrivateKey privateKey = kp.getPrivate();

            Date expiredDate = new Date(1609459200L * 1000);
            Date issuedDate = new Date(1609372800L * 1000);

            EXPIRED_TOKEN_WITH_VALID_SIGNATURE = Jwts.builder()
                    .setSubject("test-operator")
                    .setIssuer("http://localhost:8090/realms/PRS")
                    .setIssuedAt(issuedDate)
                    .setExpiration(expiredDate)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            Date futureDate = new Date(9999999999L * 1000);

            WRONG_ISSUER_TOKEN_WITH_VALID_SIGNATURE = Jwts.builder()
                    .setSubject("test-operator")
                    .setIssuer("http://unauthorized-issuer:8080/realms/OTHER")
                    .setIssuedAt(new Date())
                    .setExpiration(futureDate)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

        } catch (Exception e) {
            throw new ExceptionInInitializerError("Unable to initialize TestJwtTokens: " + e.getMessage());
        }
    }

    private TestJwtTokens() {}
}
