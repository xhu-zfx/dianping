package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
/**
 * @author xhu-zfx
 * @email <756867768@qq.com>
 * @description
 */
public class UserHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    public static UserDTO getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
