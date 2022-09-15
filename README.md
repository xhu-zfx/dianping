# Redis笔记

# 安装(....懒得写)

# Redis命令

## 通用命令

### KEYS **pattern**

Redis [KEYS](https://redis.com.cn/commands/keys.html) 命令用于查找所有匹配给定模式 pattern 的 key 。

匹配模式:

* `h?llo` 匹配 `hello`, `hallo` 和 `hxllo`
* `h*llo` 匹配 `hllo` 和 `heeeello`
* `h[ae]llo` 匹配 `hello` and `hallo,` 不匹配 `hillo`
* `h[^e]llo` 匹配 `hallo`, `hbllo`, ... 不匹配 `hello`
* `h[a-b]llo` 匹配 `hallo` 和 `hbllo`

使用 `\` 转义你想匹配的特殊字符。

### DEL **key [key ...]**

#### 语法

Redis DEL 命令用于删除给定的一个或多个 `key` 。

不存在的 `key` 会被忽略。

#### 返回值

整数：被删除 `key` 的数量。

### EXISTS **key [key ...]**

Redis EXISTS 命令用于检查给定 `key` 是否存在。

#### 返回值

整数：

* `1` key存在
* `0` key不存在

### EXPIRE **key seconds**

Redis `Expire` 命令设置 `key` 的过期时间（seconds）。 设置的时间过期后，key 会被自动删除。带有超时时间的 key 通常被称为易失的( *volatile* )。

#### 返回值

整数：

* `1` 设置超时成功。
* `0` `key` 不存在。

### TTL **key**

Redis TTL 命令以秒为单位返回 key 的剩余过期时间。用户客户端检查 key 还可以存在多久。

[PTTL](https://redis.com.cn/commands/pttl.html) 返回以毫秒为单位的剩余超时时间。

#### 返回值

* 整数：剩余超时秒数，失败返回负数如下。
* key 不存在返回 `-2`
* key 存在但是没有关联超时时间返回 `-1`

## String类型

|String的常见命令|描述|例子|
| -----------------------------------------------------------------------------------------------------------------------------------------------------| --------------------------------------------------------------------------| -----------------------|
|set **key value**|添加或者修改已经存在的一个String类型的键值对|set name jack|
|get **key**|根据key获取String类型的value|get name|
|mset **key [key ...]**|批量添加多个String类型的键值对|mset name jack age 19|
|mget **key value [key value ...]**|根据多个key获取多个String类型的value|mget name age|
|incr **key**|让一个**整型的key自增1**|incr age|
|incrby **key increment**|让一个**整型的key自增并指定步长**，例如：incrby num 2 让num值自增2|incr age 3|
|incrbyfloat **key increment**|让一个**浮点类型**的数字自增并指定步长|incrbyfloat num 2.1|
|setnx **key value**|添加一个String类型的键值对，前提是这个key不存在，否则不执行|setnx name jack|
|setex **key seconds value**|添加一个String类型的键值对，并且指定有效期|set name 20 jack|

## Redis的key的格式，[项目名]:[业务名]:[类型]:[id]

1. 原因：Redis没有类似MySQL中的Table的概念，我们该**如何区分不同类型的key**呢？例如，需要存储用户、商品信息到redis，有一个用户id是1，有一个商品id恰好也是1。

2. 解决：Redis的key允许有多个单词形成层级结构，多个单词之间用’:’隔开，格式如下：

`项目名:业务名:类型:id`

3. 例子

例如我们的项目名称叫 heima，有user和product两种不同类型的数据，我们可以这样定义key：

uuser相关的key：heima:user:1

uproduct相关的key：heima:product:1

`set heima:user:1 '{"id":1,"name":"jack","age":21}'`

`set heima:user:2 '{"id":2,"name":"rose","age":22}'`

`set heima:product:2 '{"id":1,"name":"redmi30","price":1999}'`

`set heima:product:1 '{"id":1,"name":"xiaomi11","price":24999}'`

key会被自动转化为包含关系，value会显示成json格式，如下图

![image](assets/image-20220821165048-2lut3b0.png)


## Hash类型

|Hash的常见命令：|描述|例子|
| ----------------------| -------------------------------------------------------------------| -------------------------------------|
|HSET key field value|添加或者修改hash类型key的field的值|hset heima:user:3 name jack|
|HGET key field|获取一个hash类型key的field的值|hget heima:user:3 name|
|HMSET|批量添加多个hash类型key的field的值|hmset heima:user:3 name jack age 18|
|HMGET|批量获取多个hash类型key的field的值|hmget heima:user:3 name age|
|HGETALL|获取一个hash类型的key中的所有的field和value|hgetall heima:user:3|
|HKEYS|获取一个hash类型的key中的所有的field|hkeys heima:user:3|
|HVALS|获取一个hash类型的key中的所有的value|hvals heima:user:3|
|HINCRBY|让一个hash类型key的字段值自增并指定步长|hincrby heima:user:3 age 5|
|HSETNX|添加一个hash类型的key的field值，前提是这个field不存在，否则不执行|hsetnx heima:user:3 sex man|

## List类型

命令	简述	使用  
RPUSH	将给定值推入到列表右端	RPUSH key value  
LPUSH	将给定值推入到列表左端	LPUSH key value  
RPOP	从列表的右端弹出一个值，并返回被弹出的值	RPOP key  
LPOP	从列表的左端弹出一个值，并返回被弹出的值	LPOP key  
LRANGE	获取列表在给定范围上的所有值	LRANGE key 0 -1  
LINDEX	通过索引获取列表中的元素。你也可以使用负数下标，以 -1 表示列表的最后一个元素， -2 表示列表的倒数第二个元素，以此类推。	LINDEX key index  
LINSERT	在某一个旧元素值的前边或后边插入一个新的值	linsert key before/after old_value new_value  
LLEN	过去链表长度	llen key  
LTRIM	截取 list 从 stater 到 end 位置的值并保留	ltrim key start end  
LREM	删除 count 个元素值为 value 的元素	lrem key count value  
LSET	修改索引号为 index 的元素的值为 value	LSET key index value


## Set类型

## SortedSet

# Jedis

## Jedis直连**使用方法**

1. 建立连接

    ```java
            jedis=new Jedis("192.168.181.128",6379);
            jedis.auth("123456");
            jedis.select(0);
    ```
2. 使用jedis对象操作redis，方法名跟redis命令名相同

    操作String类型

    ```java
            String res = jedis.set("name", "jack");
            System.out.println("res "+res);
            String name = jedis.get("name");
            System.out.println("name "+name);
    ```

    操作Hash类型

    ```java
            Long hset = jedis.hset("user:1", "name", "mom");
            Long hset1 = jedis.hset("user:1", "age", "20");
            Map<String, String> HashMapRes = jedis.hgetAll("user:1");
            System.out.println(hset);
            System.out.println(hset1);
            System.out.println(HashMapRes);
    ```
3. 关闭连接

    ```java
            if (jedis!=null){
                jedis.close();
            }
    ```

## Jedis连接池

# SpringDateRedis

# Redis实战-[仿大众点评项目](F:\Java Web\Redis\dianping)
# 前端
## 本文件仅对后端代码进行讲解，前端不做解释，前端项目位于 nginx-1.18.0 下，使用nginx反向代理，并使用负载均衡，方便分布式测试
## 快速开始(windows 环境)
在 nginx-1.18.0/ 下调出CMD窗口，输入以下命令
```
start nginx.exe
```
前端项目仅作手机端适配
# 缓存问题

## 缓存穿透

> **缓存击穿**是指客户端请求的数据在缓存和数据库中都不存在，这样缓存永远不会生效，这些请求都会交给数据库来处理，当用户反复进行此操作时，数据库访问压力过大
>

### 解决方案

* **缓存空对象**

第一次发送请求时，依然会从数据库读查询，当查询为失败时，会将空对象存入redis中

当再次发送请求时，当从redis读取的缓存为空字符串时，直接返回错误信息，而不会再次从数据库查询

例：

```java
//    缓存穿透
    public Shop queryWithPassThrough(Long id) {
        String key=CACHE_SHOP_KEY + id;
//        1. 从redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        2. 判断缓存是否命中
//          3. 命中，返回商铺信息
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson,Shop.class);
        }
