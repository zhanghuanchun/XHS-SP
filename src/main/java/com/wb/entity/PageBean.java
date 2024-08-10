package com.wb.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageBean<T> implements Serializable {

    private int pageNum; //当前页数
    private int pageSize; //每页显示数
    private int totalPage; //总页数
    private int totalRecord; //总的记录数
    private List<T> data; //当前页面的数据集合
    private int start;
    private int end;

    public PageBean() {
    }

    public PageBean(int pageNum, int pageSize, int totalRecord) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;

        //计算总页数
        this.totalPage=totalRecord%pageSize==0?(totalRecord/pageSize):(totalRecord/pageSize+1);

        //计算每页的起始下标
        this.start=(pageNum-1)*pageSize;
        this.end=this.start+pageSize;
    }
}

