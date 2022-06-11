package com.knightboost.cpuprofiler.core;

import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import com.knightboost.cpuprofiler.util.CpuUtils;
import com.knightboost.cpuprofiler.util.ProcUtil;

import java.io.File;
import java.util.ArrayList;

import static com.knightboost.cpuprofiler.core.ProcConst.*;

public class ProcessCpuTracker {

    private static final String TAG = "ProcessCpuTracker";
    private static final boolean DEBUG = false;
    private static final boolean localLOGV = DEBUG || false;


    static final int PROCESS_FULL_STAT_MINOR_FAULTS = 1;
    static final int PROCESS_FULL_STAT_MAJOR_FAULTS = 2;
    static final int PROCESS_FULL_STAT_UTIME = 3;
    static final int PROCESS_FULL_STAT_STIME = 4;
    static final int PROCESS_FULL_STAT_VSIZE = 5;

    private final String[] mProcessFullStatsStringData = new String[6];
    private final long[] mProcessFullStatsData = new long[6];

    private static final int[] SYSTEM_CPU_FORMAT = new int[] {
            PROC_SPACE_TERM|PROC_COMBINE,
            PROC_SPACE_TERM|PROC_OUT_LONG,                  // 1: user time
            PROC_SPACE_TERM|PROC_OUT_LONG,                  // 2: nice time
            PROC_SPACE_TERM|PROC_OUT_LONG,                  // 3: sys time
            PROC_SPACE_TERM|PROC_OUT_LONG,                  // 4: idle time
            PROC_SPACE_TERM|PROC_OUT_LONG,                  // 5: iowait time
            PROC_SPACE_TERM|PROC_OUT_LONG,                  // 6: irq time
            PROC_SPACE_TERM|PROC_OUT_LONG                   // 7: softirq time
    };


    private static final int[] PROCESS_STATS_FORMAT = new int[] {
            PROC_SPACE_TERM,
            PROC_SPACE_TERM|PROC_PARENS,
            PROC_SPACE_TERM,
            PROC_SPACE_TERM,
            PROC_SPACE_TERM,
            PROC_SPACE_TERM,
            PROC_SPACE_TERM,
            PROC_SPACE_TERM,
            PROC_SPACE_TERM,
            PROC_SPACE_TERM|PROC_OUT_LONG,                  // 10: minor faults
            PROC_SPACE_TERM,
            PROC_SPACE_TERM|PROC_OUT_LONG,                  // 12: major faults
            PROC_SPACE_TERM,
            PROC_SPACE_TERM|PROC_OUT_LONG,                  // 14: utime
            PROC_SPACE_TERM|PROC_OUT_LONG,                  // 15: stime
    };


    private final long[] mSystemCpuData = new long[7];

    private static final int[] LOAD_AVERAGE_FORMAT = new int[] {
            PROC_SPACE_TERM|PROC_OUT_FLOAT,                 // 0: 1 min
            PROC_SPACE_TERM|PROC_OUT_FLOAT,                 // 1: 5 mins
            PROC_SPACE_TERM|PROC_OUT_FLOAT                  // 2: 15 mins
    };

    private final float[] mLoadAverageData = new float[3];

    private final boolean mIncludeThreads;

    // How long a CPU jiffy is in milliseconds.
    private final long mJiffyMillis;

    private float mLoad1 = 0;
    private float mLoad5 = 0;
    private float mLoad15 = 0;

    // All times are in milliseconds. They are converted from jiffies to milliseconds
    // when extracted from the kernel.
    private long mCurrentSampleTime;
    private long mLastSampleTime;

    private long mCurrentSampleRealTime;
    private long mLastSampleRealTime;

    private long mCurrentSampleWallTime;
    private long mLastSampleWallTime;

    private long mBaseUserTime;
    private long mBaseSystemTime;
    private long mBaseIoWaitTime;
    private long mBaseIrqTime;
    private long mBaseSoftIrqTime;
    private long mBaseIdleTime;
    private int mRelUserTime;
    private int mRelSystemTime;
    private int mRelIoWaitTime;
    private int mRelIrqTime;
    private int mRelSoftIrqTime;
    private int mRelIdleTime;
    private boolean mRelStatsAreGood;

    private int[] mCurPids;
    private int[] mCurThreadPids;

