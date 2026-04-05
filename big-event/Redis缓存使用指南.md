# Redis 缓存技术使用指南

## 📋 目录
1. [Redis 基础概念](#redis-基础概念)
2. [项目配置检查](#项目配置检查)
3. [项目中已有的 Redis 使用](#项目中已有的-redis-使用)
4. [Redis 缓存实现方案](#redis-缓存实现方案)
5. [完整代码示例](#完整代码示例)
6. [缓存问题与解决方案](#缓存问题与解决方案)
7. [快速使用模板](#快速使用模板)
8. [最佳实践](#最佳实践)

---

## 🔍 Redis 基础概念

### 什么是 Redis？

Redis（Remote Dictionary Server）是一个**内存数据库**，可以用作：
- **缓存**：提高查询速度，减轻数据库压力
- **消息队列**：异步处理任务
- **会话存储**：存储用户登录状态（如 token）
- **计数器**：实现点赞、浏览量等功能

### 为什么使用 Redis 缓存？

1. **性能提升**：Redis 基于内存，读写速度极快（10万+ QPS）
2. **减轻数据库压力**：减少对 MySQL 的查询次数
3. **提高用户体验**：响应速度更快

### 缓存工作流程

```
用户请求 → Controller → Service
                          ↓
                    先查 Redis
                          ↓
              ┌───────────┴───────────┐
              ↓                       ↓
         缓存命中                  缓存未命中
              ↓                       ↓
         直接返回              查询数据库
                                  ↓
                              存入 Redis
                                  ↓
                              返回结果
```

---

## ⚙️ 项目配置检查

### 1. 依赖配置（已配置 ✅）

在 `pom.xml` 中已包含：

```xml
<!-- Redis 依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2. 配置文件（已配置 ✅）

在 `application.yml` 中已配置：

```yaml
spring:
  data:
    redis:
      host: localhost    # Redis 服务器地址
      port: 6379         # Redis 端口号
```

### 3. Redis 服务器启动

确保 Redis 服务已启动：

**Windows 方式：**
```bash
# 启动 Redis（如果已安装）
redis-server

# 或使用 Redis Desktop Manager 等工具启动
```

**验证连接：**
```bash
# 命令行测试
redis-cli ping
# 应该返回：PONG
```

---

## 📝 项目中已有的 Redis 使用

### 当前使用场景：Token 存储

项目中已经使用 Redis 存储用户登录 token，主要在两个地方：

#### 1. UserController.java - 登录时存储 Token

```java
@PostMapping("/login")
public Result<String> login(String username, String password) {
    // ... 验证用户名密码 ...
    
    // 登录成功，生成 token
    String token = JwtUtil.genToken(claims);
    
    // 把 token 存到 Redis 中，设置过期时间 1 小时
    ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
    operations.set(token, token, 1, TimeUnit.HOURS);
    
    return Result.success(token);
}
```

**关键点：**
- 使用 `StringRedisTemplate` 操作 Redis
- `opsForValue()` 获取字符串操作对象
- `set(key, value, timeout, timeUnit)` 设置带过期时间的值

#### 2. LoginInterceptor.java - 验证 Token

```java
@Override
public boolean preHandle(HttpServletRequest request, ...) {
    String token = request.getHeader("Authorization");
    
    // 从 Redis 中获取 token
    ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
    String redisToken = operations.get(token);
    
    if (redisToken == null) {
        // token 不存在或已过期，拦截请求
        return false;
    }
    
    // token 有效，放行
    return true;
}
```

**关键点：**
- `get(key)` 获取值
- 如果返回 `null`，说明 token 不存在或已过期

---

## 🚀 Redis 缓存实现方案

### 方案一：查询缓存（最常用）

**场景**：查询数据时，先查 Redis，如果缓存中没有，再查数据库，并将结果存入 Redis。

**适用场景：**
- 查询用户信息
- 查询文章详情
- 查询分类列表
- 查询商品信息

#### 实现步骤

1. **查询前先查 Redis**
2. **如果缓存命中，直接返回**
3. **如果缓存未命中，查询数据库**
4. **将查询结果存入 Redis**
5. **返回结果**

#### 代码模板

```java
@Service
public class XxxServiceImpl implements XxxService {
    
    @Autowired
    private XxxMapper xxxMapper;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Override
    public Xxx getById(Integer id) {
        // 1. 构造 Redis key
        String key = "xxx:id:" + id;
        
        // 2. 先从 Redis 中查询
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        String json = operations.get(key);
        
        // 3. 如果缓存命中，直接返回
        if (json != null && !json.isEmpty()) {
            // 将 JSON 字符串转换为对象
            return JSON.parseObject(json, Xxx.class);
        }
        
        // 4. 缓存未命中，查询数据库
        Xxx xxx = xxxMapper.findById(id);
        
        // 5. 如果查询到数据，存入 Redis（设置过期时间）
        if (xxx != null) {
            // 将对象转换为 JSON 字符串
            String value = JSON.toJSONString(xxx);
            // 存入 Redis，设置过期时间 30 分钟
            operations.set(key, value, 30, TimeUnit.MINUTES);
        }
        
        // 6. 返回结果
        return xxx;
    }
}
```

**注意事项：**
- 需要引入 JSON 处理库（如 Fastjson2 或 Jackson）
- Redis key 要有规范，建议格式：`模块名:类型:id`
- 设置合理的过期时间，避免数据过期

---

### 方案二：更新缓存（双写策略）

**场景**：更新数据时，同时更新数据库和 Redis 缓存。

**适用场景：**
- 更新用户信息
- 更新文章内容
- 修改分类信息

#### 实现步骤

1. **更新数据库**
2. **删除或更新 Redis 缓存**
3. **返回结果**

#### 代码模板

```java
@Override
public void update(Xxx xxx) {
    // 1. 更新数据库
    xxxMapper.update(xxx);
    
    // 2. 删除对应的 Redis 缓存（下次查询时会重新加载）
    String key = "xxx:id:" + xxx.getId();
    stringRedisTemplate.delete(key);
    
    // 或者：直接更新缓存
    // ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
    // String value = JSON.toJSONString(xxx);
    // operations.set(key, value, 30, TimeUnit.MINUTES);
}
```

**两种策略对比：**

| 策略 | 优点 | 缺点 | 推荐 |
|------|------|------|------|
| **删除缓存** | 简单，保证数据一致性 | 下次查询需要查数据库 | ✅ 推荐 |
| **更新缓存** | 下次查询更快 | 可能数据不一致，代码复杂 | ⚠️ 谨慎使用 |

---

### 方案三：列表缓存

**场景**：缓存列表数据（如分类列表、文章列表）。

**注意事项：**
- 列表数据变化频繁，缓存时间不宜过长
- 更新数据时，需要删除列表缓存

#### 代码模板

```java
@Override
public List<Category> list() {
    String key = "category:list:userId:" + userId;
    
    // 1. 先查 Redis
    ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
    String json = operations.get(key);
    
    if (json != null && !json.isEmpty()) {
        // 缓存命中
        return JSON.parseArray(json, Category.class);
    }
    
    // 2. 查询数据库
    List<Category> list = categoryMapper.list(userId);
    
    // 3. 存入 Redis
    if (list != null && !list.isEmpty()) {
        String value = JSON.toJSONString(list);
        operations.set(key, value, 10, TimeUnit.MINUTES); // 列表缓存时间短一些
    }
    
    return list;
}

// 更新时删除列表缓存
@Override
public void add(Category category) {
    categoryMapper.add(category);
    
    // 删除列表缓存
    String key = "category:list:userId:" + userId;
    stringRedisTemplate.delete(key);
}
```

---

## 💻 完整代码示例

### 示例：为 CategoryService 添加缓存

假设我们要为分类查询添加缓存功能，以下是完整的实现：

#### 1. 添加 JSON 依赖（如果还没有）

在 `pom.xml` 中添加：

```xml
<!-- Fastjson2（推荐，性能好） -->
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
    <version>2.0.43</version>
</dependency>
```

或者使用 Spring Boot 自带的 Jackson（无需额外依赖）：

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
```

#### 2. 修改 CategoryServiceImpl.java

**完整代码示例：**

```java
package com.itheima.service.impl;

import com.alibaba.fastjson2.JSON;
import com.itheima.mapper.CategoryMapper;
import com.itheima.pojo.Category;
import com.itheima.service.CategoryService;
import com.itheima.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CategoryServiceImpl implements CategoryService {
    
    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Override
    public void add(Category category) {
        // 补充属性
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        category.setCreateUser(userId);
        
        // 1. 更新数据库
        categoryMapper.add(category);
        
        // 2. 删除列表缓存（使缓存失效）
        String listKey = "category:list:userId:" + userId;
        stringRedisTemplate.delete(listKey);
    }
    
    @Override
    public List<Category> list() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        
        // 1. 构造 Redis key
        String key = "category:list:userId:" + userId;
        
        // 2. 先从 Redis 查询
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        String json = operations.get(key);
        
        // 3. 如果缓存命中，直接返回
        if (json != null && !json.isEmpty()) {
            // 将 JSON 字符串转换为 List<Category>
            return JSON.parseArray(json, Category.class);
        }
        
        // 4. 缓存未命中，查询数据库
        List<Category> list = categoryMapper.list(userId);
        
        // 5. 将查询结果存入 Redis
        if (list != null && !list.isEmpty()) {
            String value = JSON.toJSONString(list);
            // 设置过期时间 30 分钟
            operations.set(key, value, 30, TimeUnit.MINUTES);
        }
        
        return list;
    }
    
    @Override
    public Category findById(Integer id) {
        // 1. 构造 Redis key
        String key = "category:id:" + id;
        
        // 2. 先从 Redis 查询
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        String json = operations.get(key);
        
        // 3. 如果缓存命中，直接返回
        if (json != null && !json.isEmpty()) {
            return JSON.parseObject(json, Category.class);
        }
        
        // 4. 缓存未命中，查询数据库
        Category category = categoryMapper.findById(id);
        
        // 5. 将查询结果存入 Redis
        if (category != null) {
            String value = JSON.toJSONString(category);
            operations.set(key, value, 30, TimeUnit.MINUTES);
        }
        
        return category;
    }
    
    @Override
    public void update(Category category) {
        category.setUpdateTime(LocalDateTime.now());
        
        // 1. 更新数据库
        categoryMapper.update(category);
        
        // 2. 删除对应的缓存
        String key = "category:id:" + category.getId();
        stringRedisTemplate.delete(key);
        
        // 3. 删除列表缓存
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        String listKey = "category:list:userId:" + userId;
        stringRedisTemplate.delete(listKey);
    }
    
    @Override
    public void delete(Integer id) {
        // 1. 删除数据库记录
        categoryMapper.delete(id);
        
        // 2. 删除对应的缓存
        String key = "category:id:" + id;
        stringRedisTemplate.delete(key);
        
        // 3. 删除列表缓存
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        String listKey = "category:list:userId:" + userId;
        stringRedisTemplate.delete(listKey);
    }
}
```

**关键修改点：**

1. **注入 StringRedisTemplate**
   ```java
   @Autowired
   private StringRedisTemplate stringRedisTemplate;
   ```

2. **查询方法添加缓存逻辑**
   - 先查 Redis
   - 缓存未命中时查数据库
   - 将结果存入 Redis

3. **更新/删除方法清除缓存**
   - 删除对应的单个缓存
   - 删除列表缓存

---

## ⚠️ 缓存问题与解决方案

### 1. 缓存穿透

**问题**：查询一个不存在的数据，Redis 中没有，数据库也没有，导致每次请求都查数据库。

**解决方案：缓存空值**

```java
@Override
public Category findById(Integer id) {
    String key = "category:id:" + id;
    ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
    String json = operations.get(key);
    
    if (json != null && !json.isEmpty()) {
        // 判断是否是空值标记
        if ("null".equals(json)) {
            return null; // 直接返回 null，不再查数据库
        }
        return JSON.parseObject(json, Category.class);
    }
    
    // 查询数据库
    Category category = categoryMapper.findById(id);
    
    if (category != null) {
        // 有数据，正常缓存
        String value = JSON.toJSONString(category);
        operations.set(key, value, 30, TimeUnit.MINUTES);
    } else {
        // 没有数据，缓存空值（设置较短的过期时间）
        operations.set(key, "null", 5, TimeUnit.MINUTES);
    }
    
    return category;
}
```

---

### 2. 缓存击穿

**问题**：热点数据过期，大量请求同时访问数据库。

**解决方案：互斥锁**

```java
@Override
public Category findById(Integer id) {
    String key = "category:id:" + id;
    ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
    String json = operations.get(key);
    
    if (json != null && !json.isEmpty()) {
        return JSON.parseObject(json, Category.class);
    }
    
    // 尝试获取锁
    String lockKey = "lock:category:id:" + id;
    Boolean lock = stringRedisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
    
    if (Boolean.TRUE.equals(lock)) {
        try {
            // 再次检查缓存（双重检查）
            json = operations.get(key);
            if (json != null && !json.isEmpty()) {
                return JSON.parseObject(json, Category.class);
            }
            
            // 查询数据库
            Category category = categoryMapper.findById(id);
            
            if (category != null) {
                String value = JSON.toJSONString(category);
                operations.set(key, value, 30, TimeUnit.MINUTES);
            }
            
            return category;
        } finally {
            // 释放锁
            stringRedisTemplate.delete(lockKey);
        }
    } else {
        // 获取锁失败，等待一小段时间后重试
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return findById(id); // 递归重试
    }
}
```

---

### 3. 缓存雪崩

**问题**：大量缓存同时过期，导致所有请求都查数据库。

**解决方案：设置随机过期时间**

```java
// 不要使用固定时间
// operations.set(key, value, 30, TimeUnit.MINUTES); // ❌ 不推荐

// 使用随机过期时间
Random random = new Random();
int expireTime = 25 + random.nextInt(10); // 25-35 分钟之间
operations.set(key, value, expireTime, TimeUnit.MINUTES); // ✅ 推荐
```

---

## 📋 快速使用模板

### 模板 1：查询缓存模板

```java
@Override
public Xxx getById(Integer id) {
    // 1. 构造 key
    String key = "xxx:id:" + id;
    
    // 2. 查 Redis
    ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
    String json = operations.get(key);
    
    // 3. 缓存命中
    if (json != null && !json.isEmpty()) {
        return JSON.parseObject(json, Xxx.class);
    }
    
    // 4. 查数据库
    Xxx xxx = xxxMapper.findById(id);
    
    // 5. 存入 Redis
    if (xxx != null) {
        String value = JSON.toJSONString(xxx);
        operations.set(key, value, 30, TimeUnit.MINUTES);
    }
    
    return xxx;
}
```

### 模板 2：更新缓存模板

```java
@Override
public void update(Xxx xxx) {
    // 1. 更新数据库
    xxxMapper.update(xxx);
    
    // 2. 删除缓存
    String key = "xxx:id:" + xxx.getId();
    stringRedisTemplate.delete(key);
    
    // 3. 删除列表缓存（如果有）
    String listKey = "xxx:list:userId:" + userId;
    stringRedisTemplate.delete(listKey);
}
```

### 模板 3：列表缓存模板

```java
@Override
public List<Xxx> list() {
    String key = "xxx:list:userId:" + userId;
    
    ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
    String json = operations.get(key);
    
    if (json != null && !json.isEmpty()) {
        return JSON.parseArray(json, Xxx.class);
    }
    
    List<Xxx> list = xxxMapper.list(userId);
    
    if (list != null && !list.isEmpty()) {
        String value = JSON.toJSONString(list);
        operations.set(key, value, 10, TimeUnit.MINUTES);
    }
    
    return list;
}
```

---

## 🎯 最佳实践

### 1. Redis Key 命名规范

```
格式：模块名:类型:id或标识
示例：
- category:id:1
- category:list:userId:123
- article:id:100
- user:token:abc123
```

### 2. 过期时间设置

| 数据类型 | 推荐过期时间 | 说明 |
|---------|------------|------|
| 用户信息 | 30-60 分钟 | 变化不频繁 |
| 文章详情 | 1-2 小时 | 变化不频繁 |
| 列表数据 | 10-30 分钟 | 变化较频繁 |
| Token | 1-24 小时 | 根据业务需求 |
| 热点数据 | 5-10 分钟 | 变化频繁 |

### 3. 缓存更新策略

**推荐：删除缓存（Cache Aside）**

```java
// ✅ 推荐：更新数据库后删除缓存
@Override
public void update(Xxx xxx) {
    xxxMapper.update(xxx);
    stringRedisTemplate.delete("xxx:id:" + xxx.getId());
}

// ⚠️ 谨慎：直接更新缓存（可能导致数据不一致）
@Override
public void update(Xxx xxx) {
    xxxMapper.update(xxx);
    String value = JSON.toJSONString(xxx);
    operations.set("xxx:id:" + xxx.getId(), value, 30, TimeUnit.MINUTES);
}
```

### 4. 使用 JSON 库的选择

**方案一：Fastjson2（推荐）**

```xml
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
    <version>2.0.43</version>
</dependency>
```

```java
import com.alibaba.fastjson2.JSON;

// 对象转 JSON
String json = JSON.toJSONString(object);

// JSON 转对象
Xxx xxx = JSON.parseObject(json, Xxx.class);

// JSON 转列表
List<Xxx> list = JSON.parseArray(json, Xxx.class);
```

**方案二：Jackson（Spring Boot 自带）**

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Autowired
private ObjectMapper objectMapper;

// 对象转 JSON
String json = objectMapper.writeValueAsString(object);

// JSON 转对象
Xxx xxx = objectMapper.readValue(json, Xxx.class);

// JSON 转列表
List<Xxx> list = objectMapper.readValue(json, new TypeReference<List<Xxx>>() {});
```

---

## 📊 性能对比

### 查询性能对比

| 操作 | 数据库查询 | Redis 查询 | 性能提升 |
|------|-----------|-----------|---------|
| 单次查询 | ~10ms | ~0.1ms | **100倍** |
| 1000次查询 | ~10秒 | ~0.1秒 | **100倍** |

### 适用场景

**适合使用缓存：**
- ✅ 查询频繁的数据
- ✅ 变化不频繁的数据
- ✅ 热点数据
- ✅ 计算结果复杂的数据

**不适合使用缓存：**
- ❌ 实时性要求极高的数据
- ❌ 变化非常频繁的数据
- ❌ 数据量特别大的列表（考虑分页）

---

## 🔧 常见问题

### Q1: Redis 连接失败怎么办？

**A:** 检查以下几点：
1. Redis 服务是否启动
2. `application.yml` 中的 host 和 port 是否正确
3. 防火墙是否阻止连接

### Q2: 如何查看 Redis 中的数据？

**A:** 使用 Redis 客户端工具：
- Redis Desktop Manager（图形化工具）
- 命令行：`redis-cli` → `keys *` → `get key`

### Q3: 缓存和数据库数据不一致怎么办？

**A:** 
- 更新数据时，**删除缓存**而不是更新缓存
- 使用较短的过期时间
- 重要数据可以考虑实时更新缓存

### Q4: 如何选择过期时间？

**A:**
- 根据数据变化频率
- 根据业务需求
- 使用随机过期时间避免缓存雪崩

### Q5: 对象序列化用什么？

**A:**
- 推荐使用 **Fastjson2**（性能好，使用简单）
- 或使用 Spring Boot 自带的 **Jackson**

---

## 📚 总结

### 核心要点记忆：

1. **查询缓存流程**：查 Redis → 未命中 → 查数据库 → 存 Redis → 返回
2. **更新缓存策略**：更新数据库 → 删除缓存（推荐）
3. **Key 命名规范**：`模块名:类型:id`
4. **过期时间设置**：根据数据变化频率，使用随机时间
5. **解决三大问题**：穿透（空值缓存）、击穿（互斥锁）、雪崩（随机过期）

### 快速记忆口诀：

> **查询：先查 Redis，未命中查数据库，结果存 Redis**  
> **更新：更新数据库，删除缓存**  
> **Key：模块:类型:id**  
> **过期：随机时间，避免雪崩**

---

## 🎓 实战练习

### 练习 1：为 ArticleService 添加缓存

**任务：**
- 为 `getById()` 方法添加查询缓存
- 为 `update()` 方法添加缓存更新
- 为 `delete()` 方法添加缓存删除

**提示：**
- Key 格式：`article:id:{id}`
- 过期时间：1 小时
- 更新/删除时删除缓存

### 练习 2：为 UserService 添加缓存

**任务：**
- 为 `findByUserName()` 方法添加缓存
- 为 `update()` 方法添加缓存更新

**提示：**
- Key 格式：`user:username:{username}`
- 过期时间：30 分钟

---

**祝你使用愉快！** 🎉
