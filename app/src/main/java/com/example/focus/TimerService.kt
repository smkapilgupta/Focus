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
import java.time.LocalDateTime
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.UUID

class TimerService: Service(), SensorEventListener {
    private lateinit var mediaQuite: MediaPlayer
    private lateinit var vibrator: VibratorManager
    private lateinit var sensorManager: SensorManager
    private lateinit var keyguardManager: KeyguardManager
    private var maxInaccuracy: Long=0
    private var notificationTime: Long=0
    private var sensorRecordingTime: Long=0
    private var lastAlarmInstanceDelay: Long=0
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
    private var lastBreakInstances=arrayOf(LocalDateTime.now().toString())
    private var loudTunes=arrayOf(R.raw.sound_notif2_132674, R.raw.your_tale_awaits_you_down_the_road_pomodoro_tune, R.raw.saoirse_notification, R.raw.saoirse_device_connect,R.raw.halo_canticles, R.raw.saoirse_device_disconnect, R.raw.shadow_of_mordor,R.raw.reapers_coast_pomodoro_tune)
    companion object {
        public var pomodoroTimeSeconds = Constants.pomodoroTimeSeconds;
        public var breakTimeSeconds= Constants.breakTimeSeconds;
        public var isBreakPeriod=false;
        public var timeRemainingMs:Long=Constants.pomodoroTimeSeconds*1000;
        public var thread: Thread? =null;
        public var serviceActive: Boolean = false;
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
        println("onStartCommand called")
        createNotificationChannel();
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
        thread = Thread(Runnable {
            try {
                schedulePulses()
                while (true) {
                    onPulse();
                    Thread.sleep(1000);
                }
            }
            catch (_:InterruptedException){

            }
        });
        startForeground(startId, createNotification(formatTimeFromSeconds(timeRemainingMs/1000)
                +(if(isBreakPeriod) {"\nTake a break"} else {"\nFocus on job"})));
        thread!!.start();
        return START_STICKY
    }

    fun schedulePulses(){
        println("Scheduling pulses")
        lastTimestamp=System.currentTimeMillis();
        if(isBreakPeriod)
            startTime=lastTimestamp+ timeRemainingMs-breakTimeSeconds*1000
        else
            startTime=lastTimestamp+ timeRemainingMs-pomodoroTimeSeconds*1000
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
        lastTimestamp=System.currentTimeMillis();
        if (timeRemainingMs <= 0) {
            lastAlarmInstanceDelay=Math.abs(timeRemainingMs);
            isBreakPeriod = !isBreakPeriod;
            Thread{recordSensorsAndPlay()}.start();
            if (isBreakPeriod) {
                timeRemainingMs = breakTimeSeconds * 1000
                lastBreakInstances=arrayOf(LocalDateTime.now().toString())+lastBreakInstances
                lastBreakInstances=lastBreakInstances.copyOfRange(0,Math.min(10,lastBreakInstances.size-1))
            }
            else
                timeRemainingMs = pomodoroTimeSeconds*1000
            schedulePulses();
        }
        serviceActive=true;
        val processingDelay=System.currentTimeMillis()-start;
        notificationTime=Math.max(notificationTime,processingDelay)
    }

    fun generateRandomNumber():Int{
        var uuid=UUID.randomUUID().toString()
        var sumOfChars=0;
        for(i:Int in 0..uuid.length-1){
            sumOfChars+=uuid.get(i).code
        }
        return sumOfChars
    }
    fun recordSensorsAndPlay(){
        var start=System.currentTimeMillis();
        recordSensors()
        recordSensors()
        sensorRecordingTime=System.currentTimeMillis()-start;
        vibrator.cancel()
        if(Utilities.isInsidePocket(orientation, lightSensorReading, proximitySensorReading)||Utilities.isFacingDown(
                orientation)||!keyguardManager.isKeyguardLocked) {
            if(keyguardManager.isKeyguardLocked)
                vibrator.vibrate(combinedVibrationEffect)
            startPlayer(mediaQuite);
        }
        else {
            var mediaLoud: MediaPlayer= MediaPlayer.create(this, loudTunes[generateRandomNumber()%loudTunes.size]);
            startPlayer(mediaLoud);
            vibrator.vibrate(combinedVibrationEffect)
        }
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
        println("onDestroy called")
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