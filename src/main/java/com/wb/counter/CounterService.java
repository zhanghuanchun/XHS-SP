package com.wb.counter;


import com.wb.entity.CounterConfig;

/**
 * 计数中台
 *
 */
public interface CounterService {

    int blurCount(CounterConfig counterConfig);

    int preciseCount(CounterConfig counterConfig);



}
