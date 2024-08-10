package com.wb.service;

import com.wb.entity.Comment;
import com.wb.entity.CommentIndex;

import java.util.List;

/**
 * 评论service
 */
public interface CommentService {

    List<Comment> query(int objId);

    //分页查询
    List<Comment> queryPage(int objId,int page);

    int insert(Comment comment);

    int delete(Comment comment);

    int update(Comment comment);

    /**
     * 分页查询root评论
     * @param objId
     * @param start
     * @param end
     * @return
     */
    List<CommentIndex> rootQueryPage(int objId , int start , int end);

    /**
     * 分页查询二级评论
     * @param root
     * @param start
     * @param end
     * @return
     */
    List<CommentIndex> queryPage(int root , int start , int end);

    /**
     * 判断是否点赞过
     * @param commentIndices
     */
    void is_like(List<CommentIndex> commentIndices);

    /**
     * 新增评论
     * @param commentIndex
     */
    void insert(CommentIndex commentIndex);

    /**
     * 点赞评论
     * @param commentIndex
     */
    void like(CommentIndex commentIndex);

    /**
     * 修改或删除评论
     * @param commentIndex
     */
    void update(CommentIndex commentIndex);

    /**
     * 取消点赞
     * @param commentIndex
     */
    void noLike(CommentIndex commentIndex);
}
