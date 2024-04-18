package com.example.focus
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.CombinedVibration
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.focus.common.Constants
import com.example.focus.common.Utilities
import com.example.focus.common.Utilities.Companion.formatTimeFromSeconds
import java.util.concurrent.ScheduledThreadPoolExecutor


class TimerService: Service(), SensorEventListener {
    private lateinit var mediaLoud: MediaPlayer
    private lateinit var mediaQuite: MediaPlayer
    private lateinit var vibrator: VibratorManager
    private lateinit var sensorManager: SensorManager
    private lateinit var keyguardManager: KeyguardManager
    private var maxInaccuracy: Long=0
    private var maxProcessingDelay: Long=0
    private lateinit var accelerometer:Sensor
    private lateinit var magnetometer:Sensor
    private lateinit var lightSensor:Sensor
    private lateinit var proxitySensor:Sensor
    private lateinit var combinedVibrationEffect:CombinedVibration
    private var startId:Int=0
    private var lastTimestamp:Long=0;
    private var startTime:Long=0;
    private var accelerometerReadingDone:Boolean=false;
    private var magnetometerReadingDone:Boolean=false;
    private var lightSensorReadingDone:Boolean=false;
    private var proximitySensorReadingDone:Boolean=false;

    companion object {
        public var pomodoroTimeSeconds = Constants.pomodoroTimeSeconds;
        public var breakTimeSeconds= Constants.breakTimeSeconds;
        public var isBreakPeriod=false;
        public var timeRemainingMs:Long=Constants.pomodoroTimeSeconds*1000;
        public var thread: Thread? =null;
        public var scheduledThreadPoolExecutor: ScheduledThreadPoolExecutor?=null;
        public var handler: Handler?=null;
        public var serviceActive: Boolean = false;
        public var sensorsActive: Boolean = false;
        public var proximitySensorReading: Float = 0.0F;
        public var lightSensorReading: Float = 0.0F;
        public var accelerometerReading: FloatArray=FloatArray(3)
        public var magnetometerReading: FloatArray= FloatArray(3)
        public var orientation: FloatArray= FloatArray(3)
        private var runnable: Runnable? =null;

        public fun isLooperThreadAlive():Boolean{
//            return thread?.isAlive==true;
//            return scheduledThreadPoolExecutor?.isShutdown==false
            if(runnable!=null)
                return handler?.hasCallbacks(runnable!!)==true;
            return false;
        }
        public fun shutDownLooperThread(){
//            thread?.interrupt()
//            TimerService.scheduledThreadPoolExecutor?.shutdown();
            if(runnable!=null)
                handler?.removeCallbacks(runnable!!);
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        createNotificationChannel();
        mediaLoud = MediaPlayer.create(this, R.raw.your_tale_awaits_you_down_the_road_pomodoro_tune);
        mediaQuite = MediaPlayer.create(this, R.raw.trine4_pomodoro);
        vibrator = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrationEffect1 =
            VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE)
        combinedVibrationEffect=CombinedVibration.createParallel(vibrationEffect1);
        keyguardManager=getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager;
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) as Sensor;
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) as Sensor;
        proxitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY) as Sensor;
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT) as Sensor;
        this.startId=startId
        scheduledThreadPoolExecutor=ScheduledThreadPoolExecutor(1);
        runnable= Runnable { onPulse() }
        handler = Handler(Looper.getMainLooper());
        schedulePulses();
        return START_STICKY
    }

    fun schedulePulses(){
        lastTimestamp=System.currentTimeMillis();
        startTime=lastTimestamp
        handler!!.removeCallbacks(runnable!!);
        var pulses:Long=0;
        if(isBreakPeriod)
            pulses= breakTimeSeconds;
        else
            pulses= pomodoroTimeSeconds;
        for (i in 0..pulses+1){
            handler!!.postDelayed(runnable!!,i*Constants.pulseMs);
        }
        sensorManager.unregisterListener(this)
    }

    fun getInaccuracy():Long{
        if(lastTimestamp==startTime)
            return 0;
        return Math.abs((System.currentTimeMillis()-lastTimestamp)-Constants.pulseMs);
    }

    fun getTimeRemaining():Long{
        if(isBreakPeriod)
            return breakTimeSeconds*1000-(System.currentTimeMillis()-startTime);
        return pomodoroTimeSeconds*1000-(System.currentTimeMillis()-startTime);
    }

    fun onPulse(){
        val start=System.currentTimeMillis();
        val currentInaccuracy=getInaccuracy();
        maxInaccuracy=Math.max(currentInaccuracy,maxInaccuracy);
        timeRemainingMs= getTimeRemaining()
        println("Time remaining: "+ timeRemainingMs/1000f);
        lastTimestamp=System.currentTimeMillis();
        if (timeRemainingMs <= 0) {
            isBreakPeriod = !isBreakPeriod;
            Thread{recordSensorsAndPlay()}.start();
            if (isBreakPeriod)
                timeRemainingMs = breakTimeSeconds*1000
            else
                timeRemainingMs = pomodoroTimeSeconds*1000
            schedulePulses();
        }
        startForeground(startId, createNotification(formatTimeFromSeconds(timeRemainingMs/1000)
                +(if(isBreakPeriod) {"\nTake a break"} else {"\nFocus on job"}+"\nCurrent inaccuracy: "+currentInaccuracy+"\nMax inaccuracy: "+maxInaccuracy+"ms\nMax processing delay: "+maxProcessingDelay+"ms \n" +
                "isInsidePocket: "+Utilities.isInsidePocket(orientation, lightSensorReading, proximitySensorReading)+"\nisFacingDown: "+Utilities.isFacingDown(
            orientation)+"\nisDark: "+Utilities.isDark(lightSensorReading)+"\nisSomethingInProximity: "+Utilities.isSomethingInProximity(
            proximitySensorReading)+"\nisSomethingCoveringScreen: "+Utilities.isSomethingCoveringScreen(lightSensorReading, proximitySensorReading)
                +"\nisOnHorizontalSurface: "+Utilities.isOnHorizontalSurface(orientation)+"\nisFacingDown: "
                +Utilities.isFacingDown(orientation))));
        serviceActive=true;
        val processingDelay=System.currentTimeMillis()-start;
        maxProcessingDelay=Math.max(maxProcessingDelay,processingDelay)
    }

    fun recordSensorsAndPlay(){
        var start=System.currentTimeMillis();
        recordSensors()
        recordSensors()
        val processingDelay=System.currentTimeMillis()-start;
        maxProcessingDelay=Math.max(maxProcessingDelay,processingDelay)
        if(Utilities.isInsidePocket(orientation, lightSensorReading, proximitySensorReading)||Utilities.isFacingDown(
                orientation)||!keyguardManager.isKeyguardLocked)
            startPlayer(mediaQuite);
        else
            startPlayer(mediaLoud);
        vibrator.cancel();
        vibrator.vibrate(combinedVibrationEffect)
    }

    fun recordSensors(){
        val start=System.currentTimeMillis()
        accelerometerReadingDone=false;
        magnetometerReadingDone=false;
        lightSensorReadingDone=false;
        proximitySensorReadingDone=false;
        while(!accelerometerReadingDone||!magnetometerReadingDone||!lightSensorReadingDone||!proximitySensorReadingDone){
            if(System.currentTimeMillis()-start>Constants.breakTimeSeconds*1000)
                break;
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager?.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager?.registerListener(this, proxitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager?.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            println(""+accelerometerReadingDone+" "+magnetometerReadingDone+" "+lightSensorReadingDone+" "+proximitySensorReadingDone)
        }
        sensorManager.unregisterListener(this);
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceActive=false;
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
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
            .setContentText(Constants.TIMER_RUNNING+message)
            .setSmallIcon(R.mipmap.app_notification_icon)
            .setStyle(NotificationCompat.BigTextStyle())
//            .setContentIntent(pendingIntent)

        return notificationBuilder.build()
    }

    private fun startPlayer(mediaPlayer: MediaPlayer){
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager;
        val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,maxVolume,0);
        mediaPlayer.setOnCompletionListener {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,originalVolume,0);
        }
        mediaPlayer.start();
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event?.sensor?.type==Sensor.TYPE_ACCELEROMETER) {
            accelerometerReading= event.values
            accelerometerReadingDone=true
            println("accelerometer " +event.values.get(0)+" "+event.values.get(1)+" "+event.values.get(2))
//            println("isMoving " +Utilities.isMoving(accelerometerReading));
        }
        if(event?.sensor?.type==Sensor.TYPE_MAGNETIC_FIELD) {
            magnetometerReading= event.values
            magnetometerReadingDone=true
            println("magnetometer" +event.values.get(0)+" "+event.values.get(1)+" "+event.values.get(2))
        }
        if(event?.sensor?.type==Sensor.TYPE_PROXIMITY) {
            proximitySensorReading=event.values.get(0);
            proximitySensorReadingDone=true
            println("proximity " +event.values.get(0))
        }
        if(event?.sensor?.type==Sensor.TYPE_LIGHT) {
            lightSensorReading=event.values.get(0);
            lightSensorReadingDone=true
            println("light " +event.values.get(0))
        }
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)

        SensorManager.getOrientation(rotationMatrix, orientation)
        Utilities.convertRadianOrientationToDegree(orientation);
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}