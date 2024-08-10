package com.wb.service;

import com.wb.entity.Counter;
import com.wb.entity.Re;
import com.wb.entity.User;

import java.util.HashMap;
import java.util.List;

public interface CounterService {

    int updateReadCount(int id);

    int updatePraiseCount(int id , int type);

    int updateTransmitCount(int id , int type);

    int updateCommentCount(int id , int type);

    int insert(Counter counter);

    Re<Counter> getById(int id);

    List<Counter> gets(List<Integer> ids);

    Re<HashMap<Long , Integer>> getPraises(List<Object> cids , int uid);

    /**
     * 保存点赞详情
     * @param pid
     * @param uid
     */
    void insLikeDetails(int pid , int uid);

    List<User> likeDetails(int pid);



}
