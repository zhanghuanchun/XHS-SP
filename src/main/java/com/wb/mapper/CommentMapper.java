package com.wb.mapper;

import com.wb.entity.CommentContent;
import com.wb.entity.CommentIndex;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {

    int updateCommentIndex(CommentIndex commentIndex);

    int insertCommentIndex(CommentIndex commentIndex);

    int insertCommentContent(CommentContent commentContent);

    List<CommentIndex> pageRootComment(int objId,int start , int end);

    List<CommentIndex> pageSecondComment(@Param("id") int id , @Param("start") int start , @Param("end") int end);

    List<CommentIndex> contents(@Param("commentIds") List<Integer> commentIds);

    List<CommentIndex> rootComment(int objId);
}
