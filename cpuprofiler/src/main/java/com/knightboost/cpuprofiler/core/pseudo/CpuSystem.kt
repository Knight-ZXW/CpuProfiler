package com.knightboost.cpuprofiler.core.pseudo

import com.knightboost.cpuprofiler.core.CpuPseudo
import com.knightboost.cpuprofiler.util.CpuUtils

object CpuSystem {

    private val cpuPesudos = mutableListOf<CpuPseudo>()

    init {
        val cpuFiles = CpuUtils.cpuFiles
        for (cpuFile in cpuFiles) {
            val cpuIndex = cpuFile.name.replace("cpu", "").toInt()
            val cpuPesudo = CpuPseudo(cpuIndex)
            cpuPesudos.add(cpuPesudo)
        }
    }

    fun allCpu(): List<CpuPseudo> {
        return cpuPesudos
    }

    fun sampleIdleTime(sampleIntervalMill:Int):Long{
        return  0L;
    }

    fun idleTime(): Long {
        var total = 0L

        //idle time 智能调整
        // 当 idle时间未发生变化，且当前cpu执行在低频率上，则认为是full idle
        for (cpuPesudo in allCpu()) {
            total += cpuPesudo.idleTime()
        }
        return total
    }

    fun getCpu(index: Int): CpuPseudo {
        return cpuPesudos[index]
    }

}