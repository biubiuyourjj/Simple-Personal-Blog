package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName:Category
 * Package:com.itheima.pojo
 * Description:
 * Num:1
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/18 下午3:42
 * @Version 1.0
 **/
@Data
public class Category {
    /**
     * 主键ID
     */
    @NotNull(groups = Update.class)
    private Integer id;

    /**
     * 分类名称
     */
    @NotEmpty(groups = {Add.class,Update.class})
    private String categoryName;

    /**
     * 分类别名
     */
    @NotEmpty(groups = {Add.class,Update.class})
    private String categoryAlias;

    /**
     * 创建人ID（关联user表的id）
     */
    private Integer createUser;

    /**
     * 创建时间
     */
    @JsonFormat(pattern ="yyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @JsonFormat(pattern ="yyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    //没有校验分组，默认属于Default分组
    //分组之间可以继承，A extends B，那么A中拥有B所以的校验项

    public interface Add extends Default {}
    public interface Update{}
}
