package com.wb.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间类
 */
public class DateUtils {

    /**
     * 获取当前时间字符串
     * @param date
     * @return
     */
    public static String getDate(Date date){
        SimpleDateFormat dateFormat= new SimpleDateFormat("yyyy-MM-dd :hh:mm:ss");
        return dateFormat.format(date);
    }

    public static Long getDate(String time) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time).getTime();
    }
}
