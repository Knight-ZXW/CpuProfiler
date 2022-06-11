package com.knightboost.cpuprofiler.core

import android.os.StrictMode
import android.system.Os
import android.system.OsConstants
import android.text.TextUtils
import android.util.Log
import java.io.*

class CpuSpeedReader {
    private val mProcFile:File;
    private  var mJiffyMillis:Long
    private var mNumSpeedSteps = 0
    private val mLastSpeedTimesMs: LongArray
    private val mDeltaSpeedTimesMs: LongArray
    public constructor(timeInStateFile: File,numSpeedSteps :Int){
        mProcFile = timeInStateFile;
        mNumSpeedSteps = numSpeedSteps
        mLastSpeedTimesMs = LongArray(numSpeedSteps)
        mDeltaSpeedTimesMs = LongArray(numSpeedSteps)
        val jiffyHz = Os.sysconf(OsConstants._SC_CLK_TCK)
        mJiffyMillis = 1000 / jiffyHz
    }

    /**
     * Returns:
    The time (in milliseconds) spent at different cpu speeds.
    The values should be monotonically increasing, unless the cpu was hotplugged.
     */
    fun  readAbsolute(){
        var policy = StrictMode.allowThreadDiskReads()
        val speedTimeMs = LongArray(mNumSpeedSteps)
        val reader = BufferedReader(FileReader(mProcFile))
        reader.use {
            try {
                val splitter = TextUtils.SimpleStringSplitter(' ')
                var line = ""
                var speedIndex =0;
                while (speedIndex<mNumSpeedSteps &&reader.readLine().also { line = it } != null){
                    splitter.setString(line)
                    splitter.next()
                    val time= splitter.next().toLong() *mJiffyMillis
                    speedTimeMs[speedIndex] = time
                    speedIndex++
                }
            }catch (e:IOException){
                Log.e("CpuSpeedReader","Failed to read cpu-freq:"+e.message)
            }finally {
                StrictMode.setThreadPolicy(policy)
            }
        }

    }

}