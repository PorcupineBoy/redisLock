package com.example.demo.config;

import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ?> getRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, ?> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setEnableTransactionSupport(true);
        template.setConnectionFactory(factory);
        return template;
    }
    /**
     * 在springboot启动类注入 redisson
     */
    @Bean
    public Redisson redisson() {
        // 此为单机模式
        Config config = new Config();
        config.useSingleServer().setAddress("redis://110.42.229.44:6379");
        /*config.useClusterServers()
                .addNodeAddress("redis://192.168.73.130:8001")
                .addNodeAddress("redis://192.168.73.131:8002")
                .addNodeAddress("redis://192.168.73.132:8003")
                .addNodeAddress("redis://192.168.73.130:8004")
                .addNodeAddress("redis://192.168.73.131:8005")
                .addNodeAddress("redis://192.168.73.132:8006");*/
        return (Redisson) Redisson.create(config);
    }

}