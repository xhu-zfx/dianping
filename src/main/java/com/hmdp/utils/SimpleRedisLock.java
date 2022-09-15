package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @author xhu-zfx
 * @email <756867768@qq.com>
 * @date 2022/8/27 12:14
 * @description
 */
public class SimpleRedisLock implements ILock{

    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX="lock:";
    private static final String ID_PREFIX= UUID.randomUUID().toString(true);

    @Override
    public boolean tryLock(long timeoutsec) {
//        获取线程标识
        String threadId = ID_PREFIX+Thread.currentThread().getId();
//        获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, timeoutsec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unLock() {
//        获取线程标识
        String threadId = ID_PREFIX+Thread.currentThread().getId();
//        获取锁标识
        String id=stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        判断线程标识和锁标识是否一致
        if (threadId.equals(id)){
//        释放锁
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }
}
