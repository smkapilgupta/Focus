package com.example.focus

import android.app.KeyguardManager
import android.app.Service
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import com.example.focus.common.Constants
import com.example.focus.common.Utilities
import com.example.focus.common.Utilities.Companion.generateRandomNumber

class TimeoutHandler: SensorEventListener {
    private var mediaQuite: MediaPlayer
    private var vibrator: VibratorManager
    private var sensorManager: SensorManager
    private var keyguardManager: KeyguardManager
    private var sensorRecordingTime: Long=0
    private var accelerometer: Sensor
    private var magnetometer: Sensor
    private var lightSensor: Sensor
    private var proxitySensor: Sensor
    private var combinedVibrationEffect: CombinedVibration
    private var proximitySensorReading: Float = 0.0F;
    private var lightSensorReading: Float = 0.0F;
    private var accelerometerReading: FloatArray=FloatArray(3)
    private var magnetometerReading: FloatArray= FloatArray(3)
    private var orientation: FloatArray= FloatArray(3)
    private var accelerometerReadingDone:Boolean=false;
    private var magnetometerReadingDone:Boolean=false;
    private var lightSensorReadingDone:Boolean=false;
    private var proximitySensorReadingDone:Boolean=false;
    private var loudTunes=arrayOf(R.raw.sound_notif2_132674, R.raw.your_tale_awaits_you_down_the_road_pomodoro_tune, R.raw.saoirse_notification, R.raw.saoirse_device_connect,R.raw.halo_canticles, R.raw.saoirse_device_disconnect, R.raw.shadow_of_mordor,R.raw.reapers_coast_pomodoro_tune)
    private var context:Context?
    constructor(context:Context?){
        this.context=context
        mediaQuite = MediaPlayer.create(context, R.raw.trine4_pomodoro);
        vibrator = context?.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrationEffect1 =
            VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE)
        combinedVibrationEffect=CombinedVibration.createParallel(vibrationEffect1);
        keyguardManager=context?.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        sensorManager = context?.getSystemService(Service.SENSOR_SERVICE) as SensorManager;
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) as Sensor;
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) as Sensor;
        proxitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY) as Sensor;
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT) as Sensor;
    }
    public fun recordSensorsAndPlay(){
        var start=System.currentTimeMillis();
        recordSensors()
        recordSensors()
        sensorRecordingTime=System.currentTimeMillis()-start;
        vibrator.cancel()
        if(Utilities.isInsidePocket(
                orientation,
                lightSensorReading,
                proximitySensorReading
            )|| Utilities.isFacingDown(
                orientation
            )||!keyguardManager.isKeyguardLocked) {
            if(keyguardManager.isKeyguardLocked)
                vibrator.vibrate(combinedVibrationEffect)
            startPlayer(mediaQuite);
        }
        else {
            var mediaLoud: MediaPlayer= MediaPlayer.create(context, loudTunes[(generateRandomNumber(0,loudTunes.size.toLong()-1)%loudTunes.size).toInt()]);
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
            if(System.currentTimeMillis()-start> Constants.breakTimeSeconds*1000)
                break;
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager?.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager?.registerListener(this, proxitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager?.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            println(""+accelerometerReadingDone+" "+magnetometerReadingDone+" "+lightSensorReadingDone+" "+proximitySensorReadingDone)
        }
        sensorManager.unregisterListener(this);
    }

    private fun startPlayer(mediaPlayer: MediaPlayer){
        val audioManager = context?.getSystemService(AUDIO_SERVICE) as AudioManager;
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
            accelerometerReading = event.values
            accelerometerReadingDone=true
        }
        if(event?.sensor?.type==Sensor.TYPE_MAGNETIC_FIELD) {
            magnetometerReading = event.values
            magnetometerReadingDone=true
        }
        if(event?.sensor?.type==Sensor.TYPE_PROXIMITY) {
            proximitySensorReading =event.values.get(0);
            proximitySensorReadingDone=true
        }
        if(event?.sensor?.type==Sensor.TYPE_LIGHT) {
            lightSensorReading =event.values.get(0);
            lightSensorReadingDone=true
        }
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrix(rotationMatrix, null,
            accelerometerReading,
            magnetometerReading
        )

        SensorManager.getOrientation(rotationMatrix, orientation)
        Utilities.convertRadianOrientationToDegree(orientation);
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

}