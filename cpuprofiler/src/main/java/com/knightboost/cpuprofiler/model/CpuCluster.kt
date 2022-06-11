package com.knightboost.cpuprofiler.model

import com.knightboost.cpuprofiler.core.data.TimeInState
import com.knightboost.cpuprofiler.core.readutil.CpuPseudoReadUtil
import java.io.File

class CpuCluster(private val policyFile: File) {
    //e.g policy 0

    val affectedCpuList = mutableListOf<Int>()

    var freqList = mutableListOf<Long>()

    val name: String = policyFile.name

    private val timeInStateFile: File by lazy {
        return@lazy File(policyFile, "stats/time_in_state")
    }

    init {
        val lines = File(policyFile.absolutePath + "/scaling_available_frequencies").readLines()
        //实际上只有一行
        for (line in lines) {
            if (line.isEmpty()) continue
            val frequenciesArray = line.split(" ")
            for (frequency in frequenciesArray) {
                if (frequency.isNotEmpty()) {
                    this.freqList.add(frequency.toLong())
                }
            }
        }
        for (lineText in File(policyFile.absolutePath + "/affected_cpus").readLines()) {
            for (cpuIndex in lineText.split(" ").map(String::toInt)) {
                affectedCpuList.add(cpuIndex)
            }
        }

    }

    fun readTimeInState(): TimeInState {
        return CpuPseudoReadUtil.readCpuTimeInState(timeInStateFile, affectedCpuList.size)
    }


}