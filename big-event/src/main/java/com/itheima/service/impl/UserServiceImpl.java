package com.itheima.service.impl;

import com.itheima.mapper.UserMapper;
import com.itheima.pojo.User;
import com.itheima.service.UserService;
import com.itheima.utils.Md5Util;
import com.itheima.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ClassName:UserServiceImpl
 * Package:com.itheima.service.impl
 * Description:
 * Num:
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/18 下午4:59
 * @Version 1.0
 **/
@Service
public class UserServiceImpl implements UserService{
    @Autowired
    private UserMapper userMapper;
    @Override
    public User findByUserName(String username) {
        User u=userMapper.findByUserName(username);
        return u;
    }

    @Override
    public void register(String username, String password) {
        //加密
        String mdSSring= Md5Util.getMD5String(password);
        //添加
        userMapper.add(username,mdSSring);
    }

    @Override
    public void update(User user){
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
    }
    @Override
    public void updateAvatar(String avatarUrl){
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer id=(Integer) map.get("id");
        userMapper.updateAvatar(avatarUrl,id);
    }

    @Override
    public void updatePWD(String newPwd){
        Map<String,Object> map=ThreadLocalUtil.get();
        Integer id=(Integer) map.get("id");
        userMapper.updatePWD(Md5Util.getMD5String(newPwd),id);
    }
}
