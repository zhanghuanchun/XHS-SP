package com.wb.service;

import com.wb.entity.PostIndex;

import java.util.List;

public interface PostService {

    List<PostIndex> userPosts(int uid,int start,int stop);

    void insNearbyPost(int postId, double latitude, double longitude);

    List<PostIndex> nearbys(int uid , double latitude, double longitude);
}
