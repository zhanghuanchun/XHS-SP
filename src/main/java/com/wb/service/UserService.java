package com.wb.service;

import com.wb.entity.Attention;
import com.wb.entity.Follower;
import com.wb.entity.Re;
import com.wb.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * userService
 */
@Service
public interface UserService {

    //获取用户的信息
    Re<User> getUser(int id);

    //修改用户信息
    Re<String> update(User user);

    //获取粉丝列表
    Re<List<User>> getFollower(int userId,int start);

    //获取关注列表
    Re<List<User>> getAttention(int userId,int start);

    //用户点关注
    Re<String> insertAttention(Attention attention);

    //粉丝点关注
    Re<String> insertFollower(Follower follower);

    //用户取消关注
    Re<String> deleteFollower(Follower follower);

}
