package com.xxl.job.admin.web.xxlsso;

import com.xxl.tool.core.StringTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class CloudflareAccessJwtVerifier {

    public static final String ACCESS_JWT_HEADER = "Cf-Access-Jwt-Assertion";
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+$");

    private final JwtDecoder jwtDecoder;
    private final String issuer;
    private final String audience;

    @Autowired
    public CloudflareAccessJwtVerifier(
            @Value("${xxl.job.admin.cloudflare-access.enabled:false}") boolean enabled,
            @Value("${xxl.job.admin.cloudflare-access.team-domain:}") String teamDomain,
            @Value("${xxl.job.admin.cloudflare-access.audience:}") String audience) {
        this(enabled ? buildDecoder(teamDomain) : null, normalizeTeamDomain(teamDomain), audience != null ? audience.trim() : "");
        if (enabled && StringTool.isBlank(this.audience)) {
            throw new IllegalArgumentException("Cloudflare Access audience is required when Cloudflare Access login is enabled");
        }
    }

    CloudflareAccessJwtVerifier(JwtDecoder jwtDecoder, String issuer, String audience) {
        this.jwtDecoder = jwtDecoder;
        this.issuer = normalizeTeamDomain(issuer);
        this.audience = audience != null ? audience.trim() : "";
    }

    public String verify(String token) {
        if (jwtDecoder == null) {
            throw new JwtException("Cloudflare Access JWT verifier is disabled");
        }
        if (StringTool.isBlank(token)) {
            throw new JwtException("Cloudflare Access JWT is required");
        }

        Jwt jwt = jwtDecoder.decode(token);
        if (!issuer.equals(jwt.getIssuer() != null ? jwt.getIssuer().toString() : null)) {
            throw new JwtException("Cloudflare Access JWT issuer mismatch");
        }
        if (!jwt.getAudience().contains(audience)) {
            throw new JwtException("Cloudflare Access JWT audience mismatch");
        }
        if (!"app".equals(jwt.getClaimAsString("type"))) {
            throw new JwtException("Cloudflare Access JWT type must be app");
        }

        String email = jwt.getClaimAsString("email");
        email = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        if (email.isEmpty() || email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new JwtException("Cloudflare Access JWT contains an invalid email");
        }
        return email;
    }

    private static JwtDecoder buildDecoder(String teamDomain) {
        String issuer = normalizeTeamDomain(teamDomain);
        if (StringTool.isBlank(issuer)) {
            throw new IllegalArgumentException("Cloudflare Access team domain is required when Cloudflare Access login is enabled");
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(issuer + "/cdn-cgi/access/certs")
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }

    private static String normalizeTeamDomain(String teamDomain) {
        String normalized = teamDomain != null ? teamDomain.trim() : "";
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.isEmpty() && !normalized.startsWith("https://")) {
            throw new IllegalArgumentException("Cloudflare Access team domain must use HTTPS");
        }
        return normalized;
    }
}
