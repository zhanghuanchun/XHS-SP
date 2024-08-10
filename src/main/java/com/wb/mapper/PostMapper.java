package com.wb.mapper;

import com.wb.entity.PostContent;
import com.wb.entity.PostIndex;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PostMapper {

    PostIndex getPostIndex(int id);

    PostContent getPostContent(int id);

    List<PostIndex> getPostIndexs(int uid);

    int insTimeline(List<Integer> uids,int postId);
}
