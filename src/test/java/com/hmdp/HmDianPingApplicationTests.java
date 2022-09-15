package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;
/**
 * @author xhu-zfx
 * @email <756867768@qq.com>
 * @description
 */
@SpringBootTest
public class HmDianPingApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private ExecutorService es= Executors.newFixedThreadPool(500);

    @Test
    public void testId() throws InterruptedException {
        CountDownLatch latch=new CountDownLatch(300);
        Runnable task=()->{
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println(id);
            }
            latch.countDown();
        };
        long beginTime = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long endTime = System.currentTimeMillis();
        System.out.println("allTime = "+(endTime-beginTime)+ "ms");
    }

    @Test
    public void testSaveShop() throws InterruptedException {
        shopService.saveShopToRedis(1L,10L);
    }

    @Test
    public void testLoadShopData(){
        List<Shop> list = shopService.list();
        Map<Long,List<Shop>> map=list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        for (Map.Entry<Long,List<Shop>> entry: map.entrySet()){
            Long typeId = entry.getKey();
            String key=SHOP_GEO_KEY+typeId;
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations=new ArrayList<>();
            value.forEach(shop -> {
//                stringRedisTemplate.opsForGeo().add(key,new Point(shop.getX(),shop.getY()),shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),new Point(shop.getX(),shop.getY())));
            });
            stringRedisTemplate.opsForGeo().add(key,locations);
        }
    }

    @Test
    public void testHyberLogLog(){
        String[] values = new String[1000];
        int j=0;
        for (int i=0;i<1000000;i++){
            j=i%1000;
            values[j]="user_"+i;
            if (j==999) stringRedisTemplate.opsForHyperLogLog().add("hl1",values);
        }
        System.out.println(stringRedisTemplate.opsForHyperLogLog().size("hl1"));
    }

}
