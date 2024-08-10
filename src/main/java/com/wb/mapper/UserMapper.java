package com.wb.mapper;

import com.wb.entity.Attention;
import com.wb.entity.Follower;
import com.wb.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户mapper
 */
@Mapper
public interface UserMapper {

    //通过ID查询用户
    User getById(int id);

    //修改用户的信息
    int update(User user);

    //修改用户的粉丝数
    int updateUserAttentionCount(int userId);

    //修改用户的关注数
    int updateUserFollowerCount(int userId);

    //查询关注列表
    List<String> getAttentions(int userId,int start,int stop);

    //查询粉丝列表
    List<String> getFollowers(int userId,int start,int stop);

    int insertAttention(Attention attention);

    int insertFollower(Follower follower);

    List<Integer> getBigVs(int userId);

//    int AttentionCount(int userId);
//
//    int FollowerCount(int userId);

}
