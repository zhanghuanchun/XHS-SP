package com.wb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 接口统一返回
 *
 * @author yanhuo
 * @since 2022/4/23
 */
@Data
@Accessors(chain = true)
public class Re<T> {

    /**
     * 返回状态码
     */
    private int code;

    /**
     * 提示信息
     */
    private String msg;

    /**
     * 返回数据
     */
    private T data;

    private Re() {
    }

    private Re(T data) {
        this.data = data;
    }

    private Re(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    private Re(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // 没有返回数据，Re.ok()
    public static Re ok() {
        return new Re(0, "success");
    }

    // 有返回数据，Re.ok(data)
    public static <T> Re<T> ok(T data) {
        return new Re<T>(0, "success", data);
    }
    // 只有提示信息，Re.ok("新增/修改成功")
    public static Re ok(String msg) {
        return new Re(0, msg);
    }
    // Re.error()
    public static Re error() {
        return new Re(1, "error");
    }
    // Re.error("请重试")
    public static Re error(String msg) {
        return new Re(1, msg);
    }
}

