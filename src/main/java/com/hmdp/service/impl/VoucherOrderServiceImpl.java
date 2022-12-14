package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author xhu-zfx
 * @email <756867768@qq.com>
 * @description
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT=new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private BlockingQueue<VoucherOrder> orderTasks=new ArrayBlockingQueue<>(1024*1024);
    private static final ExecutorService SECKILL_ORDER_EXECUTOR= Executors.newSingleThreadExecutor();
    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {

        }
    }

//    ????????????
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        UserDTO user = UserHolder.getUser();
////        1. ??????lua??????
//        Long resultOfLua = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), user.getId().toString());
////        2. ?????????????????????0
//        int result = resultOfLua.intValue();
////        2.1 ????????????0 , ??????????????????
//        if (result!=0) return Result.fail(result==1?"????????????":"???????????????????????????");
////        2.2 ?????????0 , ??????????????? , ????????????????????????????????????
//        long orderId = redisIdWorker.nextId("order");
////        todo ??????????????????
//        orderTasks.add(voucherId);
////        3. ????????????id
//        return Result.ok(orderId);
//    }


//    ???????????????????????????,????????????????????????????????????
    @Override
    public Result seckillVoucher(Long voucherId) {
//        1. ???????????????
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        2. ???????????????????????????????????????????????????????????????????????????????????????????????????
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail(" ???????????????????????????????????? ");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail(" ??????????????????????????????????????? ");
        }
//        3. ??????????????????????????????????????????????????????????????????
        if (voucher.getStock()<1){
            return Result.fail(" ?????????????????????????????????????????? ");
        }
        Long userId = UserHolder.getUser().getId();
        //impleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("order:" + userId);
        boolean getLock = lock.tryLock();
        if (!getLock) {
            return Result.fail("???????????????????????????,???????????????");
        }
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }finally {
            lock.unlock();
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId){
        Long userId = UserHolder.getUser().getId();
//        x. ???????????????????????????
        int countOfVoucherByUser = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (countOfVoucherByUser>0) return Result.fail("?????????????????????????????????");

//        5. ????????????
        boolean decreaseStock = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock",0).update();
        if (!decreaseStock) {
            return Result.fail(" ?????????????????????????????????????????? ");
        }
//        6. ????????????
        VoucherOrder voucherOrder = new VoucherOrder();
//        6.1 ??????id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
//        6.2 ??????id
        voucherOrder.setUserId(userId);
//        6.3 ?????????id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
//        7. ????????????id
        return Result.ok(orderId);

    }
}
