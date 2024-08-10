package com.wb.service.impl;

import com.wb.entity.Follower;
import com.wb.entity.Re;
import com.wb.entity.User;
import com.wb.mapper.UserMapper;
import com.wb.service.RelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.Topic;

import java.util.List;

public class RelationServiceImpl implements RelationService {

    @Autowired
    //操作redis
    private RedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    //查询粉丝列表
    @Override
    public Re<List<User>> getFollowerList(int userId, int type , int start , int stop) {
        //查询之前首先判断用户是否允许查询,如果不允许直接返回
        //然后就是一系列的判断

        //type这个字段用来区分本人查询还是其他用户查询
        if (type==0){
            //其他用户查询
            //type=0表示是查询粉丝列表，=1表示查询关注列表
            List<User> users = other(userId, 0, start, stop);
            return Re.ok(users);
        }else{
            //用户本人查询
            List<User> users = oneself(userId, 0, start, stop);
            return Re.ok(users);
        }

    }

    //查询关注列表
    @Override
    public Re<List<User>> getAttentionList(int userId, int type) {
        //查询之前首先判断用户是否允许查询,如果不允许直接返回
        if (type==0){
            //其他用户查询
            List<User> users = other(userId, 1, 0, 10);
            return Re.ok(users);
        }else{
            //用户本人查询
            //现在微博和小红书，自己查询的时候是通过es进行查询，用户关注列表一般最多也就一两千，
            // 不会再多，所以用户自己查询关注列表的时候，是走es进行查询
        }
        return null;
    }

    //用户本人查询
    public List<User> oneself(int userId , int type , int start , int stop){
        //用户自己查询分页可以不做限制，就是直接从数据库查询
        //但是为了避免轮询查询所有库导致效率低，可以使用mget
        List<String> followers = userMapper.getFollowers(userId, start, stop);
        List<User> users = redisTemplate.opsForValue().multiGet(followers);
        //假如他真的是一个很冷的用户，甚至一个月两个月都不用这个软件一次。
        //mget查询不到他的信息也没有关系。
        //我们一般去查询列表其实都是查询大v 100条 也可以忽略不计
        //你直接使用mysql进行查询user_info信息进行返回就ok。
        return users;
    }

    //其他用户查询
    public List<User> other(int userId , int type , int start , int stop){
        if (type==0){
            //分页从redis当中进行查询
            List<String> followerIds = redisTemplate.opsForList().range("followerId:" + userId, start, stop);

            if (followerIds!=null && followerIds.size()>0){
                //使用mget进行批量查询
                List<User> users = redisTemplate.opsForValue().multiGet(followerIds);
                return users;
            }
            //用户自己查询分页可以不做限制，就是直接从数据库查询
            //但是为了避免轮询查询所有库导致效率低，可以使用mget
            List<String> followers = userMapper.getFollowers(userId, start, stop);
            List<User> users = redisTemplate.opsForValue().multiGet(followers);

            //加入缓存当中，就算没有粉丝也加入
            redisTemplate.opsForList().leftPushAll("followerId:" + userId , followers);
            return users;
        }
        List<String> attentionIds = redisTemplate.opsForList().range("attentionId:" + userId, start, stop);
        if (attentionIds!=null && attentionIds.size()>0){
            //使用mget进行批量查询
            List<User> users = redisTemplate.opsForValue().multiGet(attentionIds);
            return users;
        }
        return null;

    }
}
