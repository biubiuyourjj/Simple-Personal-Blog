package com.itheima.exception;

import ch.qos.logback.core.util.StringUtil;
import com.itheima.pojo.Result;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ClassName:GlobalExceptionHandler
 * Package:com.itheima.exception
 * Description:全局异常处理
 * Num:3
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/18 下午7:53
 * @Version 1.0
 **/
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e){
        e.printStackTrace();
        return Result.error(StringUtils.hasLength(e.getMessage())? e.getMessage() : "操作失败");
    }
}
