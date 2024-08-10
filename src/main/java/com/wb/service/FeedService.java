package com.wb.service;

import com.wb.entity.PostIndex;

import java.util.List;

/**
 * 用来发送到用户的收件箱中
 */
public interface FeedService {

    int inbox(Integer uid, int postId);

    int outbox(int uid,int postId);

    List<PostIndex> feeds(int uid);
}
