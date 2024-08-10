package com.wb.mapper;

import com.wb.entity.Counter;
import com.wb.entity.Praise;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 计数mapper
 */
@Mapper
public interface CounterMapper {


    int update(Counter counter);

    int insert(Counter counter);

    Counter getById(int id);

    List<Counter> gets(List<Integer> ids);

    Praise getPraise(int cid);

    List<String> getPraises(int uid);

}
