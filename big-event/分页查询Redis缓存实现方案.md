# 分页查询 Redis 缓存实现方案

## 📋 问题分析

你的代码中存在以下问题：

1. ❌ 使用了 `redisTemplate`，应该使用 `stringRedisTemplate`
2. ❌ 试图直接缓存 `Page<Article>` 对象，Redis 只能存储字符串，需要序列化为 JSON
3. ❌ 缓存 key 不完整，应该包含所有查询参数（pageNum, pageSize, categoryId, state）
4. ❌ 缺少查询数据库和存入缓存的逻辑
5. ❌ 缺少日志导入

---

## 🔧 解决方案

### 步骤 1: 添加 JSON 依赖（二选一）

#### 方案一：使用 Fastjson2（推荐，性能好）

在 `pom.xml` 中添加：

```xml
<!-- Fastjson2 依赖 -->
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
    <version>2.0.43</version>
</dependency>
```

#### 方案二：使用 Spring Boot 自带的 Jackson（无需额外依赖）

无需添加依赖，直接使用 `ObjectMapper`

---

### 步骤 2: 完整的 ArticleServiceImpl.java 代码

#### 方案一：使用 Fastjson2

```java
package com.itheima.service.impl;

import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.ArticleMapper;
import com.itheima.pojo.Article;
import com.itheima.pojo.PageBean;
import com.itheima.service.ArticleService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ArticleServiceImpl implements ArticleService {
    
    @Autowired
    private ArticleMapper articleMapper;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Override
    public void add(Article article) {
        article.setUpdateTime(LocalDateTime.now());
        article.setCreateTime(LocalDateTime.now());
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        article.setCreateUser(userId);
        articleMapper.add(article);
        
        // 添加文章后，删除该用户的所有文章列表缓存
        deleteArticleListCache(userId);
    }
    
    @Override
    public PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state) {
        // 1. 获取当前用户ID
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        
        // 2. 构造 Redis key（包含所有查询参数）
        String cacheKey = buildCacheKey(userId, pageNum, pageSize, categoryId, state);
        
        // 3. 先从 Redis 查询
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        String json = operations.get(cacheKey);
        
        // 4. 如果缓存命中，直接返回
        if (json != null && !json.isEmpty()) {
            log.info("从缓存中获取文章列表数据，key: {}", cacheKey);
            PageBean<Article> cachedPageBean = JSON.parseObject(json, PageBean.class);
            // 需要手动设置泛型类型
            List<Article> articles = JSON.parseArray(JSON.toJSONString(cachedPageBean.getItems()), Article.class);
            return new PageBean<>(cachedPageBean.getTotal(), articles);
        }
        
        // 5. 缓存未命中，查询数据库
        log.info("缓存未命中，查询数据库，key: {}", cacheKey);
        
        // 创建 PageBean 对象
        PageBean<Article> pb = new PageBean<>();
        
        // 开启分页查询（必须在调用 Mapper 之前）
        PageHelper.startPage(pageNum, pageSize);
        
        // 调用 Mapper 查询数据
        List<Article> as = articleMapper.list(userId, categoryId, state);
        
        // 转换为 Page 对象
        Page<Article> p = (Page<Article>) as;
        
        // 填充 PageBean 对象
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        
        // 6. 将查询结果存入 Redis（设置过期时间 10 分钟）
        if (pb.getItems() != null && !pb.getItems().isEmpty()) {
            String value = JSON.toJSONString(pb);
            operations.set(cacheKey, value, 10, TimeUnit.MINUTES);
            log.info("文章列表数据已存入缓存，key: {}", cacheKey);
        }
        
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
        
        // 更新文章后，删除相关缓存
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        deleteArticleListCache(userId);
    }
    
    @Override
    public void delete(Integer id) {
        articleMapper.delete(id);
        
        // 删除文章后，删除相关缓存
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        deleteArticleListCache(userId);
    }
    
    /**
     * 构造缓存 key
     * 格式：article:list:userId:{userId}:pageNum:{pageNum}:pageSize:{pageSize}:categoryId:{categoryId}:state:{state}
     */
    private String buildCacheKey(Integer userId, Integer pageNum, Integer pageSize, 
                                 Integer categoryId, String state) {
        StringBuilder key = new StringBuilder("article:list:userId:").append(userId);
        key.append(":pageNum:").append(pageNum);
        key.append(":pageSize:").append(pageSize);
        if (categoryId != null) {
            key.append(":categoryId:").append(categoryId);
        } else {
            key.append(":categoryId:null");
        }
        if (state != null && !state.isEmpty()) {
            key.append(":state:").append(state);
        } else {
            key.append(":state:null");
        }
        return key.toString();
    }
    
    /**
     * 删除该用户的所有文章列表缓存
     * 使用通配符匹配所有相关的缓存 key
     */
    private void deleteArticleListCache(Integer userId) {
        String pattern = "article:list:userId:" + userId + ":*";
        // 注意：stringRedisTemplate 没有直接的 keys 方法，需要使用 execute
        stringRedisTemplate.execute(connection -> {
            Set<byte[]> keys = connection.keys(pattern.getBytes());
            if (keys != null && !keys.isEmpty()) {
                connection.del(keys.toArray(new byte[0][]));
                log.info("已删除用户 {} 的所有文章列表缓存，共 {} 个", userId, keys.size());
            }
            return null;
        });
    }
}
```

