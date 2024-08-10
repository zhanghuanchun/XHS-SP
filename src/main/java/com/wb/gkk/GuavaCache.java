package com.wb.gkk;

import com.google.common.cache.*;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class GuavaCache {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        LoadingCache<Integer, String> cache = CacheBuilder.newBuilder()
                //设置并发级别为8，并发级别是指可以同时写缓存的线程数
                .concurrencyLevel(8)
                //设置缓存的初始容量为10
                .initialCapacity(10)
                // 设置缓存最大容量为100，超过100之后就会按照LRU最近最少使用算法来移除缓存
                .maximumSize(100)
                //设置写缓存后8秒钟过期
                .expireAfterWrite(8, TimeUnit.SECONDS)
                //设置要统计的缓存命中率
                .recordStats()
                //设置缓存移除通知
                .removalListener(new
                        RemovalListener<Object, Object>() {
                            public void onRemoval(RemovalNotification<Object, Object>
                                                          notification) {
                                System.out.println("----"+notification.getKey() + ":" + notification.getCause());
                            }
                        })
                //build方法可以指定CacheLoader，在缓存不存在时通过CacheLoader的实现自动加载缓存
                .build(
                        new CacheLoader<Integer, String>() {
                            @Override
                            public String load(Integer s) throws Exception {
                                System.out.println("load data:"+ s);
                                String str = s+":cache-value";
                                return str;
                            }
                        }
                );

        for (int i=0; i<20; i++){
            String str = cache.get(1);
            System.out.println(str);
            //休眠一秒
            TimeUnit.SECONDS.sleep(1);
            if (i==10){
                System.out.println("---------------------");
                cache.invalidate(1);
                System.out.println("---------------------");
            }
        }

        //将某个key的缓存直接删除
        cache.invalidate(1);
        //将一批key的缓存直接删除
        cache.invalidateAll(new ArrayList<>());
        //删除所有缓存数据
        cache.invalidateAll();

        //打印缓存的命中率情况
        System.out.println("cache stats:" + cache.stats().toString());
    }
}
