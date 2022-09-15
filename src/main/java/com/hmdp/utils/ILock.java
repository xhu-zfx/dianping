package com.hmdp.utils;

/**
 * @author xhu-zfx
 * @email <756867768@qq.com>
 * @date 2022/8/27 12:11
 * @description
 */
public interface ILock {
    boolean tryLock(long timeoutsec);
    void unLock();
}