**注意：** 上面的 `deleteArticleListCache` 方法使用了 `execute`，需要导入：

```java
import org.springframework.data.redis.core.RedisCallback;
import java.util.Set;
```

如果不想使用通配符删除，可以使用更简单的方式：

```java
/**
 * 删除该用户的所有文章列表缓存（简化版）
 * 注意：这种方式只能删除已知的缓存，无法删除所有可能的组合
 */
private void deleteArticleListCache(Integer userId) {
    // 由于分页查询的 key 包含多个参数组合，很难全部删除
    // 建议：设置较短的过期时间，或者使用 Redis 的 Hash 结构
    log.info("文章数据已更新，相关缓存将在过期后自动失效");
}
```

---

#### 方案二：使用 Jackson（Spring Boot 自带）

```java
package com.itheima.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.ArticleMapper;
import com.itheima.pojo.Article;
import com.itheima.pojo.PageBean;
import com.itheima.service.ArticleService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ArticleServiceImpl implements ArticleService {
    
    @Autowired
    private ArticleMapper articleMapper;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper; // Spring Boot 自动配置的 ObjectMapper
    
    @Override
    public PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state) {
        // 1. 获取当前用户ID
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        
        // 2. 构造 Redis key
        String cacheKey = buildCacheKey(userId, pageNum, pageSize, categoryId, state);
        
        // 3. 先从 Redis 查询
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        String json = operations.get(cacheKey);
        
        // 4. 如果缓存命中，直接返回
        if (json != null && !json.isEmpty()) {
            try {
                log.info("从缓存中获取文章列表数据，key: {}", cacheKey);
                PageBean<Article> cachedPageBean = objectMapper.readValue(
                    json, 
                    new TypeReference<PageBean<Article>>() {}
                );
                return cachedPageBean;
            } catch (Exception e) {
                log.error("反序列化缓存数据失败，key: {}", cacheKey, e);
                // 如果反序列化失败，继续查询数据库
            }
        }
        
        // 5. 缓存未命中，查询数据库
        log.info("缓存未命中，查询数据库，key: {}", cacheKey);
        
        PageBean<Article> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Article> as = articleMapper.list(userId, categoryId, state);
        Page<Article> p = (Page<Article>) as;
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        
        // 6. 将查询结果存入 Redis
        if (pb.getItems() != null && !pb.getItems().isEmpty()) {
            try {
                String value = objectMapper.writeValueAsString(pb);
                operations.set(cacheKey, value, 10, TimeUnit.MINUTES);
                log.info("文章列表数据已存入缓存，key: {}", cacheKey);
            } catch (Exception e) {
                log.error("序列化数据失败，key: {}", cacheKey, e);
            }
        }
        
        return pb;
    }
    
    /**
     * 构造缓存 key
     */
    private String buildCacheKey(Integer userId, Integer pageNum, Integer pageSize, 
                                 Integer categoryId, String state) {
        StringBuilder key = new StringBuilder("article:list:userId:").append(userId);
        key.append(":pageNum:").append(pageNum);
        key.append(":pageSize:").append(pageSize);
        key.append(":categoryId:").append(categoryId != null ? categoryId : "null");
        key.append(":state:").append(state != null ? state : "null");
        return key.toString();
    }
    
    // ... 其他方法保持不变
}
```

---

## 🎯 推荐方案（简化版，易于理解）

考虑到分页查询的参数组合很多，缓存管理复杂，这里提供一个**简化但实用**的方案：

