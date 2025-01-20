package com.example.focus

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.focus.common.Constants.Companion.INTENT_CODE
import com.example.focus.common.Constants.Companion.pomodoroTimeSeconds

class AlarmManger (private val context: Context) {
    companion object{
        var timeRemainingMs:Long= pomodoroTimeSeconds*1000
        var endTime:Long=0
        var isTimerActive:Boolean=false
        var isBreakPeriod:Boolean=false
    }

    private val alarmManager=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val pendingIntent = PendingIntent.getBroadcast(
        context,
        INTENT_CODE,
        Intent(context, AlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    @SuppressLint("ScheduleExactAlarm")
    fun start(endTimeMs:Long){
        endTime=endTimeMs
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,endTimeMs,pendingIntent)
        isTimerActive=true
    }

    fun stop(){
        alarmManager.cancel(pendingIntent)
        timeRemainingMs= pomodoroTimeSeconds*1000
        isBreakPeriod=false
        isTimerActive=false
    }

    fun pause(){
        alarmManager.cancel(pendingIntent)
        timeRemainingMs= endTime-System.currentTimeMillis()
        isTimerActive=false
    }
}
