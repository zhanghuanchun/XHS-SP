package com.wb.gkk;


import com.github.benmanes.caffeine.cache.*;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class CaffeineCache {


    public static void main(String[] args) throws InterruptedException {
        Cache<Integer, String> cache = Caffeine.newBuilder()
                //设置缓存的初始容量为10
                .initialCapacity(10)
                // 设置缓存最大容量为100，超过100之后就会按照LRU最近最少使用算法来移除缓存
                .maximumSize(100)
                //设置写缓存后8秒钟过期
                .expireAfterWrite(8, TimeUnit.SECONDS)
                //设置要统计的缓存命中率
                .recordStats()
                //设置缓存移除通知
                .removalListener((Integer key, String value, RemovalCause cause) -> {
                    System.out.println("缓存删除了！！！！");
                })
                //build方法可以指定CacheLoader，在缓存不存在时通过CacheLoader的实现自动加载缓存
                .build(k -> setValue(1).apply(1));

        //cache.put(1 , "value");
        for (int i=0; i<20; i++){
            String str = cache.getIfPresent(1);
            System.out.println(str);
            //休眠一秒
            TimeUnit.SECONDS.sleep(1);
            if (i==10){
               //cache.put(1 , "value");
                System.out.println("---------------------");
                cache.invalidate(1);
                System.out.println("---------------------");
            }
        }

    }

    public static Function<Integer, String> setValue(Integer key){
        return t -> key + "value";
    }

}
