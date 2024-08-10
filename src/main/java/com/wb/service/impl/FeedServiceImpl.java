package com.wb.service.impl;

import com.wb.entity.PostIndex;
import com.wb.mapper.PostMapper;
import com.wb.mapper.UserMapper;
import com.wb.service.FeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Feed实现
 */
public class FeedServiceImpl implements FeedService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    static ThreadPoolExecutor executor = new ThreadPoolExecutor(5
            ,50
            ,10
            , TimeUnit.SECONDS
            , new LinkedBlockingDeque<>(100)
            , Executors.defaultThreadFactory()
            , new ThreadPoolExecutor.AbortPolicy()
    );

    /**
     * 发送Feed
     * @param uid
     * @param postId
     * @return
     */
    @Override
    public int inbox(Integer uid, int postId) {
        //使用MQ来进行削峰
        //循环添加到缓存当中

        //判断是否是大V，这里假设就是不是大V了
        List<Integer> uids = userMapper.getBigVs(uid);
        redisTemplate.opsForZSet().add("outbox:"+uid , postId , new Date().getTime());
        for (int userId:uids){
            redisTemplate.opsForZSet().add("inbox:"+userId , postId , new Date().getTime());
        }
        //添加到数据库当中
        postMapper.insTimeline(uids , postId);
        return 0;
    }

    /**
     * 存入到自己的outbox当中
     * @param uid
     * @param postId
     * @return
     */
    @Override
    public int outbox(int uid, int postId) {
        //放入到缓存当中
        redisTemplate.opsForZSet().add("outbox:"+uid , postId , new Date().getTime());
        return 0;
    }

    /**
     * 用户查询feeds
     * @param uid
     * @return
     */
    @Override
    public List<PostIndex> feeds(int uid) {
        //判断是否是活跃用户
        //这里就当做不是活跃用户来实现了

        //需要查询上一次的查询feeds的时间，这个前端可以记录在本地然后返回，不需要存储到后端，这里方便演示就在后端获取一下
        double nowDate = new Date().getTime();
        double lastDate = (double) redisTemplate.opsForValue().get("inboxLastDate:"+uid);
        //1.读取自己的收件箱
        //Set<Integer> set = redisTemplate.opsForZSet().reverseRangeByScore("inbox:" + uid, lastDate, nowDate);
        //{
        //    5,4,3,2,1
        // }
        //这里有个问题如果是活跃用户就直接返回数据就ok了。但是如果是非活跃用户就需要读取大V的发件箱
        //但是我们这里读取发件箱的话需要连带时间进行排序，所以我们如果是非活跃用户的话就需要分页读取了，用zset返回带时间的
        Set<Object> set = redisTemplate.opsForZSet().reverseRange("inbox:" + uid, 0, 100);
        //{
        // 1,7937129,2,127937
        // }
        //2.获取大V的列表
        List<Integer> bigVs = userMapper.getBigVs(uid);
        //2.异步并发获取大V的发件箱
        for (Integer V : bigVs){
            CompletableFuture<Set> setCompletableFuture = CompletableFuture.supplyAsync(() -> {
                Set Vset = redisTemplate.opsForZSet().reverseRange("outbox:" + V, 0, 100);
                set.addAll(Vset);
                return Vset;
            }, executor);
        }
        //拼接好数据之后进行分类和排序
        //创建两个list
        List<Integer> postIds = new ArrayList<>();
        List<Double> times = new ArrayList<>();
        int n=0;
        for (Object o : set){
            if (n%2 == 0){
                postIds.add((int)o);
            }else{
                times.add((double)o);
            }
        }
        //进行排序，然后返回就ok了

        return null;
    }
}
