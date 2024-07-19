package com.example.focus

import android.content.Intent
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
import com.example.focus.common.Utilities
import com.example.focus.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private var handler: Handler? = null
    private lateinit var runnable: Runnable

    private val binding get() = _binding!!

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

        binding.start.setOnClickListener {
            if(TimerService.isLooperThreadAlive()){
                TimerService.shutDownLooperThread();
                TimerService.pomodoroTimeSeconds = Constants.pomodoroTimeSeconds
                TimerService.breakTimeSeconds = Constants.breakTimeSeconds
                if(TimerService.serviceActive) {
                    this.activity?.stopService(Intent(this.activity, TimerService::class.java));
                    updateUx(false)
                }
            }
            else{
                if(!TimerService.serviceActive) {
                    this.activity?.startForegroundService(
                        Intent(
                            this.activity,
                            TimerService::class.java
                        )
                    );
                    updateUx(true)
                }
            }
        }
        binding.stop.setOnClickListener {
            TimerService.shutDownLooperThread();
            TimerService.pomodoroTimeSeconds = Constants.pomodoroTimeSeconds
            TimerService.timeRemainingMs = TimerService.pomodoroTimeSeconds*1000;
            TimerService.isBreakPeriod = false
            TimerService.breakTimeSeconds = Constants.breakTimeSeconds
            this.activity?.stopService(Intent(this.activity, TimerService::class.java));
            updateUx(false);
        }
        handler = Handler(Looper.getMainLooper());
        runnable= Runnable { onPulse() }
    }

    fun onPulse() {
        updateUx(TimerService.isLooperThreadAlive());
        handler?.postDelayed(runnable!!,Constants.uiUpdatePeriodMs);
    }

    override fun onResume() {
        println("onResume called")
        super.onResume()
        handler?.post(runnable!!);
        updateUx(TimerService.isLooperThreadAlive());
    }

    override fun onPause() {
        println("onPause called")
        super.onPause()
        handler?.removeCallbacks(runnable!!);
    }

    fun updateUx(running: Boolean){
        binding.timer.setText(Utilities.formatTimeFromSeconds(TimerService.timeRemainingMs/1000));
        if(TimerService.isLooperThreadAlive() || running)
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
    }
}