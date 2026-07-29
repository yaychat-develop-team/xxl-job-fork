package com.xxl.job.admin.web.xxlsso;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.sso.core.constant.Const;
import com.xxl.sso.core.exception.XxlSsoException;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.core.CollectionTool;
import com.xxl.tool.core.StringTool;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CloudflareAccessLoginInterceptor implements HandlerInterceptor {

    private final CloudflareAccessUserService cloudflareAccessUserService;
    private final CloudflareAccessJwtVerifier jwtVerifier;
    private final boolean enabled;

    public CloudflareAccessLoginInterceptor(
            CloudflareAccessUserService cloudflareAccessUserService,
            CloudflareAccessJwtVerifier jwtVerifier,
            @Value("${xxl.job.admin.cloudflare-access.enabled:false}") boolean enabled) {
        this.cloudflareAccessUserService = cloudflareAccessUserService;
        this.jwtVerifier = jwtVerifier;
        this.enabled = enabled;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled || isBypassedPath(request)) {
            return true;
        }
        if (isNativeLoginPath(request)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }

        HandlerMethod method = handler instanceof HandlerMethod ? (HandlerMethod) handler : null;
        XxlSso xxlSso = method != null ? method.getMethodAnnotation(XxlSso.class) : null;
        String permission = xxlSso != null ? xxlSso.permission() : null;
        String role = xxlSso != null ? xxlSso.role() : null;

        String username;
        try {
            username = jwtVerifier.verify(request.getHeader(CloudflareAccessJwtVerifier.ACCESS_JWT_HEADER));
        } catch (JwtException exception) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid cloudflare access identity");
            return false;
        }

        XxlJobUser xxlJobUser = cloudflareAccessUserService.loadOrCreate(username);
        LoginInfo loginInfo = buildLoginInfo(xxlJobUser);
        request.setAttribute(Const.XXL_SSO_USER, loginInfo);

        if (method == null) {
            return true;
        }

        if (!hasPermission(loginInfo, permission)) {
            throw new XxlSsoException("permission limit, current login-user does not have permission:" + permission);
        }
        if (!hasRole(loginInfo, role)) {
            throw new XxlSsoException("permission limit, current login-user does not have role:" + role);
        }

        return true;
    }

    private boolean isBypassedPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return matchesPath(servletPath, "/api") || matchesPath(servletPath, "/actuator/health");
    }

    private boolean isNativeLoginPath(HttpServletRequest request) {
        return matchesPath(request.getServletPath(), "/auth");
    }

    private boolean matchesPath(String servletPath, String prefix) {
        return servletPath != null && (servletPath.equals(prefix) || servletPath.startsWith(prefix + "/"));
    }

    private LoginInfo buildLoginInfo(XxlJobUser user) {
        LoginInfo loginInfo = new LoginInfo(String.valueOf(user.getId()), user.getToken());
        loginInfo.setUserName(user.getUsername());
        loginInfo.setRoleList(List.of(Consts.ADMIN_ROLE));
        Map<String, String> extraInfo = new HashMap<>();
        extraInfo.put("jobGroups", user.getPermission());
        loginInfo.setExtraInfo(extraInfo);
        return loginInfo;
    }

    private boolean hasPermission(LoginInfo loginInfo, String permission) {
        return StringTool.isBlank(permission)
                || (CollectionTool.isNotEmpty(loginInfo.getPermissionList()) && loginInfo.getPermissionList().contains(permission));
    }

    private boolean hasRole(LoginInfo loginInfo, String role) {
        return StringTool.isBlank(role)
                || (CollectionTool.isNotEmpty(loginInfo.getRoleList()) && loginInfo.getRoleList().contains(role));
    }
}
