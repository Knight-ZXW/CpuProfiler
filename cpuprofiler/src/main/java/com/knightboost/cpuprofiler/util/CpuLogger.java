package com.knightboost.cpuprofiler.util;

import android.util.Log;

public final class CpuLogger {
    private static final String TAG = "cpuProfiler";
    private static boolean loggable = Log.isLoggable("cpuProfiler", Log.DEBUG);

    public static void d(String message) {
        if (loggable){
            Log.d(TAG, message);
        }
    }

    public static void i(String message) {
        Log.i("TAG", message);
    }

    public static void w(String message) {
        Log.w(TAG, message);
    }

    public static void e(String message) {
        Log.e(TAG, message);
    }
    public static void e(String message,Exception e) {
        Log.e(TAG, message,e);
    }
}
