package com.itheima;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ClassName:jwttest
 * Package:com.itheima
 * Description:
 * Num:4
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/19 上午10:06
 * @Version 1.0
 **/

public class jwtTest {
//    @Test
    public void testGen(){
        Map<String, Object> claims=new HashMap<>();
        claims.put("id",1);
        claims.put("username","张三");

        //生成gwt的代码
        String token = JWT.create()
                .withClaim("user",claims)//添加载荷
                .withExpiresAt(new Date(System.currentTimeMillis()+1000*60*60*12))//添加过期时间
                .sign(Algorithm.HMAC256("itheima"));//指定算法，配置密钥

        System.out.println(token);
    }

//    @Test
    public void testParse(){
        //定义字符串，模拟用户传递过来的token
        String token ="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"+
                ".eyJ1c2VyIjp7ImlkIjoxLCJ1c2VybmFtZSI6IuW8oOS4iSJ9LCJleHAiOjE3Njg4MzIzNzJ9"+
                ".IMdy9nY3Uf61AqpLHYHNUH4HW5HkfRFnCkvI7O6MPvI";
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256("itheima")).build();
        DecodedJWT decodedJWT = jwtVerifier.verify(token);//验证token，生成一个解析后的JWT对象
        Map<String, Claim> claims = decodedJWT.getClaims();
        System.out.println(claims.get("user"));

        //如果篡改头部和载荷验证失败
        //密钥不对，验证失败
        //过期失败
    }
}
