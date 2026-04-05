package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.ArticleMapper;
import com.itheima.pojo.Article;
import com.itheima.pojo.PageBean;
import com.itheima.service.ArticleService;
import com.itheima.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ClassName:ArticleServiceImpl
 * Package:com.itheima.service.impl
 * Description:
 * Num:
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/20 下午8:30
 * @Version 1.0
 **/
@Service
public class ArticleServiceImpl implements ArticleService {
    @Autowired
    private ArticleMapper articleMapper;
    @Override
    public void add(Article article){
        article.setUpdateTime(LocalDateTime.now());
        article.setCreateTime(LocalDateTime.now());
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId=(Integer) map.get("id");
        article.setCreateUser(userId);
        articleMapper.add(article);
    }

    @Override
    public PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state) {
        //1.创建PageBean对象
        PageBean<Article> pb = new PageBean();
        //2.开启分页查询PageHalper
        PageHelper.startPage(pageNum, pageSize);

        //3.调用mapper
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        List<Article> as = articleMapper.list(userId, categoryId, state);
        //page中提供了方法，可以获取PageHelper分页查询后得到的总记录条数和当前页数
        Page<Article> p = (Page<Article>) as;
        //把数据填充到PageBean对象中
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    public Article getById(Integer id) {
        return articleMapper.getById(id);
    }

    @Override
    public void update(Article article) {
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.update(article);
    }

    @Override
    public void delete(Integer id) {
        articleMapper.delete(id);
    }
}
