package com.antoco.lib_sonar

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.antoco.lib_sonar.bean.MFloatArray
import com.antoco.lib_sonar.bean.SonarData
import com.antoco.lib_sonar.bean.WorkState
import com.antoco.lib_sonar.manager.Sonar2PipeProcessorManager
import com.antoco.lib_sonar.socket.NettyClient
import com.antoco.lib_sonar.socket.NettyConnectListener
import com.antoco.lib_sonar.socket.OnSendAndRecvListener
import com.antoco.lib_sonar.socket.SendAndReceiveManager
import com.antoco.lib_sonar.utils.SonarDataWriter
import com.antoco.lib_sonar.view.pipe.BasePipeView
import com.antoco.lib_sonar.view.sonarview.SonarGlView
import java.lang.ref.WeakReference

/**********************************
 * @Name:         SonarManager
 * @Copyright：  Antoco
 * @CreateDate： 2023/6/17 11:04
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:
 **********************************/
object SonarManager {
    private var listener : WeakReference<SonarListener> ?= null
    private var activity : WeakReference<Activity> ?= null
    private var sonarViewMap = mutableMapOf<Int,WeakReference<SonarGlView>>()
    private var pipeViewMap = mutableMapOf<Int,WeakReference<BasePipeView>>()
    private var recodeData = false
    private var path : String? = null

    private var sonarDataWriter : WeakReference<SonarDataWriter>?=null

    private var isStart = false
    private var perDegree = 6
    private var range2M = 6
    private var gain = 10

    private var address : String = "192.168.1.8"
    private var port = 23
    private val mainHandler : Handler = Handler(Looper.getMainLooper())

