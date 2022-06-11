package com.knightboost.cpuprofiler.core

import android.os.Process
import com.knightboost.cpuprofiler.util.ProcUtil
import com.knightboost.cpuprofiler.util.ProcUtil.readProcFile
import java.io.File
import java.io.IOException
import java.nio.file.Path

/**
 * Reads and parses time_in_state files in the proc filesystem.
 * Every line in a time_in_state file contains two numbers, separated by a single space character.
 * The first number is the frequency of the CPU used in kilohertz. The second number is the time spent in this frequency.
 * In the time_in_state file, this is given in 10s of milliseconds, but this class returns in milliseconds.
 * This can be per user, process, or thread depending on which time_in_state file is used.
 * For example, a time_in_state file would look like this:
 * 300000 3
 * 364800 0
 * ...
 * 1824000 0
 * 1900800 1
 *
 * This file would indicate that the CPU has spent 30 milliseconds at frequency 300,000KHz (300Mhz)
 * and 10 milliseconds at frequency 1,900,800KHz (1.9GHz).
 * This class will also read time_in_state files with headers, such as:
 * cpu0
 * 300000 3
 * 364800 0
 * ...
 * cpu4
 * 300000 1
 * 364800 4
 */
class ProcTimeInstateReader {
    /**
     * The format of the `time_in_state` file to extract times, defined using [ ]'s `PROC_OUT_LONG` and related variables
     */
    private lateinit var mTimeInStateTimeFormat: IntArray
    /**
     * The frequencies reported in each `time_in_state` file
     *
     * Defined on first successful read of `time_in_state` file.
     */
    private lateinit var mFrequenciesKhz: LongArray
    /**
     * @param initialTimeInStateFile the file to base the format of the frequency files on, and to
     * read frequencies from. Expected to be in the same format as all other `time_in_state`
     * files, and contain the same frequencies.
     * @throws IOException if reading the initial `time_in_state` file failed
     */
    @Throws(IOException::class) constructor (initialTimeInStateFile: String) {
        initializeTimeInStateFormat(initialTimeInStateFile)
    }
    /**
     * Set the [.mTimeInStateTimeFormat] and [.mFrequenciesKhz] variables based on the
     * an input file. If the file is empty, these variables aren't set
     *
     * This needs to be run once on the first invocation of [.getUsageTimesMillis]. This
     * is because we need to know how many frequencies are available in order to parse time
     * `time_in_state` file using [Process.readProcFile], which only accepts
     * fixed-length formats. Also, as the frequencies do not change between `time_in_state`
     * files, we read and store them here.
     *
     * @param timeInStatePath the input file to base the format off of
     */
    @Throws(IOException::class) private fun initializeTimeInStateFormat(timeInStatePath: String) {
        // Read the bytes of the `time_in_state` file
        val timeInStateBytes = File(timeInStatePath).readBytes()

        // Iterate over the lines of the time_in_state file, for each one adding a line to the
        // formats. These formats are used to extract either the frequencies or the times from a
        // time_in_state file
        // Also check if each line is a header, and handle this in the created format arrays
        val timeInStateFrequencyFormat = mutableListOf<Int>()
        val timeInStateTimeFormat = mutableListOf<Int>()
        var numFrequencies = 0
        var i = 0
        while (i < timeInStateBytes.size) {

            // If the first character of the line is not a digit, we treat it as a header
            if (!Character.isDigit(timeInStateBytes[i].toInt())) {
                timeInStateFrequencyFormat.addAll(TIME_IN_STATE_HEADER_LINE_FORMAT.toList())
                timeInStateTimeFormat.addAll(TIME_IN_STATE_HEADER_LINE_FORMAT.toList())
            } else {
                timeInStateFrequencyFormat.addAll(TIME_IN_STATE_LINE_FREQUENCY_FORMAT.toList())
                timeInStateTimeFormat.addAll(TIME_IN_STATE_LINE_TIME_FORMAT.toList())
                numFrequencies++
            }
            // Go to the next line
            while (i < timeInStateBytes.size && timeInStateBytes[i].toChar() != '\n') {
                i++
            }
            i++
        }
        if (numFrequencies == 0) {
            throw IOException("Empty time_in_state file")
        }

        // Read the frequencies from the `time_in_state` file and store them, as they will be the
        // same for every `time_in_state` file
        val readLongs = LongArray(numFrequencies)
        val readSuccess: Boolean = ProcUtil.parseProcLine(
            timeInStateBytes, 0, timeInStateBytes.size,
            timeInStateFrequencyFormat.toIntArray(), null, readLongs, null
        )
        if (!readSuccess) {
            throw IOException("Failed to parse time_in_state file")
        }
        mTimeInStateTimeFormat = timeInStateTimeFormat.toIntArray()
        mFrequenciesKhz = readLongs
    }

    /**
     * Read the CPU usages from a file
     *
     * @param timeInStatePath path where the CPU usages are read from
     * @return list of CPU usage times from the file. These correspond to the CPU frequencies given
     * by [ProcTimeInStateReader.getFrequenciesKhz]
     */
    fun getUsageTimesMillis(timeInStatePath: String): LongArray {
        // Read in the time_in_state file
        val readLongs = LongArray(mFrequenciesKhz.size)
        val readSuccess: Boolean = ProcUtil.readProcFile(
            timeInStatePath,
            mTimeInStateTimeFormat,
            null, readLongs, null
        )
        if (!readSuccess) {
            return LongArray(0)
        }
        // Usage time is given in 10ms, so convert to ms
        for (i in readLongs.indices) {
            readLongs[i] = readLongs[i]*10
        }
        return readLongs
    }



    companion object {
        /**
         * The format of a single line of the `time_in_state` file that exports the frequency
         * values
         */
        private val TIME_IN_STATE_LINE_FREQUENCY_FORMAT = intArrayOf(
            ProcConst.PROC_OUT_LONG or ProcConst.PROC_SPACE_TERM,
            ProcConst.PROC_NEWLINE_TERM
        )
        /**
         * The format of a single line of the time_in_state file that exports the time values
         */
        private val TIME_IN_STATE_LINE_TIME_FORMAT = intArrayOf(
            ProcConst.PROC_SPACE_TERM,
            ProcConst.PROC_OUT_LONG or ProcConst.PROC_NEWLINE_TERM
        )
        /**
         * The format of a header line of the `time_in_state` file
         */
        private val TIME_IN_STATE_HEADER_LINE_FORMAT = intArrayOf(
            ProcConst.PROC_NEWLINE_TERM
        )
    }
}