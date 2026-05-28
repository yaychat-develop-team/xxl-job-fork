package com.xxl.job.admin.web.xxlsso;

import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudflareAccessUserServiceTest {

    @Test
    void loadOrCreateCreatesUserWithConfiguredRole() {
        XxlJobUserMapper mapper = mock(XxlJobUserMapper.class);
        CloudflareAccessUserService service = new CloudflareAccessUserService(mapper, 1);

        XxlJobUser user = service.loadOrCreate("alice@example.com");

        assertEquals("alice@example.com", user.getUsername());
        assertEquals(1, user.getRole());
        assertNotNull(user.getPassword());
        verify(mapper).save(any(XxlJobUser.class));
    }

    @Test
    void loadOrCreateReusesExistingUserWithoutChangingRole() {
        XxlJobUserMapper mapper = mock(XxlJobUserMapper.class);
        XxlJobUser existing = new XxlJobUser();
        existing.setId(7);
        existing.setUsername("bob@example.com");
        existing.setRole(0);
        when(mapper.loadByUserName("bob@example.com")).thenReturn(existing);
        CloudflareAccessUserService service = new CloudflareAccessUserService(mapper, 1);

        XxlJobUser user = service.loadOrCreate("bob@example.com");

        assertEquals(7, user.getId());
        assertEquals(0, user.getRole());
    }
}
