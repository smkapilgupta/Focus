package com.example.focus

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.focus.common.Constants
import com.example.focus.common.TimerUtilities
import com.example.focus.common.Utilities
import com.example.focus.databinding.FragmentFirstBinding

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private var handler: Handler? = null
    private lateinit var runnable: Runnable
    private lateinit var timeoutHandler:TimeoutHandler
    private val binding get() = _binding!!
    private var startTime:Long=0;

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("onViewCreated called")
        binding.settings.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
        timeoutHandler=TimeoutHandler(this.context)

        binding.start.setOnClickListener {
            if(TimerUtilities.isThreadAlive()){
                TimerUtilities.shutDownThread();
                TimerService.pomodoroTimeSeconds = Constants.pomodoroTimeSeconds
                TimerService.breakTimeSeconds = Constants.breakTimeSeconds
            }
            else{
                startThread()
            }
            updateUx()
        }
        binding.stop.setOnClickListener {
            TimerUtilities.shutDownThread();
            TimerService.pomodoroTimeSeconds = Constants.pomodoroTimeSeconds
            TimerService.timeRemainingMs = TimerService.pomodoroTimeSeconds*1000;
            TimerService.isBreakPeriod = false
            TimerService.breakTimeSeconds = Constants.breakTimeSeconds
            updateUx();
        }
        handler = Handler(Looper.getMainLooper());
        runnable= Runnable { onUxPulse() }
    }

    fun onUxPulse() {
        updateUx();
        handler?.postDelayed(runnable!!,Constants.uiUpdatePeriodMs);
    }

    public fun startThread(){
        startTime=System.currentTimeMillis();
        TimerUtilities.thread = TimerUtilities.startTimer(System.currentTimeMillis()+ TimerService.timeRemainingMs,fun (endTime:Long){onSecond(endTime)},fun (){
            onTimerEnd()
        })
    }

    fun onSecond(endTime:Long){
        TimerService.timeRemainingMs = Math.max(0,endTime-System.currentTimeMillis())
    }

    fun onTimerEnd(){
        startTime=System.currentTimeMillis()
        TimerService.isBreakPeriod = !TimerService.isBreakPeriod;
        if (TimerService.isBreakPeriod) {
            TimerService.timeRemainingMs = TimerService.breakTimeSeconds * 1000
        }
        else
            TimerService.timeRemainingMs = TimerService.pomodoroTimeSeconds *1000
        TimerUtilities.thread =TimerUtilities.startTimer(TimerService.timeRemainingMs +startTime,fun (endTime:Long){onSecond(endTime)},fun (){onTimerEnd()})
        timeoutHandler.recordSensorsAndPlay()
    }

    override fun onResume() {
        println("onResume called")
        super.onResume()
        handler?.post(runnable!!);
        updateUx();
    }

    override fun onPause() {
        println("onPause called")
        super.onPause()
        handler?.removeCallbacks(runnable!!);
    }

    fun updateUx(){
        binding.timer.setText(Utilities.formatTimeFromSeconds(TimerService.timeRemainingMs/1000));
        if(TimerUtilities.isThreadAlive())
            binding.start.setImageResource(R.mipmap.pause_icon)
        else
            binding.start.setImageResource(R.mipmap.play_icon)
        if(TimerService.isBreakPeriod){
            binding.timer.setTextColor(Color.parseColor("#0f6bf5"));
        }else{
            binding.timer.setTextColor(Color.parseColor("#8647C9"));
        }
    }
    override fun onDestroyView() {
        println("onDestroy called")
        super.onDestroyView()
        _binding = null
        handler?.removeCallbacks(runnable!!);
    }
}