//        StrUtil.isNotBlank()  当 值为 null、""、"\t\n"均会返回false
//        由于上面判断过shopJson不为空 , 所以shopJson只存在空字符串或者null
//        所以此时 shopJson!=null 即 shopJson!=""
        if (shopJson!=null){
            return null;
        }
//          4. 未命中，根据id从数据库查询
        Shop shop = getById(id);
//              5.不存在， 缓存空对象 返回错误信息
        if (shop==null){
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;

        }
//              6.存在，写入redis缓存
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        6. 返回商铺信息
        return shop;
    }
```

* **布隆过滤**

## 缓存雪崩

> **缓存雪崩**是指在同一时间段大量缓存key同时失效或者Redis服务器宕机，导致大量请求到达数据库，给数据库带来巨大压力
>

**解决方案**

* 给不同的key的TTL添加随机值
* 利用Redis集群提高服务的可用性
* 给缓存业务添加降级限流策略
* 给业务添加多级缓存

## 缓存击穿

> **缓存击穿**也叫热点key问题，就是一个被**高并发访问**并且**缓存重建业务比较复杂**的key突然失效了，大量的访问请求会在瞬间使数据库瘫痪
>

**解决方案**

* 互斥锁：给缓存重建业务添加一把乐观锁，其他请求进来需要等待


  ```java
  //    缓存击穿
      public Shop queryWithMutex(Long id) {
          String key=CACHE_SHOP_KEY + id;
  //        1. 从redis查询缓存
          String shopJson = stringRedisTemplate.opsForValue().get(key);
  //        2. 判断缓存是否命中
  //          3. 命中，返回商铺信息
          if (StrUtil.isNotBlank(shopJson)) {
              return JSONUtil.toBean(shopJson,Shop.class);
          }
  //        StrUtil.isNotBlank()  当 值为 null、""、"\t\n"均会返回false
  //        由于上面判断过shopJson不为空 , 所以shopJson只存在空字符串或者null
  //        所以此时 shopJson!=null 即 shopJson!=""
          if (shopJson!=null){
              return null;
          }
  //        4. 实现缓存重建
  //        4.1. 获取互斥锁
          String lockKey = LOCK_SHOP_KEY + id;
          Shop shop=null;
          try {
              boolean getLock = tryLock(lockKey);
  //        4.2. 判断是否获取成功
              if (!getLock){
  //          4.3. 失败，休眠，并重试
                  Thread.sleep(50);
                  return queryWithMutex(id);
              }
  //          4.4. 成功，根据id从数据库查询
              shop = getById(id);
  //           模拟重建延时
              Thread.sleep(200);
  //              5.不存在， 缓存空对象 返回错误信息
              if (shop==null){
                  stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                  return null;

              }
  //              6.存在，写入redis缓存
              stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);

          }catch (Exception e){
              throw new RuntimeException(e);
          }finally {
              //        7. 释放互斥锁
              unLock(lockKey);

          }
  //        8. 返回商铺信息
          return shop;
      }
  ```

  1


  ```java
  //    尝试获取锁
      private boolean tryLock(String key){
          Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent( key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
          return BooleanUtil.isTrue(flag);
      }

  //    删除锁
      private void unLock(String key){
          stringRedisTemplate.delete(LOCK_SHOP_KEY + key);
      }
  ```
* 逻辑过期：流程与互斥锁一样，多维护一个逻辑过期字段，其他请求进来，查看逻辑过期字段，如果发现过期，获取互斥锁，如果获取失败，直接返回旧数据

  ```java
  //    存入逻辑过期字段
      public void saveShopToRedis(Long id,Long expireSeconds) throws InterruptedException {
  //        1. 查询店铺数据
          Shop shop = getById(id);
          Thread.sleep(200);
  //        2. 封装逻辑过期时间
          RedisData redisData = new RedisData();
          redisData.setData(shop);
          redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
  //        3. 写入Redis
          stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
      }

  ```

  ```java
      private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);

  //    逻辑过期解决缓存击穿
      public Shop queryWithLogicalExpire(Long id) {
          String key=CACHE_SHOP_KEY + id;
  //        1. 从redis查询缓存
          String shopJson = stringRedisTemplate.opsForValue().get(key);
  //        2. 判断缓存是否命中
          if (StrUtil.isBlank(shopJson)) {
  //        3. 未命中，返回空
              return null;
          }
  //        4. 命中，先把json反序列化为对象
          RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
  //        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
          Shop shop = (Shop) redisData.getData();
          LocalDateTime expireTime = redisData.getExpireTime();
  //        5. 判断缓存是否过期
          if (expireTime.isAfter(LocalDateTime.now())){
  //        5.1 未过期,返回商铺信息
              return shop;
          }
  //        5.2 过期,进入缓存重建
  //          6. 缓存重建,获取互斥锁
          String lockKey = LOCK_SHOP_KEY + id;
          boolean getLock = tryLock(lockKey);
          if (getLock){
  //          6.1 获取互斥锁成功,开启独立线程,实现缓存重建
              CACHE_REBUILD_EXECUTOR.submit(()->{
                  try {
                      this.saveShopToRedis(id,30L);
                  }catch (Exception e){
                      throw new RuntimeException(e);
                  }finally {
  //                    释放锁
                      unLock(lockKey);
                  }
  //
              });
          }
  //          6.2 获取互斥锁失败,返回过期的商铺信息
          return shop;
  }

  ```


## 封装缓存工具类

注意泛型类与函数式编程的使用


```java
package com.hmdp.utils;


