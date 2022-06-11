package com.knightboost.cpuprofiler.util

import com.knightboost.cpuprofiler.core.ProcConst.*
import java.io.*

object ProcUtil {

    val myProcessId by lazy{
        return@lazy android.os.Process.myPid()
    }

    fun getMyProcTaskIds(): List<Long> {
        val taskIds = ArrayList<Long>()
        val file = File("/proc/self/task/")
        val filenameFilter = FilenameFilter { _, filename ->
            try {
                filename.toLong()
                return@FilenameFilter true
            } catch (numberFormatException: NumberFormatException) {
                return@FilenameFilter false
            }
        }
        val files = file.listFiles(filenameFilter) ?: return ArrayList(0)
        for (f in files) {
            taskIds.add(f.name.toLong())
        }
        return taskIds
    }



    @JvmStatic
     fun readProcFile(
        file: String, formatArray: IntArray,
        outStrings: Array<String?>?, outLongs: LongArray?, outFloats: FloatArray?
    ):Boolean{
        val bufferedReader = File(file).bufferedReader()
        try {
            var strIndex =0;
            var longIndex =0;
            var floatIndex =0;

            val byteBuffer = mutableListOf<Byte>()

            for (format in formatArray) {
                var procParens = false
                if (format and PROC_PARENS == PROC_PARENS){
                    procParens = true
                }

                //read until reach term
                var term = PROC_SPACE_TERM;
                if (format and PROC_SPACE_TERM == PROC_SPACE_TERM){
                    term = PROC_SPACE_TERM
                } else if (format and PROC_TAB_TERM == PROC_TAB_TERM){
                    term = PROC_TAB_TERM
                } else if (format and PROC_NEWLINE_TERM == PROC_NEWLINE_TERM){
                    term = PROC_NEWLINE_TERM
                }

                do {
                    val code = bufferedReader.read()
                    if (code!=term && code!=-1){
                        if (procParens && code!='('.toInt() && code!=')'.toInt()){
                            byteBuffer.add(code.toByte())
                        }else{
                            byteBuffer.add(code.toByte())
                        }
                    }
                }while (code!=term && code!=-1)

                //read value

                if (format and PROC_OUT_STRING ==  PROC_OUT_STRING){
                    outStrings?.set(strIndex, String(byteBuffer.toByteArray()))
                    strIndex++
                }else if (format and PROC_OUT_FLOAT == PROC_OUT_FLOAT){
                    outFloats?.set(floatIndex, String(byteBuffer.toByteArray()).toFloat())
                    floatIndex++
                } else if (format and PROC_OUT_LONG == PROC_OUT_LONG){
                    outLongs?.set(longIndex, String(byteBuffer.toByteArray()).toLong())
                    longIndex++
                }
                byteBuffer.clear()
            }
            bufferedReader.close()
        }finally {
            bufferedReader.close()
        }


        return true
     }

    @JvmStatic
    fun parseProcLine(inputBytes: ByteArray,begin:Int, end:Int,formatArray: IntArray,
        outStrings: Array<String?>?, outLongs: LongArray?, outFloats: FloatArray?):Boolean{
        var i = begin

        val byteBuffer = mutableListOf<Byte>()
        var strIndex =0;
        var longIndex =0;
        var floatIndex =0;

        for (format in formatArray) {
            var procParens = false
            if (format and PROC_PARENS == PROC_PARENS){
                procParens = true
            }

            //read until reach term
            var term = PROC_SPACE_TERM;
            if (format and PROC_SPACE_TERM == PROC_SPACE_TERM){
                term = PROC_SPACE_TERM
            } else if (format and PROC_TAB_TERM == PROC_TAB_TERM){
                term = PROC_TAB_TERM
            } else if (format and PROC_NEWLINE_TERM == PROC_NEWLINE_TERM){
                term = PROC_NEWLINE_TERM
            }
            do {
                val code = inputBytes[i++].toInt()
                if (code!=term && code!=-1){
                    if (procParens && code!='('.toInt() && code!=')'.toInt()){
                        byteBuffer.add(code.toByte())
                    }else{
                        byteBuffer.add(code.toByte())
                    }
                }
            }while (code!=term && i<end)

            if (format and PROC_OUT_STRING ==  PROC_OUT_STRING){
                outStrings?.set(strIndex, String(byteBuffer.toByteArray()))
                strIndex++
            }else if (format and PROC_OUT_FLOAT == PROC_OUT_FLOAT){
                outFloats?.set(floatIndex, String(byteBuffer.toByteArray()).toFloat())
                floatIndex++
            } else if (format and PROC_OUT_LONG == PROC_OUT_LONG){
                outLongs?.set(longIndex, String(byteBuffer.toByteArray()).toLong())
                longIndex++
            }
        }
        return true
    }


    fun BufferedReader.readNextSegment(): ByteArray {
        val bytes = mutableListOf<Byte>()
        var nextInt:Int;
        do {
            nextInt = this.read()
            bytes.add(nextInt.toByte())
        }while (nextInt!= PROC_SPACE_TERM
            && nextInt!= PROC_TAB_TERM
            && nextInt!= PROC_NEWLINE_TERM
            && nextInt!=-1)
        return bytes.toByteArray()
    }

}