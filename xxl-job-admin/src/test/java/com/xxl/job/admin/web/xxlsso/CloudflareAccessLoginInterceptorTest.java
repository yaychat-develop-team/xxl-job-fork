package com.xxl.job.admin.web.xxlsso;

import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.sso.core.constant.Const;
import com.xxl.sso.core.model.LoginInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudflareAccessLoginInterceptorTest {

    private static final String ISSUER = "https://forya.cloudflareaccess.com";
    private static final String AUDIENCE = "xxl-job-audience";
    private FakeXxlJobUserMapper mapper;
    private CountingJwtDecoder decoder;
    private CloudflareAccessLoginInterceptor interceptor;

    @BeforeEach
    void setup() {
        mapper = new FakeXxlJobUserMapper();
        CloudflareAccessUserService userService = new CloudflareAccessUserService(mapper, 1);
        decoder = new CountingJwtDecoder();
        CloudflareAccessJwtVerifier jwtVerifier = new CloudflareAccessJwtVerifier(decoder, ISSUER, AUDIENCE);
        interceptor = new CloudflareAccessLoginInterceptor(userService, jwtVerifier, true);
    }

    @Test
    void preHandleLogsInVerifiedCloudflareUserAsAdministrator() throws Exception {
        XxlJobUser user = new XxlJobUser();
        user.setId(12);
        user.setUsername("alice@example.com");
        user.setRole(0);
        mapper.put(user);
        decoder.jwt = jwt("alice@example.com");

        MockHttpServletRequest request = request("GET", "/", "signed-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));

        LoginInfo loginInfo = (LoginInfo) request.getAttribute(Const.XXL_SSO_USER);
        assertEquals("alice@example.com", loginInfo.getUserName());
        assertTrue(loginInfo.getRoleList().contains("ADMIN"));
        assertEquals(0, mapper.getSaveCount());
    }

    @Test
    void preHandleRejectsMissingOrInvalidJwt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));

        assertEquals(401, response.getStatus());
        assertNull(request.getAttribute(Const.XXL_SSO_USER));
        assertEquals(0, mapper.getSaveCount());
    }

    @Test
    void preHandleBypassesOnlyOpenApiAndHealthPaths() throws Exception {
        for (String path : new String[]{"/api", "/api/registry", "/actuator/health", "/actuator/health/readiness"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            request.setServletPath(path);
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertTrue(interceptor.preHandle(request, response, new Object()), path);
        }
        assertEquals(0, decoder.decodeCount);
    }

    @Test
    void preHandleDoesNotBypassSimilarPathPrefixes() throws Exception {
        for (String path : new String[]{"/apiary", "/actuator/healthcheck"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            request.setServletPath(path);
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertFalse(interceptor.preHandle(request, response, new Object()), path);
            assertEquals(401, response.getStatus(), path);
        }
    }

    @Test
    void preHandleDisablesNativeLoginEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/doLogin");
        request.setServletPath("/auth/doLogin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));

        assertEquals(404, response.getStatus());
        assertEquals(0, decoder.decodeCount);
    }

    private MockHttpServletRequest request(String method, String path, String jwt) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.addHeader(CloudflareAccessJwtVerifier.ACCESS_JWT_HEADER, jwt);
        return request;
    }

    private Jwt jwt(String email) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE))
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("type", "app")
                .claim("email", email)
                .build();
    }

    private static class CountingJwtDecoder implements JwtDecoder {
        private Jwt jwt;
        private JwtException failure;
        private int decodeCount;

        @Override
        public Jwt decode(String token) {
            decodeCount++;
            if (failure != null) {
                throw failure;
            }
            return jwt;
        }
    }
}
