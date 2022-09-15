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
 * @description 解决缓存击穿、穿透 工具类
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
