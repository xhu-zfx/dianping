package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author xhu-zfx
 * @email <756867768@qq.com>
 * @date 2022/8/26 11:57
 * @description 全局id生成工具类
 */

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
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;
//        2. 生成序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
//        3. 拼接id
        return timeStamp<<COUNT_BITS | count;
    }


}
