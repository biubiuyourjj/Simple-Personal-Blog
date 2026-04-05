package com.itheima.config;

import com.itheima.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Watchable;

/**
 * ClassName:WebConfig
 * Package:com.itheima.config
 * Description:注册
 * Num:5
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/19 上午11:36
 * @Version 1.0
 **/

/**
 * 拦截器配置类
 * 实现 WebMvcConfigurer 接口，重写 addInterceptors 方法注册拦截器
 */
@Configuration//注入IOC容器
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //登录注册接口不拦截
        registry.addInterceptor(loginInterceptor).excludePathPatterns("/user/login","/user/register");
    }
}
