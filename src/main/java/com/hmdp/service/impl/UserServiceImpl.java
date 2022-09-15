package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;
/**
 * @author xhu-zfx
 * @email <756867768@qq.com>
 * @description
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
//        校验手机号
        boolean isPhone = RegexUtils.isPhoneInvalid(phone);
//        不符合则返回错误信息
        if (isPhone) return Result.fail("手机号格式错误");
//        符合 , 生成验证码
        String code = RandomUtil.randomNumbers(6);

//        保存验证码到session
//        session.setAttribute("code",code);

//        保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);


//        发送验证码
        log.debug("发送短信验证码成功，验证码：{}",code);
//        返回
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {

//        校验手机号
        String phone = loginForm.getPhone();
        boolean isPhone = RegexUtils.isPhoneInvalid(phone);
        if (isPhone) return Result.fail("手机号格式错误");
//        从redis获得验证码 , 并校验
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
//        不一致报错
        if (cacheCode==null||!cacheCode.toString().equals(code)){
            return Result.fail("验证码错误");
        }
//        一致 , 根据手机号查询用户
        User user = query().eq("phone", phone).one();
//        判断用户是否存在
        if (user==null){
            //        不存在 , 添加用户
            user = createUserByPhone(phone);
        }

//        保存用户信息到redis
//        1. 生成token , 作为登录令牌
        String token = UUID.randomUUID().toString(true);
//        2. 将User对象转为HashMap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().
                        setIgnoreNullValue(true).
                        setFieldValueEditor((filedName,filedValue) -> filedValue.toString()));
//        3. 存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
//        4. 设置token有效期
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.MINUTES);
//        返回token
        return Result.ok(token);
    }

    @Override
    public Result sign() {
//        1.获取当前用户
        Long userId = UserHolder.getUser().getId();
//        2.获取当前时间
        LocalDateTime now = LocalDateTime.now();
//        将当前时间转化为年月格式即yyyyMM格式
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
//        3.拼接key
        String key=USER_SIGN_KEY+userId+":"+keySuffix;
//        4.获得当前天在当前月的索引
//        Returns:the day-of-month, from 1 to 31
        int dayOfMonth = now.getDayOfMonth();
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
//        1.获取当前用户
        Long userId = UserHolder.getUser().getId();
//        2.获取当前时间
        LocalDateTime now = LocalDateTime.now();
//        将当前时间转化为年月格式即yyyyMM格式
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
//        3.拼接key
        String key=USER_SIGN_KEY+userId+":"+keySuffix;
//        4.获得当前天在当前月的索引
//        Returns:the day-of-month, from 1 to 31
        int dayOfMonth = now.getDayOfMonth();
//        5.获得本月截至今天为止的所有签到记录
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
//        没有任何签到结果
        if (result==null|| result.isEmpty()) return Result.ok(0);
        Long num = result.get(0);
        if (num==null||num==0) return Result.ok(0);
//        6.遍历
//        遍历逻辑 : 做与运算,与运算会得到最后一位数字,遇到 0 就结束,遇到 1就向右偏移
        int count=0;
        while (true){
            if ((num & 1)==0){
//                该日未签到 , 遍历结束
                break;
            }
            count++;
            num >>>= 1;
        }
        return Result.ok(count);
    }

    private User createUserByPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(5));
        save(user);
        return user;
    }
}
