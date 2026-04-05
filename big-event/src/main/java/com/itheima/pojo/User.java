package com.itheima.pojo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * ClassName:User
 * Package:com.itheima.pojo
 * Description:
 * Num:1
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/18 下午3:32
 * @Version 1.0
 **/
@Data//lombok的getter和setter方法
public class User {
    /**
     * 主键ID
     */
    @NotNull
    private Integer id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    //导入包要注意
    @JsonIgnore//让springmvc把当前对象转换成json字符串的时候，忽略password，最终的json字符串中就没有password这个属性
    private String password;
    /*
    * 昵称
    */
    @NotEmpty
    @Pattern(regexp = "^\\S{1,10}$")
    private String nickname;

    /**
     * 邮箱
     */
    @NotEmpty
    @Email
    private String email;

    /**
     * 头像
     */
    private String userPic;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
}
