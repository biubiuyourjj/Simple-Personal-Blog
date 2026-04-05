package com.itheima.service.impl;

import com.itheima.mapper.CategoryMapper;
import com.itheima.pojo.Category;
import com.itheima.service.CategoryService;
import com.itheima.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ClassName:CategoryServiceImpl
 * Package:com.itheima.service.impl
 * Description:
 * Num:
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/20 下午12:33
 * @Version 1.0
 **/
@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;
    @Override
    public void add(Category category){
        //补充属性
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());

        Map<String,Object> map= ThreadLocalUtil.get();
        Integer id=(Integer) map.get("id");
        category.setCreateUser(id);
        categoryMapper.add(category);
    }

    @Override
    public List<Category> list() {
        Map<String,Object> map=ThreadLocalUtil.get();
        Integer id=(Integer) map.get("id");
        return categoryMapper.list(id);
    }

    @Override
    public Category findById(Integer id)
    {
        return categoryMapper.findById(id);
    }

    @Override
    public void update(Category category) {
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.update(category);
    }

    @Override
    public void delete(Integer id){
        categoryMapper.delete(id);
    }
}
