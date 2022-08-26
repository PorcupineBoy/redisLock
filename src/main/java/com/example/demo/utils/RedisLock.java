package com.example.demo.utils;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author 豪猪不挡道
 * @version 1.0
 * @desc redis分布式锁事务实现
 * @date 2021/1/20 20:04
 */
@Slf4j
@Component
public class RedisLock {

    //本地线程，存放线程
    private static ThreadLocal<Thread> THREA_LOCAL = new ThreadLocal<>();
    private final String lock_prefix = "lock_prefix:";
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * @param lock 锁名称
     * @param timeoutSeconds 过期时间（秒）
     * @return
     * @throws InterruptedException
     */
    public String lock(String lock, long timeoutSeconds) throws InterruptedException {
        log.info(Thread.currentThread().getId() + " get lock begin...");
        try {
            String identification = UUID.randomUUID().toString();
            String lockName = String.format("%s%s", lock_prefix, lock);
            // 采用redis 的setIfAbsent 方法，如果不存在则添加，
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(lockName, identification, timeoutSeconds, TimeUnit.SECONDS);
            //开启一个守护线程，重置缓存过期时间
            if (Boolean.TRUE.equals(flag)) {
                // 续签，开启守护线程
                Thread renewal = new Thread(new RenewalLock(lock, identification, timeoutSeconds));
                renewal.setDaemon(true);
                THREA_LOCAL.set(renewal);
                renewal.start();
                return identification;
            }
        } finally {
            log.info(Thread.currentThread().getId() + " get lock end...");
        }
        return "";
    }

    /**
     * @param lock
     * @param identification
     */
    public void release(String lock, String identification) {
        try {
            redisTemplate.setEnableTransactionSupport(true);
            String lockName = String.format("%s%s", lock_prefix, lock);
            redisTemplate.watch(lockName);
            String identificationDB = redisTemplate.opsForValue().get(lockName);
//            if (identification != null && identification.equals(identificationDB)) {
//                redisTemplate.multi();
//                redisTemplate.delete(lockName);
//                redisTemplate.exec();
//                //谁拿的锁。谁去解。
//            }
            if (identification != null && identification.equals(identificationDB)) {
                //通过SessionCallback接口 使用redis  事务
                List<Object> txResults = redisTemplate.execute(new SessionCallback<>() {
                    public List<Object> execute(RedisOperations operations) throws DataAccessException {
                        operations.multi();
                        operations.delete(lockName);

                        // This will contain the results of all operations in the transaction
                        return operations.exec();
                    }
                });
            }
            redisTemplate.unwatch();
        } finally {
            RedisConnectionUtils.unbindConnection(Objects.requireNonNull(redisTemplate.getConnectionFactory()));
            THREA_LOCAL.get().interrupt();
            THREA_LOCAL.remove();
        }
    }

    /**
     * 续签lock
     * 守护线程
     */
    private class RenewalLock implements Runnable {

        private String lockName;
        private String identification;
        private Long timeout;

        public RenewalLock(String lock, String identification, Long timeout) {
            this.lockName = lock_prefix + lock;
            this.identification = identification;
            this.timeout = timeout;
        }

        @Override
        public void run() {
            int i = 0;
            while (true) {
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        log.info("RenewalLock end.");
                        return;
                    }
                    //
                    // 10  5 ， 5秒-》10秒。
                    TimeUnit.SECONDS.sleep(timeout / 2);
                    redisTemplate.setEnableTransactionSupport(true);
                    redisTemplate.watch(lockName);
                    String identificationDB = redisTemplate.opsForValue().get(lockName);
                    if (identificationDB != null && identificationDB.equals(identification)) {
                        /*redisTemplate.multi();
                        redisTemplate.expire(lockName, timeout, TimeUnit.SECONDS);
                        redisTemplate.exec();
                        log.info(Thread.currentThread().getId() + ": reset expire time ok, " + ++i);*/
                        List<Object> txResults = redisTemplate.execute(new SessionCallback<>() {
                            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                                operations.multi();
                                operations.expire(lockName, timeout, TimeUnit.SECONDS);
                                log.info(Thread.currentThread().getId() + ": reset expire time ok, " );
                                // This will contain the results of all operations in the transaction
                                return operations.exec();
                            }
                        });
                    }
                    redisTemplate.unwatch();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("InterruptedException renewalLock end.");
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    log.error("Exception renewalLock end.");
                    log.info(e.getMessage(), e);
                } finally {
                    RedisConnectionUtils.unbindConnection(Objects.requireNonNull(redisTemplate.getConnectionFactory()));
                }
            }
        }
    }

}