import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * @author xhu-zfx
 * @email <756867768@qq.com>
 * @date 2022/8/25 18:35
 * @description 缓存工具类
 */

@Slf4j
@Component
public class CacheClient {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);



    private void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    private void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

//    缓存穿透
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback, Long time, TimeUnit unit) {
        String key=keyPrefix + id;
//        1. 从redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
//        2. 判断缓存是否命中
//          3. 命中，返回商铺信息
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json,type);
        }
//        StrUtil.isNotBlank()  当 值为 null、""、"\t\n"均会返回false
//        由于上面判断过shopJson不为空 , 所以shopJson只存在空字符串或者null
//        所以此时 shopJson!=null 即 shopJson!=""
        if (json!=null){
            return null;
        }
//          4. 未命中，根据id从数据库查询
        R res = dbFallback.apply(id);
//              5.不存在， 缓存空对象 返回错误信息
        if (res==null){
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;

        }
//              6.存在，写入redis缓存
        this.set(key,res,time,unit);
        return res;
    }

    //    逻辑过期解决缓存击穿
    public <R,ID> R queryWithLogicalExpire(String keyPrefix,String lockPrefix, ID id,Class<R> type,Function<ID,R> dbFallback, Long time, TimeUnit unit) {
        String key=keyPrefix + id;
//        1. 从redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        2. 判断缓存是否命中
        if (StrUtil.isBlank(shopJson)) {
//        3. 未命中，返回空
            return null;
        }
//        4. 命中，先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        R res = JSONUtil.toBean((JSONObject) redisData.getData(), type);
//        R res = (R) redisData.getData();
        LocalDateTime expireTime = redisData.getExpireTime();
//        5. 判断缓存是否过期
        if (expireTime.isAfter(LocalDateTime.now())){
//        5.1 未过期,返回商铺信息
            return res;
        }
//        5.2 过期,进入缓存重建
//          6. 缓存重建,获取互斥锁
        String lockKey = lockPrefix + id;
        boolean getLock = tryLock(lockKey);
        if (getLock){
//          6.1 获取互斥锁成功,开启独立线程,实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    R res1=dbFallback.apply(id);
                    this.setWithLogicalExpire(key,res1,time,unit);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
//                    释放锁
                    unLock(lockKey);
                }
            });
        }
