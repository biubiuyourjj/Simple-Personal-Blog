package com.itheima.mapper;

import com.itheima.pojo.Article;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * ClassName:ArticleMapper
 * Package:com.itheima.mapper
 * Description:
 * Num:
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/20 下午8:30
 * @Version 1.0
 **/
@Mapper
public interface ArticleMapper {
    //新增
    @Insert("insert into article(title,content,cover_img,state,category_id,create_user,create_time,update_time)"+
    "values(#{title},#{content},#{coverImg},#{state},#{categoryId},#{createUser},#{createTime},#{updateTime})")
    void add(Article article);

    //分页查询
    List<Article> list(Integer userId, Integer categoryId, String state);

    //获取选定文章
    @Select("select * from article where id=#{id}")
    Article getById(Integer id);

    //更新文章
    @Update("update article set title=#{title},content=#{content},cover_img=#{coverImg},state=#{state},category_id=#{categoryId},update_time=now() where id=#{id}")
    void update(Article article);

    //删除文章
    @Delete("delete from article where id=#{id}")
    void delete(Integer id);
}
