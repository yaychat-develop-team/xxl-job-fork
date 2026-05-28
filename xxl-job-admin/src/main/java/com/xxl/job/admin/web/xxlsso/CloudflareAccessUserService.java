package com.xxl.job.admin.web.xxlsso;

import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.crypto.Sha256Tool;
import com.xxl.tool.id.UUIDTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudflareAccessUserService {

    private final XxlJobUserMapper xxlJobUserMapper;
    private final int autoRegisterRole;

    public CloudflareAccessUserService(
            XxlJobUserMapper xxlJobUserMapper,
            @Value("${xxl.job.admin.cloudflare-access.auto-register-role:0}") int autoRegisterRole) {
        this.xxlJobUserMapper = xxlJobUserMapper;
        this.autoRegisterRole = autoRegisterRole;
    }

    public XxlJobUser loadOrCreate(String username) {
        if (StringTool.isBlank(username)) {
            throw new IllegalArgumentException("cloudflare access username can not be blank");
        }

        XxlJobUser existUser = xxlJobUserMapper.loadByUserName(username);
        if (existUser != null) {
            return existUser;
        }

        XxlJobUser xxlJobUser = new XxlJobUser();
        xxlJobUser.setUsername(username);
        xxlJobUser.setPassword(Sha256Tool.sha256(UUIDTool.getSimpleUUID()));
        xxlJobUser.setRole(autoRegisterRole);
        xxlJobUser.setPermission(null);
        xxlJobUserMapper.save(xxlJobUser);
        return xxlJobUser;
    }
}