//          6.2 获取互斥锁失败,返回过期的商铺信息
        return res;
    }

    //    尝试获取锁
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent( key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    //    删除锁
    private void unLock(String key){
        stringRedisTemplate.delete(LOCK_SHOP_KEY + key);
    }

}
```

# 秒杀功能

# Redis实现全局唯一ID

ID的组成部分

* 符号位：1bit，永远为0
* 时间戳：31bit，以秒为单位，可以使用69年
* 序列号：32bit，每秒的计数器，支持每秒产生2^32^个不同ID

下例中以`2022-1-1 00:00:00`为开始时间戳

```java
@Component
public class RedisIdWorker {

//    开始的时间戳 2022-1-1 00:00:00
    public static final long BEGIN_TIMESTAMP=1640995200L;
//    序列号的位数
    public static final int COUNT_BITS=32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public long nextId(String keyPrefix){
//        1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
//        将当前时间转化为秒
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
//        现在时间与开始时间之间的差
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;
//        2. 生成序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
//        3. 拼接id
        return timeStamp<<COUNT_BITS | count;
    }


}
```


# 乐观锁解决秒杀超卖问题

不使用乐观锁的情况

```java
    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
//        1. 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        2. 判断秒杀是否开始，是否结束，如果还未开始或者已经结束，返回失败信息
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail(" 秒杀尚未开始，请耐心等候 ");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail(" 秒杀已经结束，下次早点来哦 ");
        }
