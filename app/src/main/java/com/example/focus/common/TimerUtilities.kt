package com.example.focus.common

import com.example.focus.TimerService

class TimerUtilities {
    companion object {
        fun startTimer(endTime: Long, onSecond: (endTime:Long) -> Unit, onTimerEnd: () -> Unit): Thread {
            var thread = Thread(Runnable {
                try {
                    while (true) {
                        onSecond(endTime);
                        Thread.sleep(1000);
                        if (System.currentTimeMillis() >= endTime) {
                            onTimerEnd()
                            break
                        }
                    }
                } catch (_: InterruptedException) {
                }
            });
            thread.start();
            return thread
        }
    }
}