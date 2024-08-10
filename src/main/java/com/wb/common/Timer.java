package com.wb.common;

import java.util.concurrent.TimeUnit;

public interface Timer {

    void createTimerTask(TimerTask task, long delay, TimeUnit unit);

}



