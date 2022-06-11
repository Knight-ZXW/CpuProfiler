package com.knightboost.cpuprofiler.core.data

import com.knightboost.cpuprofiler.util.CpuUtils

/**
 * todo: support object pool cache
 */
class ProcStatSummary {
    var pid: String = ""
    var name: String = ""
    var state: String = ""
    var utime: Long = 0
    var stime: Long = 0
    var nice = ""
    var numThreads = 0
    var starttime: String = ""
    var vsize: Long = 0

    val totalCpuTime:Long by lazy {
        return@lazy utime+stime
    }

    val totalCpuMillSeconds:Long by lazy{
        return@lazy totalCpuTime* CpuUtils.millSecondsPerTicks
    }


}