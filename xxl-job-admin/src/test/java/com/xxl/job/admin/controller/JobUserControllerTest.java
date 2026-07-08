package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.biz.JobUserController;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.tool.response.Response;
import freemarker.template.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JobUserControllerTest {

    private FakeUserMapper xxlJobUserMapper;
    private JobUserController controller;

    @BeforeEach
    public void setup() throws Exception {
        I18nUtil i18nUtil = new I18nUtil();
        ReflectionTestUtils.setField(i18nUtil, "i18n", "zh_CN");
        ReflectionTestUtils.setField(i18nUtil, "configuration", new Configuration(Configuration.VERSION_2_3_34));
        i18nUtil.afterPropertiesSet();

        xxlJobUserMapper = new FakeUserMapper();
        controller = new JobUserController();
        ReflectionTestUtils.setField(controller, "xxlJobUserMapper", xxlJobUserMapper);
    }

    @Test
    public void insertAllowsFiftyCharacterUsername() throws Exception {
        String username = "u" + "a".repeat(49);
        XxlJobUser user = newUser(username);

        Response<String> response = controller.insert(user);

        assertEquals(200, response.getCode());
        assertTrue(xxlJobUserMapper.saveCalled);
    }

    @Test
    public void insertRejectsFiftyOneCharacterUsername() throws Exception {
        String username = "u" + "a".repeat(50);
        XxlJobUser user = newUser(username);

        Response<String> response = controller.insert(user);

        assertTrue(response.getMsg().contains("[4-50]"), response.getMsg());
        assertFalse(xxlJobUserMapper.saveCalled);
    }

    private XxlJobUser newUser(String username) {
        XxlJobUser user = new XxlJobUser();
        user.setUsername(username);
        user.setPassword("123456");
        user.setRole(0);
        return user;
    }

    private static class FakeUserMapper implements XxlJobUserMapper {
        private boolean saveCalled;

        @Override
        public List<XxlJobUser> pageList(int offset, int pagesize, String username, int role) {
            return List.of();
        }

        @Override
        public int pageListCount(int offset, int pagesize, String username, int role) {
            return 0;
        }

        @Override
        public XxlJobUser loadByUserName(String username) {
            return null;
        }

        @Override
        public XxlJobUser loadById(int id) {
            return null;
        }

        @Override
        public int save(XxlJobUser xxlJobUser) {
            saveCalled = true;
            return 1;
        }

        @Override
        public int update(XxlJobUser xxlJobUser) {
            return 0;
        }

        @Override
        public int delete(int id) {
            return 0;
        }

        @Override
        public int updateToken(int id, String token) {
            return 0;
        }
    }
}
