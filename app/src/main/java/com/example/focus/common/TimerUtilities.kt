package com.example.focus.common

import com.example.focus.common.Constants.Companion.pulseMs

class TimerUtilities {
    companion object {
        var thread:Thread?=null

        fun isThreadAlive():Boolean{
            return TimerUtilities.thread?.isAlive==true;
        }

        public fun shutDownThread(){
            TimerUtilities.thread?.interrupt()
        }

        fun startTimer(endTime: Long, onSecond: (endTime:Long) -> Unit, onTimerEnd: () -> Unit): Thread {
            var thread = Thread(Runnable {
                try {
                    while (true) {
                        onSecond(endTime);
                        Thread.sleep(pulseMs);
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