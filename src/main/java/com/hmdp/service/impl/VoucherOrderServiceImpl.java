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

//    异步秒杀
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        UserDTO user = UserHolder.getUser();
////        1. 执行lua脚本
//        Long resultOfLua = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), user.getId().toString());
////        2. 判断结果是否为0
//        int result = resultOfLua.intValue();
////        2.1 结果不为0 , 没有购买资格
//        if (result!=0) return Result.fail(result==1?"库存不足":"您已经购买过此券了");
////        2.2 结果为0 , 有购买资格 , 把下单信息保存到阻塞队列
//        long orderId = redisIdWorker.nextId("order");
////        todo 存入阻塞队列
//        orderTasks.add(voucherId);
////        3. 返回订单id
//        return Result.ok(orderId);
//    }


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
        //impleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("order:" + userId);
        boolean getLock = lock.tryLock();
        if (!getLock) {
            return Result.fail("您已经购买过此券了,请下次再来");
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
