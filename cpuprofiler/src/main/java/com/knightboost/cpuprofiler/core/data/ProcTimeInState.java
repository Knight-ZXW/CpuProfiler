package com.knightboost.cpuprofiler.core.data;

import java.util.ArrayList;
import java.util.List;

public class ProcTimeInState {

    public static ProcTimeInState EMPTY =new ProcTimeInState();
    private final List<ProcCpuTimeInState> cpus = new ArrayList<>();

    public ProcTimeInState(){
    }


    public void addCpuTimeInState(ProcCpuTimeInState procCpuTimeInState){
        cpus.add(procCpuTimeInState);
    }

    public long totalTime(){
        long total = 0;
        for (ProcCpuTimeInState procCpuTimeInState : cpus) {
            total+= procCpuTimeInState.totalTime();
        }
        return total;
    }
}
