package com.wb.entity;

import lombok.Data;

/**
 * 评论索引表
 */
@Data
public class CommentIndex {

    private Integer id;//主键id

    private Integer objId; //对象id(帖子，视频，商品留言等)

    private Integer type;//对象类型(1:帖子，2:视频，3:商品）

    private Integer memberId;//发表者id

    private Integer root;//根评论id，不为0是回复评论

    private Integer parent;//父评论id，为0是root评论

    private Integer floor;//评论楼层

    private Integer like;//点赞数

    private Integer state;//状态，0：正常；1：隐藏

    private Integer attrs;//属性，0：不置顶；1：置顶

    private Integer count;//父评论下的所有子评论的总数

    private String createTime;//创建时间

    private String updateTime;//修改时间

    /**
     * 拓展
     */
    private String message;

    private Integer is_like;
}