    fun setRecordToFile(activity : Activity,path : String = ""):SonarManager{
        if(activity is ComponentActivity){
            (activity as LifecycleOwner).lifecycle.addObserver(object : LifecycleEventObserver {

                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if(event == Lifecycle.Event.ON_DESTROY){
                        closeDataWriter()
                        (activity as LifecycleOwner).lifecycle.removeObserver(this)
                    }
                }
            })
        }
        SonarManager.activity = WeakReference(activity)
        recodeData = true
        this.path = path
        return this
    }


    fun attachSonarView(view : SonarGlView):SonarManager{
        sonarViewMap[(view as View).id] = WeakReference(view)
        return this
    }

    fun detachSonarView(view : SonarGlView):SonarManager{
        sonarViewMap.remove((view as View).id)
        return this
    }

    fun attachPipeView(view : BasePipeView):SonarManager{
        pipeViewMap[(view as View).id] = WeakReference(view)
        return this
    }

    fun detachPipeView(view : BasePipeView):SonarManager{
        pipeViewMap.remove((view as View).id)
        return this
    }

    fun setAddress(address:String =  "192.168.1.8",
                        port:Int = 23):SonarManager{
        this.address = address
        this.port = port
        return this
    }


    /***
     * SonarListener 传入不能是在方法里面直接new，因为这里接收是用的WeakReference方法。
     */
    fun connectSonar(sonarListener : SonarListener){
        listener = WeakReference(sonarListener)
        SendAndReceiveManager.instance.onSendAndRecvListener = object : OnSendAndRecvListener{
            override fun onSendStatus(isSuccess: Boolean) {
                listener?.get()?.onSendStatus(isSuccess)
            }

            override fun onSonarStart() {
                writeStart()
                isStart = true
                for (sv in sonarViewMap){
                    sv.value.get()?.start()
                }
                listener?.get()?.onSonarStart()
            }

            override fun onSonarStop() {
                writePause()
                isStart = false
                for (sv in sonarViewMap){
                    sv.value.get()?.stop()
                }
                listener?.get()?.onSonarStop()
            }

            private var dataCount = 0L
            private var time = 0L

            @SuppressLint("SetTextI18n")
            override fun onSonarData(sonarData: SonarData) {
                if(perDegree != sonarData.perDegree){
                    perDegree = sonarData.perDegree
                    for (sv in sonarViewMap){
                        sv.value.get()?.clear()
                    }
                }
                if(range2M != sonarData.range2M) {
                    range2M = sonarData.range2M
                    for (sv in sonarViewMap){
                        sv.value.get()?.setRange(sonarData.range2M)
                    }
                }
                if(gain != sonarData.gain){
                    gain = sonarData.gain
                    for (sv in sonarViewMap){
                        sv.value.get()?.setGain(gain)
                    }
                }
                //此处数据合并，将角度数据附着到距离数据的最后面，方便处理和记录,
                if(isStart && !sonarData.useless){
                    time = dataCount++ * 100
                    val data = MFloatArray.obtain(7)
                    sonarData.measureDistance2M.copyInto(data.data)
                    data.data[6] = sonarData.degree.toFloat()
                    data.time = time
                    for (sv in sonarViewMap){
                        sv.value.get()?.updateData(data.clone())
                    }
                    writeSonarData(data.clone())
                    Sonar2PipeProcessorManager.analysisData(data.clone())
                    data.recycle()
                    listener?.get()?.onSonarTime(time)
                }
                mainHandler.post{
                    listener?.get()?.onSonarData(sonarData)
                    sonarData.recycle()
                }
            }
        }
        NettyClient.instance.setAddress(address,port)
        NettyClient.instance.connect(object : NettyConnectListener {
            override fun onConnected() {
                Log.i("SonarManager","SOCKET连接成功")
                SendAndReceiveManager.instance.start()
                openDataWriter()
                listener?.get()?.onConnected()
            }

            override fun onConnectedFailed() {
                Log.e("SonarManager","SOCKET连接失败")
                listener?.get()?.onConnectedFailed()
                SendAndReceiveManager.instance.stop()
            }

            override fun onDisConnected() {
                listener?.get()?.onDisConnected()
                closeDataWriter()
                Log.e("SonarManager","SOCKET断开连接")
                SendAndReceiveManager.instance.stop()
            }

            override fun onReConnect() {
                listener?.get()?.onReConnect()
                Log.i("SonarManager","SOCKET重新连接中......")
                SendAndReceiveManager.instance.stop()
            }
        })

        Sonar2PipeProcessorManager.setOnDataListener { verts, indices, normals, s, e ->
            for(wV in pipeViewMap){
                wV.value.get()?.setData(verts,indices,normals,s,e)
            }
            listener?.get()?.onPipeData(verts,indices,normals,s,e)
        }
    }

    fun disconnect() {
        Sonar2PipeProcessorManager.setOnDataListener(null)
        NettyClient.instance.disconnect()
    }

    fun openSonarDevice(){
        SendAndReceiveManager.instance.sendStartState(WorkState.START)
    }

    fun closeSonarDevice(){
        SendAndReceiveManager.instance.sendStartState(WorkState.STOP)
    }

    fun changePerRotateAngle(i: Int) {
        SendAndReceiveManager.instance.sendPerRotateAngle(i)
    }

    fun changeRange(i: Int) {
        SendAndReceiveManager.instance.sendRange(i)
    }

    fun changeGain(i: Int) {
        SendAndReceiveManager.instance.sendGain(i)
    }

    private fun writeStart() {
        sonarDataWriter?.get()?.start()
    }

    private fun writePause() {
        sonarDataWriter?.get()?.pause()
    }

    private fun openDataWriter(){
        if(!recodeData)return
        if(activity?.get() == null)return
        closeDataWriter()
        sonarDataWriter = WeakReference(SonarDataWriter(activity?.get()!!,path))
    }

    private fun writeSonarData(data: MFloatArray) {
        if(!recodeData){
            data.recycle()
            return
        }
        sonarDataWriter?.get()?.write(data)
    }


    private fun closeDataWriter(){
        if(!recodeData)return
        sonarDataWriter?.get()?.close()
        sonarDataWriter = null
    }
}

interface SonarListener : NettyConnectListener, OnSendAndRecvListener {
    fun onSonarTime(time : Long)
    fun onPipeData(verts:FloatArray, indices:IntArray ,normals:FloatArray,startPos:Float,endPos:Float)
}

abstract class SimpleSonarListener : SonarListener{

    override fun onPipeData(
        verts: FloatArray,
        indices: IntArray,
        normals: FloatArray,
        startPos: Float,
        endPos: Float
    ) {
    }

    override fun onSonarTime(time: Long) {
    }

    override fun onSendStatus(isSuccess: Boolean) {
    }
}