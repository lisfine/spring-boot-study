package com.lhr.service;

import com.lhr.lock.RedisLock;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by LHR on 2018/11/28.
 */
@Service
public class StockService {

    /**
     * redis客户端
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    Logger logger = LoggerFactory.getLogger(StockService.class);


    /**
     * 执行扣库存的脚本
     */
    public static final String STOCK_LUA;

    /**
     * 不限库存
     */
    public static final long UNINITIALIZED_STOCK = -3L;

    static {
        /**
         *
         * @desc 扣减库存Lua脚本
         * 库存（stock）-1：表示不限库存
         * 库存（stock）0：表示没有库存
         * 库存（stock）大于0：表示剩余库存
         *
         * @params 库存key
         * @return
         * 		-3:库存未初始化
         * 		-2:库存不足
         * 		-1:不限库存
         * 		大于等于0:剩余库存（扣减之后剩余的库存）
         * 	    redis缓存的库存(value)是-1表示不限库存，直接返回1
         */
        StringBuilder sb = new StringBuilder();
        sb.append("if (redis.call('exists', KEYS[1]) == 1) then");
        sb.append("    local stock = tonumber(redis.call('get', KEYS[1]));");
        sb.append("    local num = tonumber(ARGV[1]);");
        sb.append("    if (stock == -1) then");
        sb.append("        return -1;");
        sb.append("    end;");
        sb.append("    if (stock >= num) then");
        sb.append("        return redis.call('incrby', KEYS[1], 0 - num);");
        sb.append("    end;");
        sb.append("    return -2;");
        sb.append("end;");
        sb.append("return -3;");
        STOCK_LUA = sb.toString();
    }

    /**
     * 获取库存
     *
     * @param key 库存的key
     * @return -1：不限制库存 大于等于0：剩余库存数
     */
    public int getStock(String key) {
        Integer stock = (Integer) redisTemplate.opsForValue().get(key);
        return stock == null ? -1 : stock;
    }


    /**
     * @param key           库存key
     * @param expire        库存有效时间，秒为单位
     * @param num           扣减数量
     * @param stockCallback 初始化库存回调函数
     * @return -2:库存不足; -1:不限库存; 大于等于0:扣减库存之后的剩余库存
     */
    public Long stock(String key, long expire, int num, IStockCallback stockCallback) {
        long stock = stock(key, num);
        //初始化库存
        if (stock == UNINITIALIZED_STOCK) {
            RedisLock redisLock = new RedisLock(redisTemplate, key);
            try {
                //获取锁
                if (redisLock.tryLock()) {
                    //双重验证，避免并发时重复回源到数据库
                    stock = stock(key, num);
                    if (stock == UNINITIALIZED_STOCK) {
                        //获取初始化库存
                        final int initStock = stockCallback.getStock();
                        //将库存设置到redis
                        redisTemplate.opsForValue().set(key, initStock, expire, TimeUnit.SECONDS);
                        //调用一次减库存的操作
                        stock = stock(key, num);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                redisLock.unlock();
            }
        }
        return stock;
    }


    /**
     * 加库存
     *
     * @param key    库存key
     * @param expire 过期时间（秒）
     * @param num    库存数量
     * @return 库存数量
     */
    public long addStock(String key, Long expire, int num) {
        Boolean hasKey = redisTemplate.hasKey(key);
        //判断是否有key
        if (hasKey) {
            return redisTemplate.opsForValue().increment(key, num);
        }

        Assert.notNull(expire, "初始化库存失败，库存过期时间不能为null");
        RedisLock redisLock = new RedisLock(redisTemplate, key);
        try {
            if (redisLock.tryLock()) {
                //获取到锁后再次判断是否有key
                hasKey = redisTemplate.hasKey(key);
                if (!hasKey) {
                    //初始化内存库
                    redisTemplate.opsForValue().set(key, num, expire, TimeUnit.SECONDS);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            redisLock.unlock();
        }
        return num;
    }

    /**
     * 加库存(还原库存)
     *
     * @param key 库存key
     * @param num 库存数量
     * @return
     */
    public long addStock(String key, int num) {

        return addStock(key, null, num);
    }


    /**
     * 扣库存
     *
     * @param key 库存key
     * @param num 扣减的库存数
     * @return
     */
    public Long stock(String key, int num) {
        //脚本里的KEYS参数
        List<String> keys = new ArrayList<>();
        keys.add(key);
        //脚本里的ARGV参数
        List<String> args = new ArrayList<>();
        args.add(Integer.toString(num));

        Long result = redisTemplate.execute(new RedisCallback<Long>() {
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                Object nativeConnection = connection.getNativeConnection();
                // 集群模式和单机模式虽然执行脚本的方法一样，但是没有共同的接口，所以只能分开执行
                // 集群模式
                if (nativeConnection instanceof JedisCluster) {
                    return (Long) ((JedisCluster) nativeConnection).eval(STOCK_LUA, keys, args);
                }

                // 单机模式
                else if (nativeConnection instanceof Jedis) {
                    return (Long) ((Jedis) nativeConnection).eval(STOCK_LUA, keys, args);
                }
                return UNINITIALIZED_STOCK;
            }
        });
        return result;
    }


}
