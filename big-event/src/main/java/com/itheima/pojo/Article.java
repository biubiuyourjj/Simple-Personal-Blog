package com.itheima.pojo;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.groups.Default;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

/**
 * ClassName:Article
 * Package:com.itheima.pojo
 * Description:
 * Num:1
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/18 下午3:42
 * @Version 1.0
 * 文章表实体类
 * 对应数据库中的article表
 */
@Data
public class Article {
    /**
     * 主键ID
     */
    @NotNull(groups = Update.class)
    private Integer id;

    /**
     * 文章标题
     */
    @NotEmpty(groups = {Add.class, Update.class})
    @Pattern(regexp = "^\\S{1,10}$")
    private String title;

    /**
     * 文章内容
     */
    @NotEmpty(groups = {Add.class,Update.class})
    private String content;

    /**
     * 文章封面
     */
    @NotEmpty
    @URL
    private String coverImg;

    /**
     * 文章状态：已发布/草稿
     */
    @Pattern(regexp = "^(已发布|草稿)$")
    private String state;

    /**
     * 文章分类ID（关联category表的id）
     */
    @NotNull
    private Integer categoryId;

    /**
     * 创建人ID（关联user表的id）
     */
    private Integer createUser;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    public interface Update{}

    public interface Add extends Default {}
}
