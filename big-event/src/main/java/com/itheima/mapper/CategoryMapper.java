package com.itheima.mapper;

import com.itheima.pojo.Category;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ClassName:CategoryMapper
 * Package:com.itheima.mapper
 * Description:
 * Num:
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/20 下午12:32
 * @Version 1.0
 **/
@Mapper
public interface CategoryMapper{
    //新增
    @Insert("insert into category(category_name,category_alias,create_user,create_time,update_time)"+
    "values(#{categoryName},#{categoryAlias},#{createUser},#{createTime},#{updateTime})")
    void add(Category category);

    @Select("select * from category where create_user=#{id}")
    List<Category> list(Integer id);

    @Select("select * from category where id=#{id}")
    Category findById(Integer id);

    //更新
    @Update("update category set category_name=#{categoryName},category_alias=#{categoryAlias},update_time=now() where id=#{id}")
    void update(Category category);

    //删除
    @Delete("delete from category where id=#{id}")
    void delete(Integer id);
}
