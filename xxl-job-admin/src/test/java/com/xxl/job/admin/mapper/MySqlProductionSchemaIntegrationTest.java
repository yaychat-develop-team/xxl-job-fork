package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobUser;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
class MySqlProductionSchemaIntegrationTest {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private XxlJobUserMapper userMapper;

    @Resource
    private XxlJobLockMapper lockMapper;

    @Test
    @Transactional
    void productionSchemaSupportsMapperCrudAndScheduleLock() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()",
                Integer.class);
        assertEquals(8, tableCount);
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM xxl_job_group", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM xxl_job_info", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM xxl_job_user", Integer.class));
        assertEquals("schedule_lock", lockMapper.scheduleLock());

        String email = "a".repeat(242) + "@example.com";
        XxlJobUser user = new XxlJobUser();
        user.setUsername(email);
        user.setPassword("not-a-login-password");
        user.setRole(1);
        user.setPermission(null);

        assertEquals(1, userMapper.save(user));
        assertNotNull(user.getId());
        assertEquals(email, userMapper.loadByUserName(email).getUsername());
        assertEquals(1, userMapper.updateToken(user.getId(), "test-token"));
        assertEquals("test-token", userMapper.loadById(user.getId()).getToken());
        assertEquals(1, userMapper.delete(user.getId()));
    }
}
