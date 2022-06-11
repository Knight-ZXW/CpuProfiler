package com.knightboost.cpuprofiler.core.data;
import java.util.LinkedHashMap;

public class ProcCpuTimeInState {
    private int cpuIndex=-1;
    private final LinkedHashMap<Long,Long> frequencyTimes = new LinkedHashMap<>();
    public ProcCpuTimeInState(int cpuIndex){
        this.cpuIndex = cpuIndex;
    }

    public ProcCpuTimeInState(){
    }

    public void setTime(long frequency,long time){
        frequencyTimes.put(frequency,time);
    }

    public long totalTime(){
        long total = 0;
        for (Long value : frequencyTimes.values()) {
            total+=value;
        }
        return total;
    }
}
