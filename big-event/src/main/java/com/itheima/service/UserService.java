package com.itheima.service;

import com.itheima.pojo.User;

/**
 * ClassName:UserService
 * Package:com.itheima.service.impl
 * Description:
 * Num:
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/18 下午4:58
 * @Version 1.0
 **/
public interface UserService {
    //根据用户名查询用户
    User findByUserName(String username);

    void register(String username, String password);
    //更新
    void update(User user);

    //更新头像
    void updateAvatar(String avatarUrl);

    //更新密码
    void updatePWD(String newPwd);
}
