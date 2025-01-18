package com.example.focus.common

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.example.focus.common.Constants.Companion.pomodoroTimeSeconds

class TimerUtilities {
    companion object {
        var thread:Thread?=null
        var isTimerActive:Boolean=false
        private lateinit var alarmManager:AlarmManager
        private lateinit var pendingIntent:PendingIntent
        public var endTime:Long=0;
        public var timeRemainingMs:Long=pomodoroTimeSeconds*1000

        fun pauseTimer():Long{
            isTimerActive=false
            timeRemainingMs=endTime-System.currentTimeMillis()
            alarmManager.cancel(pendingIntent)
            return endTime-System.currentTimeMillis()
        }

        fun resetTimer(){
            endTime=System.currentTimeMillis()*2
            isTimerActive=false
            println("halo: "+alarmManager.nextAlarmClock.triggerTime+" "+System.currentTimeMillis())
            alarmManager.cancel(pendingIntent)
        }

        fun startTimer(endTime:Long){
            TimerUtilities.endTime=endTime
            isTimerActive=true
            println("halo ce: "+endTime)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,endTime, pendingIntent)
            println("halo 3: "+alarmManager.nextAlarmClock.triggerTime +" "+System.currentTimeMillis())
        }

        fun initializeTimer(context: Context) {
        }
    }
}