//        3. 判断库存是否充足，如果库存不足，返回失败信息
        if (voucher.getStock()<1){
            return Result.fail(" 券已经被抢光啦，下次早点来哦 ");
        }
//        5. 扣减库存
        boolean decreaseStock = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).update();
        if (!decreaseStock) {
            return Result.fail(" 券已经被抢光啦，下次早点来哦 ");
        }
//        6. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
//        6.1 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
//        6.2 用户id
        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
//        6.3 代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
//        7. 返回订单id
        return Result.ok(orderId);
    }
```

常规乐观锁使用版本号，在执行sql语句时多加一条版本号确认

本例使用剩余库存简便，在执行sql时添加一条stock>0的判定

```java
//        对应上代码中的18行
        boolean decreaseStock = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock",0).update();
```

# 一人一单功能

在获取优惠券还有余量的时候，根据优惠券ID和用户ID在优惠券订单表中查询，如果有记录，则拒绝该用户购买该优惠券的请求


```java
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

//    未考虑线程安全问题,并发请求时会出现超卖问题
    @Override
    public Result seckillVoucher(Long voucherId) {
//        1. 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        2. 判断秒杀是否开始，是否结束，如果还未开始或者已经结束，返回失败信息
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail(" 秒杀尚未开始，请耐心等候 ");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail(" 秒杀已经结束，下次早点来哦 ");
        }
