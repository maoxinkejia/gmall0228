package com.atguigu.gmall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:disabled}")
    private  String host;

    @Value("${spring.redis.port:0}")
    private  int port;

    @Value("${spring.redis.database:0}")
    private  int database;

    //将这个类注册进IOC容器中
    @Bean
    public RedisUtil getRedisUtil(){
        if ("disabled".equals(host)) {
            return null;
        }

        //
        RedisUtil redisUtil = new RedisUtil();
        redisUtil.initJedisPool(host,port,database);
        return redisUtil;
    }
}
