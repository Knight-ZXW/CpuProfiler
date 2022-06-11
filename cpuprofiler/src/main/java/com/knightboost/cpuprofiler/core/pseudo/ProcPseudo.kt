package com.knightboost.cpuprofiler.core.pseudo

import com.knightboost.cpuprofiler.core.data.ProcStatSummary
import com.knightboost.cpuprofiler.core.data.ProcTimeInState
import com.knightboost.cpuprofiler.core.readutil.CpuPseudoReadUtil
import com.knightboost.cpuprofiler.util.CpuUtils
import java.io.File

class ProcPseudo(path: String)  {

    companion object {
        @JvmStatic
        fun create(path: String): ProcPseudo {
            return ProcPseudo(path);
        }

        @JvmStatic
        fun create():ProcPseudo{
            return create("/proc/" + android.os.Process.myPid() + "/")
        }
        @JvmStatic
        fun createTask(taskId:Long):ProcPseudo{
            return create("/proc/" + android.os.Process.myPid() + "/task/"+taskId)
        }
    }

    private val mPseudoDirFile: File = File(path)

    private val timeInStateFile by lazy {
        File(mPseudoDirFile.absolutePath, "time_in_state")
    }

    private val statFile by lazy {
        File(mPseudoDirFile.absolutePath,"stat")
    }

    fun loadTimeInState(): ProcTimeInState {
        return CpuPseudoReadUtil.loadTimeInstate(timeInStateFile)
    }

    fun totalCpuTime(): Long {
        return CpuPseudoReadUtil.loadTimeInstate(timeInStateFile).totalTime()
    }

    fun alive():Boolean{
        return mPseudoDirFile.exists()
    }

    /**
     *
     * stat 时间定义文件: https://github.com/torvalds/linux/blob/master/include/linux/kernel_stat.h
     *  utime: 用户空间占用的时间
     *  nice: 高nice(低优先级)进程 ，用户空间占用时间
     *  stime: 内核占用时间
     */
    fun loadProcStat():ProcStatSummary{
        val procStatSummary = ProcStatSummary()
        val statInfo = statFile.readText()
        val segemnts = statInfo.split(" ")
        procStatSummary.pid = segemnts[0]
        if (segemnts[1].endsWith(")")){
            procStatSummary.name = segemnts[1].substring(1,segemnts[1].length-1)
        }
        procStatSummary.state = segemnts[2]
        procStatSummary.utime = segemnts[13].toLong()
        procStatSummary.stime = segemnts[14].toLong()
        procStatSummary.nice = segemnts[18]
        procStatSummary.numThreads = segemnts[19].toInt()
        procStatSummary.vsize = segemnts[22].toLong()
        return procStatSummary
    }

    /**
     * Returns the total time (in milliseconds) spent executing in both user and system code.
     */
    fun getCpuTime(): Long {
        val statInfo = statFile.readText()
        val segments = statInfo.split(" ")
        return (segments[13].toLong() + segments[14].toLong())*CpuUtils.clockTicksPerSeconds
    }


}