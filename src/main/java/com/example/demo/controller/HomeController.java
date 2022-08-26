package com.example.demo.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.example.demo.model.RspResult;
import com.example.demo.utils.RedisLock;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequestMapping("/api")
@RestController
public class HomeController {
    @Autowired
    private RedisLock redisLock;
    @Autowired
    private RedissonClient redissonClient;

    @ResponseBody
    @RequestMapping("/hello")
    public RspResult hello() {

        return RspResult.ok();
    }

    /**
     * 分布式锁实现逻辑:
     * 1、redis 设置缓存。拿到value
     * 2、判断value是否为空，不为空则执行业务逻辑。为空则返回。
     * 3、业务逻辑结束，释放缓存。
     * @return
     * @throws InterruptedException
     */
    @ResponseBody
    @RequestMapping("/redisLock")
    public RspResult redisLock() throws InterruptedException {
        //第一步拿锁,
        String lock = redisLock.lock("redisLock", 10);
        log.info("lockName is " + lock);

        //第二步，判断拿到的锁是否为空。如果不为空。则说明绑定了锁,执行业务逻辑。
        if (StringUtils.hasText(lock)) {
            try {
                //执行业务逻辑
                log.info(Thread.currentThread().getName() + "我获得了锁");
                Thread.sleep(20000);
                log.info("业务逻辑处理完毕");
            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {
                //谁拿锁，谁开锁
                log.info("redisLock 开始解锁");
                redisLock.release("redisLock", lock);
            }
            return RspResult.ok("处理结束");
        }
        //如果为空的话，则说明有人已经获得了锁，且还在处理中，所以这里就直接返回
        else {
            log.info(Thread.currentThread().getName()+"未获得锁");
            //throw new RuntimeException("redisLock 未获得锁");
            return RspResult.fail(Thread.currentThread().getName()+"未获得锁，请等待任务结束");
        }


    }


    @ResponseBody
    @RequestMapping("/testRedisson")
    public RspResult testRedission() {
        //第一步拿锁
        RLock lock = redissonClient.getLock("redisson");
        log.info("lockName is " + lock.getName());
        //最大等待时间（秒）,多久后自动解锁
        //boolean tryLock2 = lock.tryLock(1, 10, TimeUnit.SECONDS);
        lock.tryLock(); //
        //第二步，判断拿到的锁是否为空。如果不为空。则说明绑定了锁
        if (lock.isLocked()) {
            try {
                log.info(Thread.currentThread().getName() + "我获得了锁");
                Thread.sleep(10000);
            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {
                log.info("Redisson开始解锁");
                lock.unlock();
                log.info("Redisson解锁 完毕");
            }
        } else {
            log.info(Thread.currentThread().getName()+"未获得锁");
            throw new RuntimeException("redisLock 未获得锁");
            //return RspResult.fail("未获得锁，请等待任务结束");
        }

        return RspResult.ok("处理结束");
    }
}
