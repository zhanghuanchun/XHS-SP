package com.wb.entity;

import lombok.Data;

/**
 * 评论计数
 */
@Data
public class CommentCount {

    private Integer id;

    private Integer rootCommentCount;

    private Integer commentCount;
}
