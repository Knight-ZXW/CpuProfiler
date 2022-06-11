package com.knightboost.cpuprofiler.core;

import com.knightboost.cpuprofiler.core.data.ProcCpuTimeInState;
import com.knightboost.cpuprofiler.core.data.ProcTimeInState;
import com.knightboost.cpuprofiler.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class TimeInstateFileReader {

    private File file;

    public TimeInstateFileReader(String path) {
        this.file = new File(path);
    }

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

        }
        return procTimeInState;
    }

}
