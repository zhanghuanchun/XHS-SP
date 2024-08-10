package com.wb.hotkey;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class HotSpotDetector {

    //sku_1 1000 5 10
    private final int WINDOW_SIZE = 10; // 滑动窗口大小，单位为秒
    private final int THRESHOLD = 5; // 阈值，达到该条件即视为热点数据
    private final Cache<String, Object> hotCache = CacheBuilder
            .newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .maximumSize(1000).build(); // 缓存热点数据

    private Map<String, Queue<Long>> window = new HashMap<>();
    private Map<String, Integer> counts = new HashMap<>();

    // 判断是否为热点数据
    public boolean isHot(String data) {
        //如果缓存中有数据，直接返回ture
        if (hotCache.getIfPresent(data)!=null){
            return true;
        }
        // 获取当前数据在计数器中的统计次数
        int count = counts.getOrDefault(data, 0);
        // 如果统计次数大于等于阈值，将该数据加入热点缓存，清空队列并返回true
        if (count >= THRESHOLD) {
            hotCache.put(data, 1); // 将热点数据放入缓存
            clear(data); // 处理完毕后将队列清空
            return true;
        } else {
            // 如果统计次数小于阈值，则更新计数器，维护时间窗口，并返回false
            counts.put(data, count + 1);
            // 获取对应数据的时间队列
            Queue<Long> queue = window.get(data);
            // 如果该队列为null，创建一个新的LinkedList队列
            if (queue == null) {
                queue = new LinkedList<Long>();
                window.put(data, queue);
            }
            // 获取当前时间（秒级）
            long currTime = System.currentTimeMillis() / 1000;
            queue.add(currTime);// 将当前时间加入时间队列中，用于后面数据滑动窗口的统计
            // 如果队列中数据的时间超过了滑动窗口的时间区间，则将该时间从队列中移除
            while (!queue.isEmpty() && currTime - queue.peek() > WINDOW_SIZE) {
                queue.poll();
                counts.put(data, counts.get(data) - 1); // 统计次数-1
            }
            return false;
        }
    }

    // 清除指定数据的队列和计数
    private void clear(String data) {
        window.remove(data);
        counts.remove(data);
    }

    //添加数据到本地缓存
    public void set(String key , Object value){
        hotCache.put(key , value);
    }

    public Object get(String key){
        return hotCache.getIfPresent(key);
    }


    public static void main(String[] args) throws InterruptedException {
        HotSpotDetector detector = new HotSpotDetector();
        Random random = new Random();
        String[] testData = {"A", "B", "C", "D", "E", "F"};

        // 模拟数据访问
//        for (int i = 0; i < 20; i++) {
//            int index = random.nextInt(6);
//            String data = testData[index];
//            System.out.println("Access Data: " + data);
//            boolean isHotspot = detector.isHot(data);
//            if (isHotspot) {
//                System.out.println("Hotspot Detected: " + data);
//            }
//
//        }

//        Thread.sleep(6000); // 模拟访问间隔
        detector.isHot("C");
        detector.isHot("C");
        detector.isHot("C");
        detector.isHot("C");
        detector.isHot("C");
        boolean isHotspot = detector.isHot("C");
        if (isHotspot) {
            System.out.println("----Hotspot Detected: C");
            detector.get("C");
        }else{
            System.out.println("----not Hotspot Detected: C");
        }

    }
}