//        3. 判断库存是否充足，如果库存不足，返回失败信息
        if (voucher.getStock()<1){
            return Result.fail(" 券已经被抢光啦，下次早点来哦 ");
        }
        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()){
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId){
        Long userId = UserHolder.getUser().getId();
//        x. 优化：一人一单功能
        int countOfVoucherByUser = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (countOfVoucherByUser>0) return Result.fail("您已经购买过此优惠券了");

//        5. 扣减库存
        boolean decreaseStock = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock",0).update();
        if (!decreaseStock) {
            return Result.fail(" 券已经被抢光啦，下次早点来哦 ");
        }
//        6. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
//        6.1 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
//        6.2 用户id
        voucherOrder.setUserId(userId);
//        6.3 代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
//        7. 返回订单id
        return Result.ok(orderId);

    }
}
```

# 分布式锁

> 上面已经解决了在单一服务器下面的超卖问题
>
> 但是当使用集群部署时依然会出现超卖问题，`synchronized`关键字无法锁住多个服务器的请求，所以需要分布式锁来解决
>

**满足在分布式系统或集群模式下多线程可见并且互斥的锁**


## Redission

> Redisson是一个在Redis的基础上实现的Java驻内存数据网格（In-Memory Data Grid）。它不仅提供了一系列的分布式的Java常用对象，还提供了许多分布式服务，其中就包括了各种分布式锁的实现
>


# Redis实现消息队列

# 点赞功能

# BitMap

# Redis持久化

redis 提供了两种持久化的方式，分别是 **RDB** （Redis DataBase）和 **AOF** （Append Only File）。

RDB，简而言之，就是在不同的时间点，将 redis 存储的数据生成快照并存储到磁盘等介质上；

AOF，则是换了一个角度来实现持久化，那就是将 redis 执行过的所有写指令记录下来，在下次 redis 重新启动时，只要把这些写指令从前到后再重复执行一遍，就可以实现数据恢复了。

其实 RDB 和 AOF 两种方式也可以同时使用，在这种情况下，如果 redis 重启的话，则会优先采用 AOF 方式来进行数据恢复，这是因为 AOF 方式的数据恢复完整度更高。

如果你没有数据持久化的需求，也完全可以关闭 RDB 和 AOF 方式，这样的话，redis 将变成一个纯内存数据库，就像 memcache 一样。

* ## redis持久化RDB

  RDB 方式，是将 redis 某一时刻的数据持久化到磁盘中，是一种快照式的持久化方法。

  redis 在进行数据持久化的过程中，会先将数据写入到一个临时文件中，待持久化过程都结束了，才会用这个临时文件替换上次持久化好的文件。正是这种特性，让我们可以随时来进行备份，因为快照文件总是完整可用的。

  对于 RDB 方式，redis 会单独创建（fork）一个子进程来进行持久化，而主进程是不会进行任何 IO 操作的，这样就确保了 redis 极高的性能。

  如果需要进行大规模数据的恢复，且对于数据恢复的完整性不是非常敏感，那 RDB 方式要比 AOF 方式更加的高效。

  虽然 RDB 有不少优点，但它的缺点也是不容忽视的。如果你对数据的完整性非常敏感，那么 RDB 方式就不太适合你，因为即使你每 5 分钟都持久化一次，当 redis 故障时，仍然会有近 5 分钟的数据丢失。所以，redis 还提供了另一种持久化方式，那就是 AOF。
* ## redis持久化  AOF

  AOF，英文是 Append Only File，即只允许追加不允许改写的文件。

  如前面介绍的，AOF 方式是将执行过的写指令记录下来，在数据恢复时按照从前到后的顺序再将指令都执行一遍，就这么简单。

  我们通过配置 redis.conf 中的 appendonly yes 就可以打开 AOF 功能。如果有写操作（如 SET 等），redis 就会被追加到 AOF 文件的末尾。

  默认的 AOF 持久化策略是每秒钟 fsync 一次（fsync 是指把缓存中的写指令记录到磁盘中），因为在这种情况下，redis 仍然可以保持很好的处理性能，即使 redis 故障，也只会丢失最近 1 秒钟的数据。

  如果在追加日志时，恰好遇到磁盘空间满、inode 满或断电等情况导致日志写入不完整，也没有关系，redis 提供了 redis-check-aof 工具，可以用来进行日志修复。

  因为采用了追加方式，如果不做任何处理的话，AOF 文件会变得越来越大，为此，redis 提供了 AOF 文件重写（rewrite）机制，即当 AOF 文件的大小超过所设定的阈值时，redis 就会启动 AOF 文件的内容压缩，只保留可以恢复数据的最小指令集。举个例子或许更形象，假如我们调用了 100 次 INCR 指令，在 AOF 文件中就要存储 100 条指令，但这明显是很低效的，完全可以把这 100 条指令合并成一条 SET 指令，这就是重写机制的原理。

  在进行 AOF 重写时，仍然是采用先写临时文件，全部完成后再替换的流程，所以断电、磁盘满等问题都不会影响 AOF 文件的可用性，这点大家可以放心。

  AOF 方式的另一个好处，我们通过一个“场景再现”来说明。某同学在操作 redis 时，不小心执行了 FLUSHALL，导致 redis 内存中的数据全部被清空了，这是很悲剧的事情。不过这也不是世界末日，只要 redis 配置了 AOF 持久化方式，且 AOF 文件还没有被重写（rewrite），我们就可以用最快的速度暂停 redis 并编辑 AOF 文件，将最后一行的 FLUSHALL 命令删除，然后重启 redis，就可以恢复 redis 的所有数据到 FLUSHALL 之前的状态了。是不是很神奇，这就是 AOF 持久化方式的好处之一。但是如果 AOF 文件已经被重写了，那就无法通过这种方法来恢复数据了。

  虽然优点多多，但 AOF 方式也同样存在缺陷，比如在同样数据规模的情况下，AOF 文件要比 RDB 文件的体积大。而且，AOF 方式的恢复速度也要慢于 RDB 方式。

  如果你直接执行 BGREWRITEAOF 命令，那么 redis 会生成一个全新的 AOF 文件，其中便包括了可以恢复现有数据的最少的命令集。

  如果运气比较差，AOF 文件出现了被写坏的情况，也不必过分担忧，redis 并不会贸然加载这个有问题的 AOF 文件，而是报错退出。这时可以通过以下步骤来修复出错的文件：

  1.备份被写坏的 AOF 文件\ 2.运行 redis-check-aof –fix 进行修复\ 3.用 diff -u 来看下两个文件的差异，确认问题点\ 4.重启 redis，加载修复后的 AOF 文件
* ## redis持久化 – AOF重写

  AOF 重写的内部运行原理，我们有必要了解一下。

  在重写即将开始之际，redis 会创建（fork）一个“重写子进程”，这个子进程会首先读取现有的 AOF 文件，并将其包含的指令进行分析压缩并写入到一个临时文件中。

  与此同时，主工作进程会将新接收到的写指令一边累积到内存缓冲区中，一边继续写入到原有的 AOF 文件中，这样做是保证原有的 AOF 文件的可用性，避免在重写过程中出现意外。

  当“重写子进程”完成重写工作后，它会给父进程发一个信号，父进程收到信号后就会将内存中缓存的写指令追加到新 AOF 文件中。

  当追加结束后，redis 就会用新 AOF 文件来代替旧 AOF 文件，之后再有新的写指令，就都会追加到新的 AOF 文件中了。

# Redis主从搭建

假如为一主二从，虚拟机ip为192.168.181.130

在节点7002用slaveof 192.168.181.130 7001命令将它作为7001的从节点

在节点7003用slaveof 192.168.181.130 7001命令将它作为7001的从节点

完成后用info replication在主节点和从节点分别查看状态

从节点7002/7003

```shell
127.0.0.1:7003> info replication
# Replication
role:slave
master_host:192.168.181.130
master_port:7001
master_link_status:up
master_last_io_seconds_ago:4
master_sync_in_progress:0
slave_read_repl_offset:84
slave_repl_offset:84
slave_priority:100
slave_read_only:1
replica_announced:1
connected_slaves:0
master_failover_state:no-failover
master_replid:a15c777ce96aaad331ceeaf5d3ef46bc81e231fc
master_replid2:0000000000000000000000000000000000000000
master_repl_offset:84
second_repl_offset:-1
repl_backlog_active:1
repl_backlog_size:1048576
repl_backlog_first_byte_offset:57
repl_backlog_histlen:28

