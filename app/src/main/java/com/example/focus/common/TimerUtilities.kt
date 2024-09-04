package com.example.focus.common

import com.example.focus.common.Constants.Companion.pulseMs

class TimerUtilities {
    companion object {
        var thread:Thread?=null
        var isTimerActive:Boolean=false
        private var endTime:Long=0;

        fun pauseTimer(){
            isTimerActive=false
        }

        fun resetTimer(){
            endTime=System.currentTimeMillis()*2
            isTimerActive=false
        }

        fun startTimer(endTime:Long){
            TimerUtilities.endTime=endTime
            isTimerActive=true
        }

        fun initializeTimer(onSecond: (endTime:Long) -> Unit, onTimerEnd: () -> Unit) {
            endTime=System.currentTimeMillis()*2

            var thread = Thread(Runnable {
                try {
                    while (true) {
                        if(isTimerActive) {
                            onSecond(endTime);
                        }
                        Thread.sleep(pulseMs);
                        if (isTimerActive && System.currentTimeMillis() >= endTime) {
                            onTimerEnd()
                        }
                    }
                } catch (_: InterruptedException) {
                }
            });
            thread.start();
            TimerUtilities.thread=thread;
        }
    }
}