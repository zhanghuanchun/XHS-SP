package com.wb.entity;

import lombok.Data;

/**
 * 评论内容表
 */
@Data
public class CommentContent {

    private Integer commentIndex;//评论id

    private String message;//评论内容

    private String createTime;//创建时间

    private String updateTime;//修改时间

    /**
     * 还有比如评论背景，是否热评，好评等，都可以写在内容表当中
     */
}
