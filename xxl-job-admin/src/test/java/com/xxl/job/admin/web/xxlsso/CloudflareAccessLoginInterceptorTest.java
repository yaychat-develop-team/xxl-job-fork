package com.xxl.job.admin.web.xxlsso;

import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.sso.core.constant.Const;
import com.xxl.sso.core.model.LoginInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudflareAccessLoginInterceptorTest {

    private CloudflareAccessUserService userService;
    private CloudflareAccessLoginInterceptor interceptor;

    @BeforeEach
    void setup() {
        userService = mock(CloudflareAccessUserService.class);
        interceptor = new CloudflareAccessLoginInterceptor(userService, true, "Cf-Access-Authenticated-User-Email");
    }

    @Test
    void preHandleLogsInCloudflareUserForCurrentRequest() throws Exception {
        XxlJobUser user = new XxlJobUser();
        user.setId(12);
        user.setUsername("alice@example.com");
        when(userService.loadOrCreate("alice@example.com")).thenReturn(user);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Cf-Access-Authenticated-User-Email", "alice@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());

        assertEquals("alice@example.com", ((LoginInfo) request.getAttribute(Const.XXL_SSO_USER)).getUserName());
        verify(userService).loadOrCreate("alice@example.com");
    }

    @Test
    void preHandleDoesNothingWhenCloudflareHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());

        assertNull(request.getAttribute(Const.XXL_SSO_USER));
    }
}
