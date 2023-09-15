package com.antoco.lib_sonar.utils

import android.content.Context
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.core.util.size
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**********************************
 * @Name:         SonarDataWriter
 * @Copyright：  Antoco
 * @CreateDate： 2023/5/9 15:47
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:   测试用的，用于记录原始数据，用于分析
 **********************************/
internal class CsvWriter(private val context : Context) {



    val file = File(context.getExternalFilesDir("csv")!!.absolutePath
            + File.separator
            + System.currentTimeMillis().millis2String("yyyyMMddHHmmss" )
            + ".csv")

    val bw = BufferedWriter(FileWriter(file, true))

    val queue = LinkedBlockingQueue<SparseArray<Float>>()

    var array = SparseArray<Float>()

    init {
        start()
    }

    private fun start(){
        thread {
            while(true){
                var d = queue.take()
                d.forEach { key, value ->
                    doWrite(key,value)
                }
            }
        }
    }


    fun write(degree: Int,data : Float){
        array[degree] = data
        if(array.size == 120){
            queue.add(array)
            array =  SparseArray<Float>()
        }
    }

    private fun doWrite(degree: Int, distance : Float){
        bw.write("$degree,$distance")
        bw.newLine()
        bw.flush()
    }

    fun close(){
        bw.close()
    }
}