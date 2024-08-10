package com.wb.entity;

import lombok.Data;

@Data
public class PostIndex {

    private Integer id;
    private String title;
    private Integer uid;
    private Integer channelId;
    private String channelName;
    private Integer flag;
    private Integer type;
    private String address;
    private Integer provinceId;
    private Integer cityId;
    private Integer countyId;
    private String labels;
    private String createTime;
    private String updateTime;
    private Integer isDelete;

    //点赞计数
    private Integer praiseCount;

    //经度
    private Double longitude;
    //纬度
    private Double latitude;
    //距离
    private Long distance;
}
