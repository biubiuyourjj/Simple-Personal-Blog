## 1. 项目中哪些地方用了 Redis？

- **登录与鉴权相关**
  - `UserController.login`：登录成功后，把生成的 JWT `token` 存入 Redis，并设置过期时间。
  - `LoginInterceptor.preHandle`：每次请求进来，从请求头里取出 `token`，去 Redis 校验是否存在，存在才放行。
  - `UserController.updatePwd`：修改密码成功后，从 Redis 删除这个 `token`，让用户重新登录。
- **基础测试代码**
  - `RedisTest`：演示如何用 `StringRedisTemplate` 存取字符串、设置过期时间。
- **分页查询缓存方案**
  - `分页查询Redis缓存实现方案.md`：文档中给出了文章分页查询使用 Redis 做缓存的实现思路与完整示例。

下面我们按“从简单到完整流程”的顺序讲解。

---

## 2. 基础：`StringRedisTemplate` 的基本用法

代码位置（测试类）：

```24:40:big-event/src/test/java/com/itheima/RedisTest.java
@SpringBootTest
public class RedisTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void testSet(){
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        operations.set("username","zhangsan");
        operations.set("id","1",15, TimeUnit.SECONDS);
    }

    public void testGet(){
        ValueOperations<String,String> operations=stringRedisTemplate.opsForValue();
        System.out.println(operations.get("username"));
    }
}
```

- **注入模板**：`@Autowired private StringRedisTemplate stringRedisTemplate;`
- **获取操作对象**：`ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();`
- **写入数据**：
  - 永不过期：`ops.set("username", "zhangsan");`
  - 有过期时间：`ops.set("id", "1", 15, TimeUnit.SECONDS);`
- **读取数据**：`ops.get("username");`

> 记住：`StringRedisTemplate` 适合操作 **字符串**，在 Web 项目里做 token、验证码、简单缓存时非常好用。

---

## 3. 登录：把 JWT `token` 存进 Redis

代码位置（控制器）：

```56:80:big-event/src/main/java/com/itheima/controller/UserController.java
@PostMapping("/login")
public Result<String> login(@Pattern(regexp ="^\\S{5,16}$") String username,@Pattern(regexp ="^\\S{5,16}$") String password){
    User loginUser= userService.findByUserName(username);
    if(loginUser==null){
        return Result.error("用户名错误");
    }

    if(Md5Util.getMD5String(password).equals(loginUser.getPassword())){
        Map<String,Object> claims=new HashMap<>();
        claims.put("id",loginUser.getId());
        claims.put("username",loginUser.getUsername());
        String token=JwtUtil.genToken(claims);

        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        operations.set(token,token,1, TimeUnit.HOURS);

        return Result.success(token);
    }
    return Result.error("密码错误！");
}
```

### 3.1 步骤拆解

1. **校验用户名密码**  
   - 先根据 `username` 查用户，再对比 MD5 后的密码。
2. **生成 JWT Token**  
   - 使用 `JwtUtil.genToken(claims)`，把 `id`、`username` 等放进 `claims`。
3. **将 Token 存入 Redis**
   - `ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();`
   - `operations.set(token, token, 1, TimeUnit.HOURS);`
   - **key**：token 自己  
   - **value**：同样是 token（这里你只需要判断“是否存在”，所以 value 内容无所谓）  
   - **过期时间**：`1` 小时，控制登录会话有效期。
4. **返回 token 给前端**
   - 前端把 token 放在以后的请求头 `Authorization` 里。

### 3.2 这样设计的好处

- **Redis 负责“是否仍然有效”**（可随时让 token 失效）。
- **JWT 负责“是谁”**（解析后拿到用户信息）。

---

## 4. 鉴权拦截器：从 Redis 校验 Token 是否有效

代码位置：

```34:76:big-event/src/main/java/com/itheima/interceptor/LoginInterceptor.java
@Component
public class LoginInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,Object handler) throws Exception{
        String token=request.getHeader("Authorization");
        try {
            ValueOperations<String,String> operations = stringRedisTemplate.opsForValue();
            String redisToken = operations.get(token);
            if(redisToken==null){
                throw new RuntimeException();
            }
            Map<String,Object> claims= JwtUtil.parseToken(token);
            ThreadLocalUtil.set(claims);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ThreadLocalUtil.remove();
    }
}
```

### 4.1 拦截器鉴权流程

1. **获取请求头里的 token**
   - `String token = request.getHeader("Authorization");`
2. **用 Redis 校验 token 是否存在**
   - `String redisToken = operations.get(token);`
   - 如果 `redisToken == null`，说明：
     - 没登录 / token 根本没存过，或者
     - token 过期被自动删除了，或者
     - 后端手动删掉了这个 token（比如修改密码）。
3. **解析 token，获取用户信息**
   - `Map<String,Object> claims = JwtUtil.parseToken(token);`
   - 一般会拿到 `id`、`username` 等。
4. **把用户信息放到 ThreadLocal，便于后续使用**
   - `ThreadLocalUtil.set(claims);`
   - 以后在 Controller / Service 里可以直接 `ThreadLocalUtil.get()` 拿当前登录用户。
