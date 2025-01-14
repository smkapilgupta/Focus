package com.example.focus
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.focus.common.Constants
import com.example.focus.common.TimerUtilities
import com.example.focus.common.Utilities.Companion.formatTimeFromSeconds

class TimerService: Service() {
    private lateinit var timeoutHandler:TimeoutHandler
    private var startId:Int=0
    private var startTime:Long=0
    companion object {
        public var pomodoroTimeSeconds = Constants.pomodoroTimeSeconds;
        public var breakTimeSeconds= Constants.breakTimeSeconds;
        public var isBreakPeriod=false;
        public var timeRemainingMs:Long=Constants.pomodoroTimeSeconds*1000;
        public var isServiceActive=false;
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("onStartCommand called")
        isServiceActive=true
        createNotificationChannel();
        timeoutHandler=TimeoutHandler(this)
        this.startId=startId
        startTime=System.currentTimeMillis();
        TimerUtilities.initializeTimer(fun (endTime:Long){onSecond(endTime)},fun (){ onTimerEnd() })
        startForeground(startId, createNotification(""));
        return START_STICKY
    }

    override fun onDestroy() {
        println("onDestroy called")
        super.onDestroy()
        isServiceActive=false
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    fun onSecond(endTime:Long){
        timeRemainingMs = Math.max(0,endTime-System.currentTimeMillis())
    }

    fun onTimerEnd(){
        startForeground(this.startId,createNotification("Timer ended"))
        startTime=System.currentTimeMillis()
        isBreakPeriod = !isBreakPeriod;
        if (isBreakPeriod) {
            timeRemainingMs = breakTimeSeconds * 1000
        }
        else
            timeRemainingMs = pomodoroTimeSeconds *1000
        TimerUtilities.startTimer(System.currentTimeMillis()+ timeRemainingMs)
        val thread=Thread({timeoutHandler.recordSensorsAndPlay()})
        thread.start()
        val start=System.currentTimeMillis()
        while(true){
            if(!thread.isAlive)
                break;
            if(thread.isAlive&&(System.currentTimeMillis()-start)>120000){
                thread.interrupt()
                timeoutHandler.simplyPlay()
                break
            }
        }
    }

    fun createNotificationChannel(){
        val serviceChannel = NotificationChannel(
            "CHANNEL_ID",
            "Timer Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(message:String): Notification {
//        val pendingIntent: PendingIntent = //...
        val notificationBuilder = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setContentTitle("Focus")
            .setContentText(message)
            .setSmallIcon(R.mipmap.app_notification_icon)
            .setStyle(NotificationCompat.BigTextStyle())
//            .setContentIntent(pendingIntent)

        return notificationBuilder.build()
    }
}