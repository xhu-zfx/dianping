package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;
/**
 * @author xhu-zfx
 * @email <756867768@qq.com>
 * @description
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
