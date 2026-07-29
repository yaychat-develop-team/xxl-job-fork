package com.xxl.job.admin.web.xxlsso;

import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobUser;
import org.springframework.dao.DuplicateKeyException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FakeXxlJobUserMapper implements XxlJobUserMapper {

    private final Map<String, XxlJobUser> users = new HashMap<>();
    private XxlJobUser concurrentUser;
    private boolean failNextSaveWithDuplicate;
    private int saveCount;

    void put(XxlJobUser user) {
        users.put(user.getUsername(), user);
    }

    void failNextSaveWithDuplicate(XxlJobUser user) {
        concurrentUser = user;
        failNextSaveWithDuplicate = true;
    }

    int getSaveCount() {
        return saveCount;
    }

    @Override
    public List<XxlJobUser> pageList(int offset, int pagesize, String username, int role) {
        return List.copyOf(users.values());
    }

    @Override
    public int pageListCount(int offset, int pagesize, String username, int role) {
        return users.size();
    }

    @Override
    public XxlJobUser loadByUserName(String username) {
        return users.get(username);
    }

    @Override
    public XxlJobUser loadById(int id) {
        return users.values().stream().filter(user -> user.getId() == id).findFirst().orElse(null);
    }

    @Override
    public int save(XxlJobUser user) {
        saveCount++;
        if (failNextSaveWithDuplicate) {
            failNextSaveWithDuplicate = false;
            users.put(concurrentUser.getUsername(), concurrentUser);
            throw new DuplicateKeyException("duplicate");
        }
        if (user.getId() == 0) {
            user.setId(users.size() + 1);
        }
        users.put(user.getUsername(), user);
        return 1;
    }

    @Override
    public int update(XxlJobUser user) {
        users.put(user.getUsername(), user);
        return 1;
    }

    @Override
    public int delete(int id) {
        XxlJobUser user = loadById(id);
        if (user == null) {
            return 0;
        }
        users.remove(user.getUsername());
        return 1;
    }

    @Override
    public int updateToken(int id, String token) {
        XxlJobUser user = loadById(id);
        if (user == null) {
            return 0;
        }
        user.setToken(token);
        return 1;
    }
}
