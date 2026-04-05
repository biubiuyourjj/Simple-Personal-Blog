package com.itheima.interceptor;

import com.itheima.pojo.Result;
import com.itheima.utils.JwtUtil;
import com.itheima.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * ClassName:LoginInterceptor
 * Package:com.itheima.interceptor
 * Description:
 * Num:
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/19 上午11:28
 * @Version 1.0
 **/

/**
 * 自定义拦截器
 * 实现 HandlerInterceptor 接口，重写需要的方法
 */

@Component//放入IOC容器
public class LoginInterceptor implements HandlerInterceptor {
    //重点理解
    private final StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 预处理方法：控制器方法执行前调用（核心拦截逻辑写在这里）
     * 返回 true：放行，继续执行控制器方法；返回 false：拦截，不再向下执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,Object handler) throws Exception{
        String token=request.getHeader("Authorization");
        try {

            //从redis中获取一模一样的token
            ValueOperations<String,String> operations = stringRedisTemplate.opsForValue();
            String redisToken = operations.get(token);
            if(redisToken==null){
                throw new RuntimeException();
            }
            Map<String,Object> claims= JwtUtil.parseToken(token);

            //把业务数据存储到ThreadLocal中
            ThreadLocalUtil.set(claims);
            //放行
            return true;
        } catch (Exception e) {
            //http响应状态码为401
            response.setStatus(401);
            //不放行
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //清空ThreadLocal中的数据
        ThreadLocalUtil.remove();
    }
}
