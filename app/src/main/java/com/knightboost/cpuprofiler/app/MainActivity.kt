package com.knightboost.cpuprofiler.app

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.knightboost.cpuprofiler.CpuProfiler
import com.knightboost.cpuprofiler.core.*
import com.knightboost.cpuprofiler.core.readutil.CpuPseudoReadUtil
import com.knightboost.cpuprofiler.util.ProcUtil
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btn_block_test).setOnClickListener {
            val mSystemCpuData = LongArray(7)
            ProcessCpuTracker.getCpuTimeForPid(ProcUtil.myProcessId)

            var cpuProfiler = CpuProfiler()
            cpuProfiler.init()

            var loadTimeInstate = TimeInstateFileReader.loadTimeInstate(File("/proc/self/time_in_state"))
            Log.e("CpuProfiler", "cpu totalTime " + loadTimeInstate.totalTime())
            var cpu0 = CpuPseudoReadUtil.readCpuTimeInState(File("/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state"))
            var policy = CpuPseudoReadUtil.readCpuTimeInState(File("/sys/devices/system/cpu/cpufreq/policy0/stats/time_in_state"))
            Log.e("CpuProfiler", "policy time " + policy.spendTime())

        }
        findViewById<View>(R.id.btn_thread_test).setOnClickListener {
            Thread({
                while (true) {
                    var i = 100;
                    i++
                }
            }).start()
        }
    }
}