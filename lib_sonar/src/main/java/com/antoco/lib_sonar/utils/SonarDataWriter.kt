package com.antoco.lib_sonar.utils

import android.content.Context
import com.antoco.lib_sonar.bean.MFloatArray
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**********************************
 * @Name:         SonarDataWriter
 * @Copyright：  Antoco
 * @CreateDate： 2023/5/9 15:47
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:
 **********************************/
internal class SonarDataWriter(private val context : Context,private var dic : String ? = "") {

    private var file : File ?=null
    private var fileOutputStream : FileOutputStream ?= null

    private var outputStream : DataOutputStream?=null

    private val dataQueue = LinkedBlockingQueue<MFloatArray>()

    private var thread  :Thread ? = null

    @Volatile
    private var isStart = false

    private val obj = Any()

    init {
        if(dic.isNullOrEmpty())dic = context.getExternalFilesDir("sonarData").toString()
    }

    fun write(distance : MFloatArray){
        if(isStart){
            dataQueue.add(distance)
        }
    }

    private fun doWrite(array : MFloatArray){
        synchronized(obj){
            outputStream?.writeLong(array.time)
            repeat(array.data.count()) {
                outputStream?.writeFloat(array.data[it])
            }
            array.recycle()
        }
    }

    fun start(){
        if(isStart){
            return
        }
        isStart = true
        dataQueue.clear()
        if(file == null){
            file = File(dic + File.separator + System.currentTimeMillis().millis2String("yyyyMMddHHmmss" )+ ".bin")
            fileOutputStream  = FileOutputStream(file,true)
            outputStream = DataOutputStream(fileOutputStream)
        }
        thread = thread{
            try {
                while (isStart){
                    val data = dataQueue.take()
                    doWrite(data)
                }
            }catch (e : Exception){
                e.printStackTrace()
            }
        }
    }

    fun pause(){
        isStart = false
        thread?.interrupt()
        thread = null
        outputStream?.flush()
    }

    fun close() {
        synchronized(obj){
            isStart = false
            thread?.interrupt()
            thread = null
            dataQueue.clear()
            fileOutputStream?.close()
            outputStream?.close()

            if(file?.length() == 0L){
                file!!.delete()
            }
        }
    }

}