```

主节点7001

```shell
127.0.0.1:7001> info replication
# Replication
role:master
connected_slaves:2
slave0:ip=192.168.181.130,port=7002,state=online,offset=112,lag=0
slave1:ip=192.168.181.130,port=7003,state=online,offset=112,lag=1
master_failover_state:no-failover
master_replid:a15c777ce96aaad331ceeaf5d3ef46bc81e231fc
master_replid2:0000000000000000000000000000000000000000
master_repl_offset:112
second_repl_offset:-1
repl_backlog_active:1
repl_backlog_size:1048576
repl_backlog_first_byte_offset:1
repl_backlog_histlen:112

```

tips：如果redis设置了密码，则需要在从节点的redis.conf文件中设置密码`masterauth 123456`




# 主从同步的流程

* slave节点请求增量同步
* master节点判断replid，发现不一致，说明是第一次同步，拒绝增量同步，执行全量同步
* master将完整内存数据生成RDB，发送RDB到slave
* slave清空本地数据，加载master的RDB
* master将RDB期间的命令记录在repl_baklog，并持续将log中的命令发送给slave

# Redis哨兵

# Redis多级缓存


# Redis底层

## Redis过期策略

**RedisKey的TTL记录方式**

* 在RedisDB中通过一个Dict记录每个Key的TTL时间

**过期Key的删除策略**

* 惰性清理：每次查找Key时判断是否过期，如果过期则删除
* 定期删除：定期抽样部分Key，判断是否过期，如果过期则删除
* 定时删除：在设置键的过期时间的同时，创建一个定时器，让定时器在键的过期时间来临时，立即执行对键的删除操作。（ 创建定时器删除 ）

**定期清理的两种模式**

* SLOW模式执行评论默认为10，每次不超过25ms
* FAST模式执行频率不固定，但两次间隔不低于2ms，每次耗时不超过1ms

## Redis内存淘汰策略

内存淘汰：就是当Redis内存使用达到设定的阈值时，Redis主动挑选**部分Key**删除以释放更多的内存的流程

Redis支持8种不同策略来选择删除的Key，在redis.conf文件中修改maxmemory-policy noeviction即可

* noeviction：不会淘汰任何Key，当内存满时不允许写入任何新数据，默认
* volatile-ttl：从配置了TTL的Key中比较剩余的TTL值，TTL越小的越先被淘汰
* allkeys-random：对全体Key，随机进行淘汰，即从db->dict中淘汰
* volatile-random：对设置了TTL的Key，随机进行淘汰，即从db->expires中淘汰
* allkeys-lru：对全体Key，通过LRU算法淘汰最久没有使用的键
* volatile-lru：对设置了TTL的Key，淘汰最久没有使用的键
* allkeys-lfu：对全体Key，通过LFU算法淘汰使用频率最少的键
* volatile-lfu：对设置了TTL的Key，淘汰使用频率最少的键

LRU(Least Recently Used)：最少最近使用，用当前时间减去最后一次访问时间，值越大，越会被优先淘汰

LFU(Least Frequently Used)：最少频率使用，统计每个Key的访问次数，值越小，约会被优先淘汰
