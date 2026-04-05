package com.itheima.controller;

import com.itheima.pojo.Article;
import com.itheima.pojo.Category;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import com.itheima.service.ArticleService;
import com.itheima.utils.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ClassName:ArticleController
 * Package:com.itheima.controller
 * Description:登录认证
 * Num:4
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/18 下午8:34
 * @Version 1.0
 **/
@RestController
@RequestMapping("/article")
public class ArticleController {
    @Autowired
    private ArticleService articleService;

    @PostMapping
    public Result add(@RequestBody @Validated(Article.Add.class) Article article){
        articleService.add(article);
        return Result.success();

    }

    @GetMapping("/list")
    public Result<PageBean<Article>> list(
            Integer pageNum,
            Integer pageSize,
            @RequestParam(required = false)Integer categoryId,//可有可没有
            @RequestParam(required = false) String state
    ){
        PageBean<Article> pb= articleService.list(pageNum,pageSize,categoryId,state);
        return Result.success(pb);
    }

    @GetMapping("/getById")
    public Result getById(@Validated Integer id){
        return Result.success(articleService.getById(id));

    }

    @PutMapping("/update")
    public Result update(@RequestBody @Validated(Article.Update.class) Article article){
        articleService.update(article);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result delete(Integer id){
        articleService.delete(id);
        return Result.success();
    }

/*
    @GetMapping("/list")
    public Result<String> list(*//*@RequestHeader(name="Authorization") String token,HttpServletResponse responsee*//* ){
//        //验证token
//        try {
//            Map<String,Object> claims= JwtUtil.parseToken(token);
//            return Result.success("所有的文章数据。。。");
//        } catch (Exception e) {
//            //http响应状态码为401
//            response.setStatus(401);
//            return Result.error("未登录");
//        }
        return Result.success("所有的文章数据。。。");
    }*/
}
