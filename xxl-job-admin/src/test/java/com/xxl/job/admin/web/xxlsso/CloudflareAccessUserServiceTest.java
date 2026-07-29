package com.xxl.job.admin.web.xxlsso;

import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CloudflareAccessUserServiceTest {

    @Test
    void loadOrCreateCreatesUserWithConfiguredRole() {
        FakeXxlJobUserMapper mapper = new FakeXxlJobUserMapper();
        CloudflareAccessUserService service = new CloudflareAccessUserService(mapper, 1);

        XxlJobUser user = service.loadOrCreate("alice@example.com");

        assertEquals("alice@example.com", user.getUsername());
        assertEquals(1, user.getRole());
        assertNotNull(user.getPassword());
        assertEquals(1, mapper.getSaveCount());
    }

    @Test
    void loadOrCreateReusesExistingUserWithoutChangingRole() {
        FakeXxlJobUserMapper mapper = new FakeXxlJobUserMapper();
        XxlJobUser existing = new XxlJobUser();
        existing.setId(7);
        existing.setUsername("bob@example.com");
        existing.setRole(0);
        mapper.put(existing);
        CloudflareAccessUserService service = new CloudflareAccessUserService(mapper, 1);

        XxlJobUser user = service.loadOrCreate("bob@example.com");

        assertEquals(7, user.getId());
        assertEquals(0, user.getRole());
        assertEquals(0, mapper.getSaveCount());
    }

    @Test
    void loadOrCreateReloadsUserAfterConcurrentInsert() {
        FakeXxlJobUserMapper mapper = new FakeXxlJobUserMapper();
        XxlJobUser concurrent = new XxlJobUser();
        concurrent.setId(9);
        concurrent.setUsername("alice@example.com");
        mapper.failNextSaveWithDuplicate(concurrent);
        CloudflareAccessUserService service = new CloudflareAccessUserService(mapper, 1);

        XxlJobUser user = service.loadOrCreate("alice@example.com");

        assertEquals(9, user.getId());
    }
}
