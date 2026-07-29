package com.xxl.job.admin.web.xxlsso;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CloudflareAccessJwtVerifierTest {

    private static final String ISSUER = "https://forya.cloudflareaccess.com";
    private static final String AUDIENCE = "xxl-job-audience";
    private StubJwtDecoder decoder;
    private CloudflareAccessJwtVerifier verifier;

    @BeforeEach
    void setup() {
        decoder = new StubJwtDecoder();
        verifier = new CloudflareAccessJwtVerifier(decoder, ISSUER, AUDIENCE);
    }

    @Test
    void springCreatesVerifierUsingConfiguredConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            TestPropertyValues.of(
                    "xxl.job.admin.cloudflare-access.enabled=false",
                    "xxl.job.admin.cloudflare-access.team-domain=",
                    "xxl.job.admin.cloudflare-access.audience=")
                    .applyTo(context);
            context.register(CloudflareAccessJwtVerifier.class);
            context.refresh();

            assertNotNull(context.getBean(CloudflareAccessJwtVerifier.class));
        }
    }

    @Test
    void acceptsValidApplicationTokenAndNormalizesEmail() {
        decoder.jwt = jwt(ISSUER, List.of(AUDIENCE), "app", " Alice@Example.com ");

        assertEquals("alice@example.com", verifier.verify("valid"));
    }

    @Test
    void rejectsWrongIssuer() {
        decoder.jwt = jwt("https://evil.cloudflareaccess.com", List.of(AUDIENCE), "app", "alice@example.com");

        assertThrows(JwtException.class, () -> verifier.verify("wrong-issuer"));
    }

    @Test
    void rejectsWrongAudience() {
        decoder.jwt = jwt(ISSUER, List.of("other"), "app", "alice@example.com");

        assertThrows(JwtException.class, () -> verifier.verify("wrong-audience"));
    }

    @Test
    void rejectsServiceToken() {
        decoder.jwt = jwt(ISSUER, List.of(AUDIENCE), "service", "alice@example.com");

        assertThrows(JwtException.class, () -> verifier.verify("service-token"));
    }

    @Test
    void rejectsApplicationTokenWithoutEmail() {
        decoder.jwt = jwt(ISSUER, List.of(AUDIENCE), "app", null);

        assertThrows(JwtException.class, () -> verifier.verify("missing-email"));
    }

    @Test
    void rejectsMissingAssertionHeader() {
        assertThrows(JwtException.class, () -> verifier.verify(null));
    }

    @Test
    void rejectsEmailLongerThanDatabaseLimit() {
        String longEmail = "a".repeat(250) + "@example.com";
        decoder.jwt = jwt(ISSUER, List.of(AUDIENCE), "app", longEmail);

        assertThrows(JwtException.class, () -> verifier.verify("long-email"));
    }

    @Test
    void propagatesSignatureOrTimestampValidationFailure() {
        decoder.failure = new JwtException("expired");

        assertThrows(JwtException.class, () -> verifier.verify("expired"));
    }

    @Test
    void propagatesNonRs256ValidationFailure() {
        decoder.failure = new JwtException("unsupported algorithm");

        assertThrows(JwtException.class, () -> verifier.verify("hs256"));
    }

    private Jwt jwt(String issuer, List<String> audience, String type, String email) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer(issuer)
                .audience(audience)
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("type", type);
        if (email != null) {
            builder.claims(claims -> claims.putAll(Map.of("email", email)));
        }
        return builder.build();
    }

    private static class StubJwtDecoder implements JwtDecoder {
        private Jwt jwt;
        private JwtException failure;

        @Override
        public Jwt decode(String token) {
            if (failure != null) {
                throw failure;
            }
            return jwt;
        }
    }
}
