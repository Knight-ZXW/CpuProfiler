package com.knightboost.cpuprofiler.core

import android.util.Log
import com.knightboost.cpuprofiler.core.data.TimeInState
import com.knightboost.cpuprofiler.core.pseudo.Pseudo
import com.knightboost.cpuprofiler.core.readutil.CpuPseudoReadUtil
import com.knightboost.cpuprofiler.util.CpuUtils.readLong
import java.io.File
import java.util.regex.Pattern

class CpuPseudo(val cpuIndex: Int) : Pseudo {
    val basePath = "/sys/devices/system/cpu/cpu${cpuIndex}/"

    val cpuIdleStates by lazy {
        val idleStates = mutableListOf<CpuIdleState>()
        val file = File("${basePath}/cpuidle")
        val stateFiles = file.listFiles { _, name -> Pattern.matches("state[0-9]", name) }

        for (cpuIdleFile in stateFiles) {
            val state = cpuIdleFile.name.replace("state", "").toInt()
            val cpuIdle = CpuIdleState(cpuIndex, state)
            idleStates.add(cpuIdle)
        }
        return@lazy idleStates
    }

    val timeInStateFile by lazy{
        return@lazy File(basePath+"cpufreq/stats/time_in_state")
    }

    var lastState0Time =0L;
    var lastState1Time =0L;
    fun idleTime():Long{
        var total = 0L;
        for (cpuIdleState in cpuIdleStates) {
            val time = cpuIdleState.time()
            if (cpuIdleState.state==0){
                if (time ==lastState0Time){
                    Log.e("cpuProfiler","cpu ${cpuIndex} -> state: ${cpuIdleState.state} 时间${time} 未变化")
                }

                lastState0Time =time
            }else if (cpuIdleState.state==1){
                if (time ==lastState1Time){
                    Log.e("cpuProfiler","cpu ${cpuIndex} -> state: ${cpuIdleState.state} 时间${time} 未变化")
                }
                lastState1Time =time
            }
            total+=time
        }
        return total
    }

    fun timeInState():TimeInState{
        return CpuPseudoReadUtil.readCpuTimeInState(timeInStateFile)
    }

}

class CpuIdleState(val cpuIndex: Int, val state: Int) : Pseudo {
    val path = "/sys/devices/system/cpu/cpu${cpuIndex}/cpuidle/state${state}"


    val name by lazy {
        return@lazy File(path, "name").readText()
    }

    val timeFile by lazy{
        return@lazy File(path, "time")
    }
    val usageFile by lazy{
        return@lazy File(path, "usage")
    }

    fun time(): Long {
        return timeFile.readLong()
    }

    fun usage(): Long {
        return usageFile.readLong()
    }
}

