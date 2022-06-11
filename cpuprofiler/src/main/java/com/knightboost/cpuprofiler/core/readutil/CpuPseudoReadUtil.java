package com.knightboost.cpuprofiler.core.readutil;


import androidx.annotation.NonNull;

import com.knightboost.cpuprofiler.core.data.ProcCpuTimeInState;
import com.knightboost.cpuprofiler.core.data.ProcTimeInState;
import com.knightboost.cpuprofiler.core.data.TimeInState;
import com.knightboost.cpuprofiler.util.CpuLogger;
import com.knightboost.cpuprofiler.util.FileUtils;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CpuPseudoReadUtil {

    public static TimeInState readCpuTimeInState(@NonNull File cpuTimeInStateFile) {
        return readCpuTimeInState(cpuTimeInStateFile, 1);
    }

    @NotNull
    public static TimeInState readCpuTimeInState(@NonNull File cpuTimeInStateFile, int cpuCount) {
        TimeInState timeInState = new TimeInState();
        if (!cpuTimeInStateFile.exists()) {
            return TimeInState.EMPTY;
        }
        BufferedReader bufferedReader;
        if ((bufferedReader = FileUtils.openBuffer(cpuTimeInStateFile)) == null)
            return TimeInState.EMPTY;
        try {
            String line;
            while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
                String[] timeInStatePair = line.split("\\s+");
                if (timeInStatePair.length < 2) {// unknown state
                    continue;
                }
                Long frequency = Long.parseLong(timeInStatePair[0]);
                Long time = Long.parseLong(timeInStatePair[1]);
                timeInState.setTime(frequency, time  * cpuCount);
            }
            return timeInState;
        } catch (Exception exception) {
            CpuLogger.e("readCpuTimeInState failed", exception);
            return TimeInState.EMPTY;
        } finally {
            FileUtils.close(bufferedReader);
        }
    }

    /**
     * doc:
     *  TODO:测试
     *   如果对应的是task，则有可能读取的时候线程已经退出了，有可能会读取失败
     * @param file
     * @return
     */
    public static ProcTimeInState loadTimeInstate(File file) {
        BufferedReader bufferedReader = null;
        if ((bufferedReader = FileUtils.openBuffer(file)) == null) {
            return new ProcTimeInState();
        }

        String line;
        ArrayList<ProcCpuTimeInState> cpusTimeInStates = new ArrayList<>();

        ProcTimeInState procTimeInState = new ProcTimeInState();
        ProcCpuTimeInState procCpuTimeInState = null;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("cpu")) {
                    int cpuIndex = Integer.parseInt(line.replace("cpu", ""));
                    procCpuTimeInState = new ProcCpuTimeInState(cpuIndex);
                    cpusTimeInStates.add(procCpuTimeInState);
                    procTimeInState.addCpuTimeInState(procCpuTimeInState);
                } else {
                    String[] pair;
                    if ((pair = line.split("\\s+")).length > 1) {
                        long frequency = Long.parseLong(pair[0]);
                        long time = 10L * Long.parseLong(pair[1]);
                        procCpuTimeInState.setTime(frequency, time);
                    }
                }

            }
        } catch (IOException e) {
            //todo 异常处理
            e.printStackTrace();
            return ProcTimeInState.EMPTY;
        } catch (Exception e){
            e.printStackTrace();
            return ProcTimeInState.EMPTY;
        }
        return procTimeInState;
    }

}
