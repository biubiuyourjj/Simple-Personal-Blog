package com.itheima.validation;

import com.itheima.anno.State;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * ClassName:StateValidation
 * Package:com.itheima.validation
 * Description:自定义的校验器
 * Num:
 *
 * @Author SGG_蒋志凯
 * @Create 2026/2/12 下午4:06
 * @Version 1.0
 **/
//创建一个自定义的校验器
public class StateValidation implements ConstraintValidator<State,String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value ==null){
            return false;
        }
        if(value.equals("已发布") || value.equals("草稿")){
            return true;
        }
        return false;

    }
}