```java
@Override
public PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state) {
    // 1. 获取当前用户ID
    Map<String, Object> map = ThreadLocalUtil.get();
    Integer userId = (Integer) map.get("id");
    
    // 2. 构造 Redis key（包含所有查询参数）
    String cacheKey = String.format("article:list:userId:%d:pageNum:%d:pageSize:%d:categoryId:%s:state:%s",
            userId, pageNum, pageSize, 
            categoryId != null ? categoryId : "null",
            state != null ? state : "null");
    
    // 3. 先从 Redis 查询
    ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
    String json = operations.get(cacheKey);
    
    // 4. 如果缓存命中，直接返回
    if (json != null && !json.isEmpty()) {
        log.info("从缓存中获取文章列表数据，key: {}", cacheKey);
        try {
            // 使用 Fastjson2 反序列化
            PageBean<Article> cachedPageBean = JSON.parseObject(json, PageBean.class);
            // 手动转换 items 列表
            List<Article> articles = JSON.parseArray(
                JSON.toJSONString(cachedPageBean.getItems()), 
                Article.class
            );
            return new PageBean<>(cachedPageBean.getTotal(), articles);
        } catch (Exception e) {
            log.error("反序列化缓存数据失败，key: {}", cacheKey, e);
            // 反序列化失败，继续查询数据库
        }
    }
    
    // 5. 缓存未命中，查询数据库
    log.info("缓存未命中，查询数据库，key: {}", cacheKey);
    
    // 创建 PageBean 对象
    PageBean<Article> pb = new PageBean<>();
    
    // 开启分页查询（必须在调用 Mapper 之前）
    PageHelper.startPage(pageNum, pageSize);
    
    // 调用 Mapper 查询数据
    List<Article> as = articleMapper.list(userId, categoryId, state);
    
    // 转换为 Page 对象
    Page<Article> p = (Page<Article>) as;
    
    // 填充 PageBean 对象
    pb.setTotal(p.getTotal());
    pb.setItems(p.getResult());
    
    // 6. 将查询结果存入 Redis（设置过期时间 10 分钟）
    if (pb.getItems() != null && !pb.getItems().isEmpty()) {
        try {
            String value = JSON.toJSONString(pb);
            operations.set(cacheKey, value, 10, TimeUnit.MINUTES);
            log.info("文章列表数据已存入缓存，key: {}", cacheKey);
        } catch (Exception e) {
            log.error("序列化数据失败，key: {}", cacheKey, e);
        }
    }
    
    return pb;
}
```

---

## 📝 需要添加的导入

```java
import com.alibaba.fastjson2.JSON;  // 如果使用 Fastjson2
import lombok.extern.slf4j.Slf4j;   // 日志注解
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.concurrent.TimeUnit;
```

---

## ⚠️ 重要注意事项

### 1. 缓存 Key 的设计

分页查询的缓存 key **必须包含所有查询参数**，否则会出现数据混乱：

```java
// ✅ 正确：包含所有参数
"article:list:userId:1:pageNum:1:pageSize:10:categoryId:2:state:已发布"

// ❌ 错误：缺少参数
"article:list:userId:1"  // 不同查询条件会覆盖
```

### 2. 缓存更新策略

**问题：** 分页查询的参数组合很多（pageNum × pageSize × categoryId × state），很难在更新时删除所有相关缓存。

**解决方案：**

**方案 A：设置较短的过期时间（推荐）**
```java
operations.set(cacheKey, value, 5, TimeUnit.MINUTES); // 5分钟过期
```

**方案 B：更新时删除该用户的所有列表缓存**
```java
// 使用通配符删除（需要 RedisCallback）
String pattern = "article:list:userId:" + userId + ":*";
```

**方案 C：不缓存分页列表，只缓存单个文章详情**
- 分页列表变化频繁，缓存意义不大
- 只缓存 `getById()` 方法

### 3. 泛型序列化问题

`PageBean<Article>` 在序列化时，泛型信息会丢失。需要手动转换：

```java
// 反序列化后需要手动转换 items
List<Article> articles = JSON.parseArray(
    JSON.toJSONString(cachedPageBean.getItems()), 
    Article.class
);
```

---

## 🚀 最终推荐代码

考虑到实际使用场景，**推荐使用简化版**，设置较短的过期时间：

```java
@Override
public PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state) {
    Map<String, Object> map = ThreadLocalUtil.get();
    Integer userId = (Integer) map.get("id");
    
    // 构造缓存 key
    String cacheKey = String.format("article:list:userId:%d:pageNum:%d:pageSize:%d:categoryId:%s:state:%s",
            userId, pageNum, pageSize, 
            categoryId != null ? categoryId : "null",
            state != null ? state : "null");
    
    // 先查 Redis
    ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
    String json = operations.get(cacheKey);
    
    if (json != null && !json.isEmpty()) {
        log.info("从缓存中获取数据，key: {}", cacheKey);
        PageBean<Article> cached = JSON.parseObject(json, PageBean.class);
        List<Article> articles = JSON.parseArray(
            JSON.toJSONString(cached.getItems()), 
            Article.class
        );
        return new PageBean<>(cached.getTotal(), articles);
    }
    
    // 查询数据库
    log.info("缓存未命中，查询数据库，key: {}", cacheKey);
    PageBean<Article> pb = new PageBean<>();
    PageHelper.startPage(pageNum, pageSize);
    List<Article> as = articleMapper.list(userId, categoryId, state);
    Page<Article> p = (Page<Article>) as;
    pb.setTotal(p.getTotal());
    pb.setItems(p.getResult());
    
    // 存入 Redis（5分钟过期，避免数据不一致）
    if (pb.getItems() != null && !pb.getItems().isEmpty()) {
        String value = JSON.toJSONString(pb);
        operations.set(cacheKey, value, 5, TimeUnit.MINUTES);
    }
    
    return pb;
}
```

---

## 📊 性能优化建议

1. **只缓存热点数据**：第一页、第二页的数据
2. **设置合理过期时间**：5-10 分钟
3. **考虑缓存单个文章详情**：`getById()` 方法更适合缓存
4. **监控缓存命中率**：如果命中率低，考虑不使用缓存

---

**祝你使用愉快！** 🎉
