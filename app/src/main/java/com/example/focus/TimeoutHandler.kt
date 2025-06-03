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
    private var accelerometer: Sensor
    private var magnetometer: Sensor
    private var lightSensor: Sensor
    private var combinedVibrationEffect: CombinedVibration
    private var lightSensorReading: Float = 0.0F;
    private var accelerometerReading: FloatArray=FloatArray(3)
    private var magnetometerReading: FloatArray= FloatArray(3)
    private var orientation: FloatArray= FloatArray(3)
    private var accelerometerReadingDone:Boolean=false;
    private var magnetometerReadingDone:Boolean=false;
    private var lightSensorReadingDone:Boolean=false;
    private var loudTunesBatch2=arrayOf(R.raw.the_flute_of_shri_krishna_pomodoro_tune, R.raw.crystal_peak_pomodoro_tune, R.raw.the_weapon_pomodoro_tune, R.raw.your_tale_awaits_you_down_the_road_pomodoro_tune, R.raw.saoirse_notification, R.raw.saoirse_device_disconnect, R.raw.shadow_of_mordor,R.raw.reapers_coast_pomodoro_tune)
    private var loudTunesBatch1=arrayOf(R.raw.a_single_drop_of_magic_pomodoro_tune, R.raw.one_one_seven_pomodoro_tune, R.raw.power_of_innoscence_pomodoro_tune, R.raw.reapers_coast_pomodoro_tune_2, R.raw.reapers_coast_pomodoro_tune_3, R.raw.reapers_coast_pomodoro_tune_4, R.raw.your_tale_awaits_you_down_the_road_pomodoro_tune_2, R.raw.your_tale_awaits_you_down_the_road_pomodoro_tune_3,R.raw.sebilles_theme_pomodoro_tune)
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
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT) as Sensor;
    }
    private fun recordSensorsAndPlay(){
        recordSensors()
        vibrator.cancel()
        if(Utilities.isInsidePocket(
                orientation,
                lightSensorReading
            )|| Utilities.isFacingDown(
                orientation
            )||!keyguardManager.isKeyguardLocked) {
            if(keyguardManager.isKeyguardLocked)
                vibrator.vibrate(combinedVibrationEffect)
            startPlayer(mediaQuite);
        }
        else {
            var mediaLoud: MediaPlayer= MediaPlayer.create(context, loudTunesBatch2[(generateRandomNumber(0,loudTunesBatch1.size.toLong()-1)%loudTunesBatch1.size).toInt()]);
            startPlayer(mediaLoud);
            vibrator.vibrate(combinedVibrationEffect)
        }
    }

    /**
     * play a simple tune if unable to read sensors for long
     */
    public fun safePlay(){
        //Starting music as it forces the device to wake up from sleep
        var media: MediaPlayer= MediaPlayer.create(context, R.raw.silent_pomodoro_tune);
        startPlayer(media);

        val thread=Thread({recordSensorsAndPlay()})
        thread.start()
        val start=System.currentTimeMillis()
        Thread({
            while (true) {
                if (!thread.isAlive)
                    break;
                if (thread.isAlive && (System.currentTimeMillis() - start) > 60000) {
                    thread.interrupt()
                    simplyPlay()
                    break
                }
            }
        }).start()

    }

    private fun simplyPlay(){
        vibrator.cancel()
        var mediaLoud: MediaPlayer= MediaPlayer.create(context, R.raw.saoirse_device_connect);
        startPlayer(mediaLoud);
        vibrator.vibrate(combinedVibrationEffect)
    }

    private fun recordSensors(){
        val start=System.currentTimeMillis()
        accelerometerReadingDone=false;
        magnetometerReadingDone=false;
        lightSensorReadingDone=false;
        while(!accelerometerReadingDone||!magnetometerReadingDone||!lightSensorReadingDone){
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager?.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager?.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            println(""+accelerometerReadingDone+" "+magnetometerReadingDone+" "+lightSensorReadingDone)
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