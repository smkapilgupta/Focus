package com.example.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.focus.common.Constants.Companion.breakTimeSeconds
import com.example.focus.common.Constants.Companion.pomodoroTimeSeconds

class AlarmReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        AlarmManger.isBreakPeriod = !AlarmManger.isBreakPeriod;
        if (AlarmManger.isBreakPeriod) {
            AlarmManger(context).start(System.currentTimeMillis()+ breakTimeSeconds * 1000)
        }
        else
            AlarmManger(context).start(System.currentTimeMillis()+ pomodoroTimeSeconds *1000)
        TimeoutHandler(context).safePlay()
    }
}