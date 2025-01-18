package com.example.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.focus.common.Constants.Companion.breakTimeSeconds
import com.example.focus.common.Constants.Companion.pomodoroTimeSeconds

class AlarmReceiver: BroadcastReceiver() {
    lateinit var timeoutHandler:TimeoutHandler

    override fun onReceive(context: Context, intent: Intent?) {
        println("halo2")
//        timeoutHandler=TimeoutHandler(context)
//        timeoutHandler.simplyPlay()
//        TimerService.isBreakPeriod = !TimerService.isBreakPeriod;
//        if (TimerService.isBreakPeriod) {
//            TimerUtilities.startTimer(System.currentTimeMillis()+ TimerService.breakTimeSeconds * 1000)
//        }
//        else
//            TimerUtilities.startTimer(System.currentTimeMillis()+ TimerService.pomodoroTimeSeconds *1000)
        AlarmManger.isBreakPeriod = !AlarmManger.isBreakPeriod;
        if (AlarmManger.isBreakPeriod) {
            AlarmManger(context).start(System.currentTimeMillis()+ breakTimeSeconds * 1000)
        }
        else
            AlarmManger(context).start(System.currentTimeMillis()+ pomodoroTimeSeconds *1000)
        TimeoutHandler(context).safePlay()
    }
}