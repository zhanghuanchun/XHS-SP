package com.wb.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

public class ExampleCache {

    private static final Cache<String, String> cache = CacheBuilder.newBuilder()
            .maximumSize(1000) // 设置最大缓存数量为1000
            .expireAfterWrite(3, TimeUnit.SECONDS) // 设置写入之后过期时间为10分钟
            .build();

    public static void put(String key, String value) {
        cache.put(key, value);
    }

    public static String get(String key) {
        return cache.getIfPresent(key);
    }

    public static void main(String[] args) {
        ExampleCache.put("key1" , "value1");
        String value = ExampleCache.get("key1");
        if (value != null) {
            // 缓存存在，执行业务逻辑
            System.out.println("key1存在");
        } else {
            // 缓存不存在，执行其他逻辑
            System.out.println("key1不存在");
        }
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        value = ExampleCache.get("key1");
        if (value != null) {
            // 缓存存在，执行业务逻辑
            System.out.println("key1存在");
        } else {
            // 缓存不存在，执行其他逻辑
            System.out.println("key1不存在");
        }

    }
}

