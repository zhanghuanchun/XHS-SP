package com.wb.entity;

import lombok.Data;

@Data
public class Counter {

    //主键
    private Integer id;

    //帖子id
    private Integer postId;

    //阅读数
    private Integer readCount;

    //点赞数
    private Integer praiseCount;

    //转发数
    private Integer transmitCount;

    //评论数
    private Integer commentCount;

    //根评论数
    private Integer rootCommentCount;

}
