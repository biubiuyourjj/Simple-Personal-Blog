package com.itheima.controller;

import com.itheima.pojo.Result;
import com.itheima.pojo.User;
import com.itheima.service.UserService;
import com.itheima.utils.JwtUtil;
import com.itheima.utils.Md5Util;
import com.itheima.utils.ThreadLocalUtil;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ClassName:UserController
 * Package:com.itheima.controller
 * Description:
 * Num:
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/18 下午4:57
 * @Version 1.0
 **/
@RestController
@RequestMapping("/user")
@Validated
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private String username;

    @PostMapping("/register")
    public Result register(@Pattern(regexp ="^\\S{5,16}$") String username,@Pattern(regexp ="^\\S{5,16}$") String password){
        this.username = username;

        User u=userService.findByUserName(username);
        if(u==null){
            userService.register(username,password);
            return Result.success();
        }else{
            return  Result.error("用户名已被占用");
        }
    }

    @PostMapping("/login")
    public Result<String> login(@Pattern(regexp ="^\\S{5,16}$") String username,@Pattern(regexp ="^\\S{5,16}$") String password){
        //根据用户名查询用户
        User loginUser= userService.findByUserName(username);
        //该用户名是否存在
        if(loginUser==null){
            return Result.error("用户名错误");

        }

        //判断密码是否正确
        if(Md5Util.getMD5String(password).equals(loginUser.getPassword())){
            //登录成功
            Map<String,Object> claims=new HashMap<>();
            claims.put("id",loginUser.getId());
            claims.put("username",loginUser.getUsername());
            String token=JwtUtil.genToken(claims);
            //把token存到redis中
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            operations.set(token,token,1, TimeUnit.HOURS);

            return Result.success(token);
        }
        return Result.error("密码错误！");
    }

    @GetMapping("/userInfo")//获取用户信息
    public Result<User> userInfo(){
       /* Map<String,Object> map=JwtUtil.parseToken(token);
        String username=(String) map.get("username");*/
        Map<String,Object> map=ThreadLocalUtil.get();
        String username=(String) map.get("username");
        User user= userService.findByUserName(username);
        return Result.success(user);
    }

    @PutMapping("/update")
    public Result update(@RequestBody @Validated User user ){
        userService.update(user);
        return Result.success();
    }
    @PatchMapping("updateAvatar")
    public Result updateAvatar(@RequestParam @URL String avatarUrl){
        userService.updateAvatar(avatarUrl);
        return Result.success();
    }

    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String,Object> params,@RequestHeader("Authorization") String token){
        //校验参数
        String oldPwd= (String) params.get("old_pwd");
        String newPwd= (String) params.get("new_pwd");
        String rePwd= (String) params.get("re_pwd");
        if(!StringUtils.hasLength(oldPwd) || !StringUtils.hasLength(newPwd) || !StringUtils.hasLength(rePwd))
        {return Result.error("缺少必要参数！");}
        //原密码是否正确
        //调用userService根据用户名拿到原密码，再和old_pwd比对
        Map<String,Object> map=ThreadLocalUtil.get();
        String username=(String)map.get("username");
        User loginUser= userService.findByUserName(username);
        if(!loginUser.getPassword().equals(Md5Util.getMD5String(oldPwd))){
            return Result.error("原密码填写不正确");
        }
        //newPwd和repwd是否一样
        if(!rePwd.equals(newPwd)){
            return Result.error("两次填写的新密码不一样！");

        }
        //调用service实现数据更新
        userService.updatePWD(newPwd);
        //删除redis对应的token
        stringRedisTemplate.delete(token);
        /*
        ValueOperations<String,String> operations=stringRedisTemplate.opsForValue();
        operations.getOperations().delete(token);*/
        return Result.success();

    }

}
