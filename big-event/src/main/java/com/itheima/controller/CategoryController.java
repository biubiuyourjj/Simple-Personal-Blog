package com.itheima.controller;

import com.itheima.pojo.Category;
import com.itheima.pojo.Result;
import com.itheima.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ClassName:CategoryController
 * Package:com.itheima.controller
 * Description:
 * Num:6
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/20 下午12:30
 * @Version 1.0
 **/
@RestController
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @PostMapping
    //validate验证的意思
    public Result add(@RequestBody @Validated(Category.Add.class) Category category){
        categoryService.add(category);
        return Result.success();
    }

    @GetMapping
    public Result<List<Category>> list(){
        List<Category> cs=categoryService.list();
        return Result.success(cs);
    }

    @GetMapping("/detail")
    public Result<Category> detail(Integer id){

        return Result.success(categoryService.findById(id));
    }

    @PutMapping     //requestbody将jason数据转换为category对象
    public Result update(@RequestBody @Validated(Category.Update.class) Category category){
        categoryService.update(category);
        return Result.success();
    }

    @DeleteMapping
    public Result delete(Integer id){
        categoryService.delete(id);
        return Result.success();
    }
}
