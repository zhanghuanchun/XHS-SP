package com.wb.service.impl;

import com.wb.service.UserSignInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;

public class UserSignInServiceImpl implements UserSignInService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 用户签到.
     *
     * @param userId  用户 ID
     * @return true-签到成功,false-已签到过
     */
    public void signIn(String userId) {
        // 获取当前年份和当前周数（以周一为第一天）
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int week = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        // 定义存储签到信息的key
        String key = "sign:" + year + ":" + week + ":" + userId;

        // 计算当前日期是本周第几天
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        int dayIndex = dayOfWeek.getValue() % 7; // 周日为第0天，需要转换一下

        // 设置签到位为1，并返回原来的位值
        Boolean isSigned = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            return connection.setBit(key.getBytes(), dayIndex, true);
        });

        // 如果签到位原来是0，则表示成功签到，可以做一些相关处理
        if (isSigned) {
            // TODO: 签到成功后的处理
        }

    }

    /**
     * 查询用户签到情况
     * @param userId
     * @return
     */
    public boolean[] getSign(int userId){
        // 获取当前年份和当前周数（以周一为第一天）
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int week = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        // 定义存储签到信息的key
        String key = "sign:" + year + ":" + week + ":" + userId;

        // 获取用户本周的签到情况，返回一个数组，其中1表示已签到，0表示未签到
        byte[] bytes = redisTemplate.execute((RedisCallback<byte[]>) connection -> {
            return connection.get(key.getBytes());
        });
        boolean[] signStatus = new boolean[7];
        for (int i = 0; i < 7; i++) {
            signStatus[i] = (bytes[i / 8] & (1 << (i % 8))) != 0;
        }

        return signStatus;
    }

}
