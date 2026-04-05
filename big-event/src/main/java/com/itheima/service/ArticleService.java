package com.itheima.service;

import com.itheima.pojo.Article;
import com.itheima.pojo.PageBean;

/**
 * ClassName:ArticleService
 * Package:com.itheima.service
 * Description:
 * Num:
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/20 下午8:30
 * @Version 1.0
 **/
public interface ArticleService {
    void add(Article article);//新增文章


    //条件分页列表查询
    PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state);

    Article getById(Integer id);

    void update(Article article);

    void delete(Integer id);
}
