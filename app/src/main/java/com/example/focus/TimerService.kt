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
import java.util.concurrent.TimeUnit


class TimerService: Service(), SensorEventListener, Runnable {
    private lateinit var mediaLoud: MediaPlayer
    private lateinit var mediaQuite: MediaPlayer
    private lateinit var vibrator: VibratorManager
    private lateinit var sensorManager: SensorManager
    private lateinit var keyguardManager: KeyguardManager
    private var maxInaccuracy: Long=0
    private var maxProcessingDelay: Long=0
    private var missedSensorInput:Boolean=false;
    private lateinit var accelerometer:Sensor
    private lateinit var magnetometer:Sensor
    private lateinit var lightSensor:Sensor
    private lateinit var proxitySensor:Sensor
    private lateinit var combinedVibrationEffect:CombinedVibration
    private var startId:Int=0
    private var lastTimestamp:Long=0;
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

        public fun isLooperThreadAlive():Boolean{
            return thread?.isAlive==true;
        }
        public fun shutDownLooperThread(){
            thread?.interrupt()
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
        handler = Handler(Looper.getMainLooper());
        handler!!.postDelayed(this,Constants.pulseMs)
        lastTimestamp=System.currentTimeMillis();
        return START_STICKY
    }
    override fun run(){
        val start=System.currentTimeMillis();
        maxInaccuracy=Math.max(maxInaccuracy,Math.abs((System.currentTimeMillis()-lastTimestamp)-Constants.pulseMs));
        timeRemainingMs= timeRemainingMs- (System.currentTimeMillis() - lastTimestamp);
        lastTimestamp=System.currentTimeMillis();
        if (timeRemainingMs <= 0) {
            isBreakPeriod = !isBreakPeriod;
            if(Utilities.isInsidePocket(orientation, lightSensorReading, proximitySensorReading)||Utilities.isFacingDown(
                    orientation)||!keyguardManager.isKeyguardLocked)
                startPlayer(mediaQuite);
            else
                startPlayer(mediaLoud);
            if(sensorsActive) {
                sensorManager.unregisterListener(this)
                sensorsActive = false;
            }
            else{
                missedSensorInput=true;
            }
            vibrator.cancel();
            vibrator.vibrate(combinedVibrationEffect)
            if (isBreakPeriod)
                timeRemainingMs = breakTimeSeconds*1000
            else
                timeRemainingMs = pomodoroTimeSeconds*1000
        }
        if(timeRemainingMs<=2*Constants.pulseMs && !sensorsActive){
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager?.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager?.registerListener(this, proxitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager?.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorsActive=true;
        }
        startForeground(startId, createNotification(formatTimeFromSeconds(timeRemainingMs/1000)+(if(isBreakPeriod) "\nTake a break" else "\nFocus on job\nMax inaccuracy: "+maxInaccuracy+"\nMissed sensor input: "+missedSensorInput+"\nMax processing delay: "+maxProcessingDelay)));
        serviceActive=true;
        val processingDelay=System.currentTimeMillis()-start;
        maxProcessingDelay=Math.max(maxProcessingDelay,processingDelay)
        handler?.postDelayed(this,Math.max(0,Constants.pulseMs-processingDelay));
    }

    override fun onDestroy() {
        serviceActive=false;
        super.onDestroy()
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
            println("accelerometer " +event.values.get(0)+" "+event.values.get(1)+" "+event.values.get(2))
//            println("isMoving " +Utilities.isMoving(accelerometerReading));
        }
        if(event?.sensor?.type==Sensor.TYPE_MAGNETIC_FIELD) {
            magnetometerReading= event.values
            println("magnetometer" +event.values.get(0)+" "+event.values.get(1)+" "+event.values.get(2))
        }
        if(event?.sensor?.type==Sensor.TYPE_PROXIMITY) {
            proximitySensorReading=event.values.get(0);
            println("proximity " +event.values.get(0))
        }
        if(event?.sensor?.type==Sensor.TYPE_LIGHT) {
            lightSensorReading=event.values.get(0);
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