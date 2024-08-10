package com.wb.service;

import com.wb.entity.Re;
import com.wb.entity.User;

import java.util.List;

/**
 * 查询关系列表
 */
public interface RelationService {

    //查询粉丝列表
    Re<List<User>> getFollowerList(int userId , int type , int start , int stop);

    //查询关注列表
    Re<List<User>> getAttentionList(int userId , int type);
}
