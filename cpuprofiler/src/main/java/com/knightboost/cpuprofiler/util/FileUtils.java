package com.knightboost.cpuprofiler.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class FileUtils {


    public static BufferedReader openBuffer(File file) {
        if (file == null)
            return null;
        if (!file.exists()) {
            CpuLogger.w("file:" + file.getName() + " not exist");
            return null;
        }
        if (!file.canRead()) {
            CpuLogger.w("file:" + file.getName() + " can not read");
            return null;
        }
        BufferedReader bufferedReader = null;
        try {
            FileReader fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader, 10000);
        } catch (Exception exception) {
            CpuLogger.e(Log.getStackTraceString(null));
        }
        return bufferedReader;
    }

    /**
     * safety close bufferedReader
     * @param bufferReader
     */
    public static void close(BufferedReader bufferReader) {
        if (bufferReader != null)
            try {
                bufferReader.close();
                return;
            } catch (Exception exception) {
                CpuLogger.d(Log.getStackTraceString(null));
            }
    }
}