5. **失败时返回 401**
   - `response.setStatus(401);`，告诉前端“未认证/未登录”，需要重新登录。

### 4.2 Redis 在这里起到的作用

- **让 JWT 可控**：单纯 JWT 自带过期时间，但没法“强制提前失效”；配合 Redis，就可以通过删除 key 让 token 立即失效。
- **多终端管理**：如果你以后需要区分不同终端，可以把 key 设计成 `login:token:{userId}:{device}`。

---

## 5. 修改密码：删除 Redis 中的 Token 实现“下线”

代码位置：

```103:131:big-event/src/main/java/com/itheima/controller/UserController.java
@PatchMapping("/updatePwd")
public Result updatePwd(@RequestBody Map<String,Object> params,@RequestHeader("Authorization") String token){
    // ... 校验参数、校验原密码、对 newPwd / rePwd 的校验 ...

    userService.updatePWD(newPwd);
    //删除redis对应的token
    stringRedisTemplate.delete(token);
    return Result.success();
}
```

### 5.1 步骤拆解

1. 校验请求参数、原密码、新密码（略，跟 Redis 无关）。
2. 调用 `userService.updatePWD(newPwd)` 更新数据库中的密码。
3. **关键一步：删除 Redis 里的 token**
   - `stringRedisTemplate.delete(token);`
   - 这一步会让当前 token 立即失效。
4. 以后再携带这个旧 token 请求时，拦截器从 Redis 中拿不到，就会返回 401。

### 5.2 总结这一步的意义

- 这就是“修改密码后强制重新登录”的实现方式。
- 思路非常通用：**只要想让某个登录状态失效，就删掉对应的 Redis key 即可**。

---

## 6. 分页查询 + Redis 缓存的思路

详细示例已经写在 `分页查询Redis缓存实现方案.md`，这里只提炼思路方便你记忆。

### 6.1 设计缓存 Key（非常重要）

- 一个分页查询的结果依赖这些参数：
  - `userId`（当前登录用户）
  - `pageNum`（第几页）
  - `pageSize`（每页几条）
  - `categoryId`（分类，可空）
  - `state`（状态，可空）
- 因此缓存 key 必须包含所有这些维度，例如：

```java
String cacheKey = String.format(
        "article:list:userId:%d:pageNum:%d:pageSize:%d:categoryId:%s:state:%s",
        userId, pageNum, pageSize,
        categoryId != null ? categoryId : "null",
        state != null ? state : "null"
);
```

### 6.2 查询流程（伪代码）

```java
// 1. 先查 Redis
String json = ops.get(cacheKey);
if (json 命中并且能反序列化成功) {
    直接返回缓存结果;
}

// 2. 未命中，查数据库 + 分页
PageHelper.startPage(pageNum, pageSize);
List<Article> list = articleMapper.list(userId, categoryId, state);
PageBean<Article> pb = 封装 total 和 items;

// 3. 把结果转成 JSON 存进 Redis，设置短一点的过期时间（5~10 分钟）
String value = JSON.toJSONString(pb);
ops.set(cacheKey, value, 5, TimeUnit.MINUTES);
```

### 6.3 缓存更新策略

- 文章新增 / 修改 / 删除时，列表缓存可能都不准确了。
- 方案一：**简单粗暴**：不主动删，只依靠过期时间（推荐入门使用）。
- 方案二：复杂些：用通配符删除当前用户所有列表缓存 key（例：`article:list:userId:{id}:*`）。

---

## 7. 你可以照着做的“实战步骤”

1. **先玩转基础 API**
   - 在 `RedisTest` 里多写几个方法，练习：
     - `set/get`
     - 设置/查看过期时间
     - 删除 key。
2. **完整走一遍登录流程**
   - 打断点 / 日志观察：
     - 登录后，Redis 里是否写入了 token；
     - 请求其他接口时，拦截器是否从 Redis 读到了 token；
     - 拦截器中 `ThreadLocalUtil.get()` 能否在 Controller 里拿到用户信息。
3. **测试修改密码强制下线**
   - 登录 -> 拿 token -> 调用修改密码接口；
   - 再用旧 token 调用业务接口，看是否返回 401。
4. **根据 `分页查询Redis缓存实现方案.md`，给分页接口加上缓存**
   - 按文档抄一遍代码跑通；
   - 自己尝试调整：
     - 改过期时间；
     - 在控制台打印“是命中缓存还是查数据库”。

---

## 8. 小结：本项目 Redis 使用思路

- **使用工具**：`StringRedisTemplate` + `ValueOperations` 对字符串做读写。
- **典型场景**
  - 登录状态控制：token 存入 Redis，拦截器 + 删除 key 实现登录/下线控制。
  - 数据缓存：分页列表结果序列化成 JSON 放入 Redis，提升查询性能。
- **设计关键**
  - 合理设计 key（包含必要维度）。
  - 设置合理过期时间。
  - 搭配业务操作（如修改密码、更新文章）选择是否删除或等待过期。

你可以先按第 7 节的“实战步骤”边看边敲，如果希望，我也可以帮你把文章分页接口里的 Redis 缓存代码一起检查/优化一遍。

