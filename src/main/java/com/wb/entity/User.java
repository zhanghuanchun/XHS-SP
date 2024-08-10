package com.wb.entity;

import lombok.Data;

/**
 * 用户实体类
 */
@Data
public class User {
    private Integer id;
    private String name;
    private Integer sex;
    private Integer age;
    private String date;
    private Integer status;
    private Integer attentionCount;
    private Integer followerCount;

}
