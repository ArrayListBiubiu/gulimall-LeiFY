package com.atguigu.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 若要使用redisson，必须要使用RedissonClient对象
 */
@Configuration
public class MyRedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        // 1.创建配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.232.129:6379");  // 特别注意：在IP地址之前要写上协议名：redis:// 或 rediss://
        // 2.根据config创建RedissonClient实
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }

}
