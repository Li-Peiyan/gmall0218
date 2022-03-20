package com.atguigu.gmall0218.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

//设计 redis，必须注意用哪种数据类型来存储
/*
     redis 五种数据类型使用场景！
     String:短信验证码，存储一个变量
     hash:json字符串（对象转换的字符串）
        hset(key, field, value);
            hset(key, id, 1);
            hset(key, name, admin);
        hget(key, field);
     list: lpush，pop 队列使用
     set: 去重，交集，并集，补集。。。。不重复！
     zset: 评分，排序
*/

public class RedisUtil {

    //创建连接池
    private JedisPool jedisPool;

    //host,port 等参数可以配置在 application.properties
    //初始化连接池
    public void initJedisPool(String host,int port,int database){
        //直接创建一个连接池配置类
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //设置获取连接池的最大数
        jedisPoolConfig.setMaxTotal(200);
        //设置等待时间
        jedisPoolConfig.setMaxWaitMillis(10*1000);
        //设置最小剩余数
        jedisPoolConfig.setMinIdle(10);
        //开启（获取连接池的）缓冲池
        jedisPoolConfig.setBlockWhenExhausted(true);
        //当用户获取到一个连接池后，自检是否可以使用
        jedisPoolConfig.setTestOnBorrow(true);

        //连接池配置
        jedisPool = new JedisPool(jedisPoolConfig, host, port, 20*1000);
    }

    //获取 Jedis
    public Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }

}
