package com.itheima.service;

import com.itheima.pojo.Category;

import java.util.List;

/**
 * ClassName:CategoryService
 * Package:com.itheima.service
 * Description:
 * Num:
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/20 下午12:32
 * @Version 1.0
 **/
public interface CategoryService {
    void update(Category category);

    void add(Category category);

    List<Category> list();

    Category findById(Integer id);

    void delete(Integer id);
}
