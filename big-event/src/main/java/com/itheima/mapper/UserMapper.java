package com.itheima.mapper;

import com.itheima.pojo.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * ClassName:UserMapper
 * Package:com.itheima.mapper
 * Description:
 * Num:
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/18 下午5:00
 * @Version 1.0
 **/

@Mapper
public interface UserMapper {

    //添加
    @Insert("insert into user(username,password,create_time,update_time)"+
            "values(#{username},#{password},now(),now())")
    void add(String username, String password);

    //根据用户名查询用户
    @Select("select * from user where username=#{username}")
    User findByUserName(String username);

    @Update("update user set nickname=#{nickname},email=#{email},update_time=#{updateTime} where id=#{id}")
    void update(User user);

    @Update("update  user set user_pic=#{avatarUrl},update_time=now() where id=#{id}")
    void updateAvatar(String avatarUrl,Integer id);

    @Update("update user set password=#{newPwd},update_time=now() where id=#{id}")
    void updatePWD(String newPwd,Integer id);
}
