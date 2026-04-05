package com.itheima.anno;

import com.itheima.validation.StateValidation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * ClassName:state
 * Package:com.itheima.anno
 * Description:
 * Num:
 *
 * @Author SGG_蒋志凯
 * @Create 2026/2/12 下午3:57
 * @Version 1.0
 **/

@Documented//元注解

@Target({ElementType.FIELD})//元注解:用在属性上
@Constraint(validatedBy = {StateValidation.class})//指定提供校验规则的类
@Retention(RetentionPolicy.RUNTIME)//元注解:运行时生效
public @interface State {
    //提供校验失败的提示信息
    String message() default "state参数的值只能是已发布或草稿";
    //定义分组
    Class<?>[] groups() default {};
    //定义负载 获取到state注解的附加信息
    Class<? extends Payload>[] payload() default {};
}
