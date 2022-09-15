package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import jodd.util.StringUtil;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author xhu-zfx
 * @email <756867768@qq.com>
 * @description
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
//        缓存空对象解决缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);

//        互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);

//        逻辑过期解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY,LOCK_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
        if (shop==null) return Result.fail("店铺不存在");
        return Result.ok(shop);
    }


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

//    互斥锁解决缓存击穿
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

//    尝试获取锁
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent( key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

//    删除锁
    private void unLock(String key){
        stringRedisTemplate.delete(LOCK_SHOP_KEY + key);
    }

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

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id==null) return Result.fail("店铺id不能为空");
//        1. 更新数据库
        updateById(shop);
//        2. 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        if (x==null||y==null){
            Page<Shop> page=query()
                    .eq("type_id",typeId)
                    .page(new Page<>(current,System.currentTimeMillis()));
            return Result.ok(page.getRecords());
        }
//        计算分页参数
        int from=(current-1)* SystemConstants.DEFAULT_PAGE_SIZE;
        int end=current*SystemConstants.DEFAULT_PAGE_SIZE;

//
        String key=SHOP_GEO_KEY+typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = stringRedisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeCoordinates().limit(end)
        );
        if (geoResults==null) return Result.ok(Collections.emptyList());
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = geoResults.getContent();
        if (content.size()<=from) return Result.ok(Collections.emptyList());
//        截取从from到end
        List<Long> ids=new ArrayList<>(content.size());
        Map<String,Distance> distanceMap=new HashMap<>(content.size());
        content.stream().skip(from).forEach(result->{
//            获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
//            获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr,distance);
        });

        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        shops.forEach(shop -> {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        });
        return Result.ok(shops);
    }
}
