package com.knightboost.cpuprofiler

import android.util.Log
import com.knightboost.cpuprofiler.core.pseudo.CpuSystem
import com.knightboost.cpuprofiler.core.pseudo.ProcPseudo
import com.knightboost.cpuprofiler.model.CpuCluster
import com.knightboost.cpuprofiler.util.*
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 *
 * cpuProfiler 配置:
 *
 * linux sys 相关文档:
 *  1.idle https://www.kernel.org/doc/Documentation/cpuidle/sysfs.txt
 *  2.
 */
class CpuProfiler {

    //sys resource parame
    private lateinit var cpuClusters: List<CpuCluster>
    private val cpuClusterTimeInStatFile: List<File> = ArrayList()
    private val procPseudoFile = ProcPseudo.create()

    // immutable default config
    //200ms 更新一次
    private val mJiffyMillis: Long = CpuUtils.millSecondsPerTicks

    //profiler config

    //  thread cpu usage Config
    private val sampleThreadCpuUsage = true;
    private val onlyMainTaskCpuUsage = true;

    private val sampleInterval = 1000

    //profiler state data
    var lastCpuTime = 0L;
    var lastProcCpuTime = 0L;
    var lastIdleTime = 0L;
    var lastSampleWallTime = 0L;

    fun init() {
        cpuClusters = CpuUtils.cpuClusters
        val executorService = Executors.newScheduledThreadPool(1)
        executorService.scheduleAtFixedRate({
            try {
                sample()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, 0, sampleInterval.toLong(), TimeUnit.MILLISECONDS)
    }

    private fun sample() {
        var cpuTime = readTotalTime()
        var idleTime = readSysIdleTime() / 1000
        val wallTime = System.currentTimeMillis()

        var procStat = procPseudoFile.loadProcStat()
        var procCpuTime = procPseudoFile.totalCpuTime()
        procCpuTime = procStat.totalCpuMillSeconds

        Log.e("cpuProfiler", "cpuTime " + cpuTime + " idleTime " + idleTime + " procCpuTIime " + procCpuTime)
        var deltaWallTime = 0L
        var deltaSystemCpuTime = 0L //正常情况下 CPU核数 * 经过的wallTime
        if (lastSampleWallTime != 0L) {
            deltaWallTime = wallTime - lastSampleWallTime
        }

        if (deltaWallTime > 0) {
            //真个系统CPU的总时间
            deltaSystemCpuTime = cpuTime - lastCpuTime

            val deltaIdleTime = idleTime - lastIdleTime
            val deltaProcCpuTime = procCpuTime - lastProcCpuTime
            Log.e("cpuProfiler", "cpuDeltaTime " + deltaSystemCpuTime + " idleDeltaTime " + deltaIdleTime)
            // CPU 使用率
            Log.e("cpuProfiler", "系统CPU使用率 = " + (1 - deltaIdleTime.toFloat() / deltaSystemCpuTime) * 100 + "%")
            Log.e("cpuProfiler", "cpu = " + deltaSystemCpuTime / deltaWallTime.toFloat() * 100 + "%")
            Log.e("cpuProfiler", "进程 cpu使用率 = " + (100f * deltaProcCpuTime / deltaSystemCpuTime) + "%")
        }
        //线程CPU 使用率计算
        if (sampleThreadCpuUsage) {
            sampleThreadCpuUsage(deltaWallTime, deltaSystemCpuTime)
        }

        //like AS cpu Proifler
        //APP cpu 使用率、others使用、

        lastCpuTime = cpuTime
        lastProcCpuTime = procCpuTime
        lastIdleTime = idleTime
        lastSampleWallTime = wallTime;
        //thread cpu calculate
    }

    val taskProcPseudos = mutableMapOf<Long, ProcPseudo>()
    val taskCpuTimeRecord = mutableMapOf<Long, Long>()

    private class TaskStat {

    }

    //打印该函数耗时
    private fun sampleThreadCpuUsage(deltaWallTime: Long, deltaSystemCpuTime: Long) {
        var myProcTaskIds = ProcUtil.getMyProcTaskIds()
        for (taskId in myProcTaskIds) {
            var taskProcPseudo = taskProcPseudos[taskId]
            if (taskProcPseudo == null) {
                taskProcPseudo = ProcPseudo.createTask(taskId)
            }

            val cpuUsedMillSeconds = taskProcPseudo.getCpuTime()

            val lastTaskCpuTime = taskCpuTimeRecord[taskId]?:0
            taskCpuTimeRecord[taskId] = cpuUsedMillSeconds

            if (deltaWallTime>0 && lastTaskCpuTime>0){

                val deltaCpuUseMs = cpuUsedMillSeconds - lastTaskCpuTime
                val cpuUsagePercent = deltaCpuUseMs * 100f / deltaSystemCpuTime;
                val threadCpuUsageRate = deltaCpuUseMs * 100f / deltaWallTime;
                //1. 该线程的CPU 时间占整个系统CPU时间的比例 ->cpu usage
                //2. 该线程的CPU 时间 / 经过的系统时钟时间 ->  cpu Rate

                CpuLogger.e(
                    "thread id " + taskId + "cpu时间 "+ deltaCpuUseMs+" cpu 使用率 "
                            + cpuUsagePercent
                            + " 线程cpu运行比例" + threadCpuUsageRate
                )
            }





        }
    }

    private fun readTotalTime(): Long {
        var time: Long = 0
        for (cpuCluster in cpuClusters) {
            val timeInState = cpuCluster.readTimeInState()
            time += timeInState.spendTime()
        }
        return time
    }

    /**
     * 时间单位 microseconds
     */
    private fun readSysIdleTime(): Long {
        return CpuSystem.idleTime()
    }

}