    private final ArrayList<Stats> mProcStats = new ArrayList<Stats>();
    private final ArrayList<Stats> mWorkingProcs = new ArrayList<Stats>();
    private boolean mWorkingProcsSorted;

    private boolean mFirst = true;

    public interface FilterStats {
        /** Which stats to pick when filtering */
        boolean needed(Stats stats);
    }

    public static class Stats {
        public final int pid;
        public final int uid;
        final String statFile;
        final String cmdlineFile;
        final String threadsDir;
        final ArrayList<Stats> threadStats;
        final ArrayList<Stats> workingThreads;

        // public BatteryStatsImpl.Uid.Proc batteryStats;

        public boolean interesting;

        public String baseName;

        public String name;
        public int nameWidth;

        // vsize capture when process first detected; can be used to
        // filter out kernel processes.
        public long vsize;

        /**
         * Time in milliseconds.
         */
        public long base_uptime;

        /**
         * Time in milliseconds.
         */
        // @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public long rel_uptime;

        /**
         * Time in milliseconds.
         */
        public long base_utime;

        /**
         * Time in milliseconds.
         */
        public long base_stime;

        /**
         * Time in milliseconds.
         */
        public int rel_utime;

        /**
         * Time in milliseconds.
         */
        // @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public int rel_stime;

        public long base_minfaults;
        public long base_majfaults;
        public int rel_minfaults;
        public int rel_majfaults;

        public boolean active;
        public boolean working;
        public boolean added;
        public boolean removed;

        Stats(int _pid, int parentPid, boolean includeThreads) {
            pid = _pid;
            if (parentPid < 0) {
                final File procDir = new File("/proc", Integer.toString(pid));
                uid = getUid(procDir.toString());
                statFile = new File(procDir, "stat").toString();
                cmdlineFile = new File(procDir, "cmdline").toString();
                threadsDir = (new File(procDir, "task")).toString();
                if (includeThreads) {
                    threadStats = new ArrayList<Stats>();
                    workingThreads = new ArrayList<Stats>();
                } else {
                    threadStats = null;
                    workingThreads = null;
                }
            } else {
                final File procDir = new File("/proc", Integer.toString(
                        parentPid));
                final File taskDir = new File(
                        new File(procDir, "task"), Integer.toString(pid));
                uid = getUid(taskDir.toString());
                statFile = new File(taskDir, "stat").toString();
                cmdlineFile = null;
                threadsDir = null;
                threadStats = null;
                workingThreads = null;
            }
        }

        private static int getUid(String path) {
            try {
                return Os.stat(path).st_uid;
            } catch (ErrnoException e) {
                Log.w(TAG, "Failed to stat(" + path + "): " + e);
                return -1;
            }
        }
    }


    public ProcessCpuTracker(boolean includeThreads){
        mIncludeThreads = includeThreads;
        long jiffyHz = CpuUtils.INSTANCE.getMillSecondsPerTicks();
        mJiffyMillis = 1000/jiffyHz;
    }

    public void init(){
        mFirst = true;
        update();
    }

    public void update(){
        final long nowUptime = SystemClock.uptimeMillis();
        final long nowRealtime = SystemClock.elapsedRealtime();
        final long nowWallTime = System.currentTimeMillis();
        final long[] sysCpu = mSystemCpuData;

    }


    //Stores user time and system time in jiffies.
    // Used for public API to retrieve CPU use for a process. Must lock while in use.
    private static final long[] mSinglePidStatsData = new long[4];
    static final int PROCESS_STAT_MINOR_FAULTS = 0;
    static final int PROCESS_STAT_MAJOR_FAULTS = 1;
    static final int PROCESS_STAT_UTIME = 2;
    static final int PROCESS_STAT_STIME = 3;

    public static long getCpuTimeForPid(int pid){
        synchronized (mSinglePidStatsData) {
            final String statFile = "/proc/" + pid + "/stat";
            final long[] statsData = mSinglePidStatsData;
            if (ProcUtil.readProcFile(statFile, PROCESS_STATS_FORMAT,
                    null, statsData, null)) {
                long time = statsData[PROCESS_STAT_UTIME]
                        + statsData[PROCESS_STAT_STIME];
                // return time * mJiffyMillis;
                Log.e("zxw","time is"+time);
                return time;
            }
            return 0;
        }
    }



}
