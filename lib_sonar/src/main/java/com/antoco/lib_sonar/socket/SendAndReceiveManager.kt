package com.antoco.lib_sonar.socket

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.annotation.IntRange
import com.antoco.lib_sonar.bean.SonarData
import com.antoco.lib_sonar.bean.WorkState
import com.antoco.lib_sonar.utils.toHexString
import com.antoco.lib_sonar.view.sonarview.SonarSpec
import java.lang.Exception
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**********************************
 * @Name:         SendAndReceiveManager
 * @Copyright：  CreYond
 * @CreateDate： 2023/1/30 8:31
 * @author:      HuangFeng
 * @Version：    1.0
 * @Describe:
 *
 **********************************/
internal class SendAndReceiveManager {

    private val tag = "SendAndReceiveManager"

    companion object {
        val instance: SendAndReceiveManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            SendAndReceiveManager() }
    }

    private val handler:Handler by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        Handler(Looper.getMainLooper())
    }

    private var sendFailCount = 0
    private val MAX_SEND_FAIL_COUNT = 5
    private val SEND_TIME_OUT = 800L//发送超时最大时间
    private var receiveThread : Thread?=null
    private var sendThread : HandlerThread?=null
    @Volatile
    private var isStart = false
    private var sendHandler : Handler?=null
    private var lastCommand:ByteArray? = null//上一次发送的指令
    private val receiveQueue : LinkedBlockingQueue<Byte> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        LinkedBlockingQueue<Byte>()
    }
    /**
     * 存储生成指令的function，在thread里面invoke调用，生成指令并且发送
     */
    private val sendQueue : LinkedBlockingQueue<()->ByteArray> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        LinkedBlockingQueue<()->ByteArray>()
    }

    var onSendAndRecvListener : OnSendAndRecvListener?=null

    private var workState: WorkState = WorkState.STOP
    private var gain: Int = 10
    private var perRotateAngle:Int = 6
    private var range:Int = 2000

    fun start(){
        if(isStart())return
        init()
        sendFailCount = 0
        isStart = true
        receiveQueue.clear()
        sendQueue.clear()
        /**
         * 发送线程，从queue里面取一条数据进行发送，如果queue里面没有数据，阻塞并且等待数据
         */
        sendThread = HandlerThread("SocketSendThread").apply  {
            start()
            sendHandler = Handler(looper) {
                try {
                    val f = sendQueue.take()
                    val command = (f as () -> ByteArray).invoke()
                    lastCommand = command
                    if(NettyClient.instance.sendMsgToServer(command)){
                        handler.removeCallbacks(sendCheckRunnable)
                        handler.postDelayed(sendCheckRunnable,SEND_TIME_OUT)
                    }else{
                        Log.e(tag,"指令发送失败 ${command.toHexString()}")
                        handleSendFail()
                    }
                }catch (e :Exception){
                    e.printStackTrace()
                }
                true
            }
            doSend()
        }

        receiveThread = thread(start = true, name = "SocketRecvThread") {
//            var checkEnd  = false
            val cacheList = mutableListOf<Byte>()
            while (isStart){
                try {
                    val h1 = receiveQueue.take()
                    if(h1.toInt() != 0x55)continue
                    val h2 = receiveQueue.take()
                    if(h2.toInt() != 0x55)continue
                    cacheList.add(h1)
                    cacheList.add(h2)
                    val lenByte = receiveQueue.take()
                    cacheList.add(lenByte)
                    var len = lenByte.toInt()
                    while (len != 0){
                        cacheList.add(receiveQueue.take())
                        len--
                    }
                    val t1 = receiveQueue.take()
                    if(t1.toByte() != 0xaa.toByte()){
                        cacheList.clear()
                        continue
                    }
                    val t2 = receiveQueue.take()
                    if(t2.toByte() != 0xaa.toByte()){
                        cacheList.clear()
                        continue
                    }
                    cacheList.add(t1)
                    cacheList.add(t2)
                    cacheList.add(receiveQueue.take())
                    cacheList.add(receiveQueue.take())
                    val l = cacheList.toByteArray()
                    cacheList.clear()
                    decodeCommand(l){
                        if(it is SonarData){
                            handleCommand(it)
                        }else{
                            if(it.srcByteArray?.size == 28){//说明是发送后的应答消息
                                Log.d(tag, "收到发送回执，发送成功！")
                                if(it.srcByteArray contentEquals  lastCommand){//如果收到的跟发送的相等，代表发送成功，取消超时检测
                                    sendFailCount = 0
                                    handler.removeCallbacks(sendCheckRunnable)
                                    onSendAndRecvListener?.onSendStatus(true)
                                    doSend()
                                }
                            }
                        }
                    }

                }catch (e :Exception){
                    cacheList.clear()
                    e.printStackTrace()
                }

            }
        }
    }

    /**
     * 发送收不到回执，超时检测
     */
    private val sendCheckRunnable= Runnable {
        Log.e(tag,"sendCheckRunnable")
        handleSendFail()
    }

    /**
     * 执行一次发送
     */
    private fun doSend(){
        sendHandler!!.sendEmptyMessage(0)
    }

    /**
     * 处理发送失败流程
     */
    private fun handleSendFail(){
        Log.e(tag,"handleSendFail")
        onSendAndRecvListener?.onSendStatus(false)
        doSend()
    }

    private fun checkToReConnect(){
        sendFailCount++
        if(sendFailCount >= MAX_SEND_FAIL_COUNT){//达到最大失败次数
            Log.e(tag,"发送失败到达最大次数")
            NettyClient.instance.disconnectAndReConnect()//重新连接
            return
        }
    }

    /**
     * 处理收到的数据流程
     */
    private fun handleCommand(sonarData : SonarData){
        if(workState != sonarData.workState){//当前的工作状态跟收到的不一样
            if(workState == WorkState.STOP){
                onSendAndRecvListener?.onSonarStart()
                init()
            }else if(sonarData.workState == WorkState.STOP){
                onSendAndRecvListener?.onSonarStop()
            }
            workState = sonarData.workState
            SonarSpec.workState = workState
        }
        gain = sonarData.gain
        range = sonarData.range
        perRotateAngle = sonarData.perDegree
        onSendAndRecvListener?.onSonarData(sonarData)
    }


    fun isStart() : Boolean  =  isStart

    fun stop(){
        if(!isStart())return
        receiveQueue.clear()
        sendQueue.clear()
        isStart = false
        try {
            receiveThread!!.interrupt()
            receiveThread = null
        }catch (e:Exception){
            e.printStackTrace()
        }
        try {
            sendThread!!.quit()
            sendThread = null
        }catch (e:Exception){
            e.printStackTrace()
        }
        handler.removeCallbacksAndMessages(null)
        sendHandler!!.removeCallbacksAndMessages(null)
        sendHandler =null

        lastCommand = null
    }

    fun test(){
//        55552307d0010a000006aaaaaaaaaaaaaaaaaaaaaaaa00000000000000f0004800310021013baaaa330e
        start()
        val data = byteArrayOf(
            0x55,0x55,0x23,0x07,
            0xd0.toByte(),0x01,0x0a,0x00,0x00,0x06,0xaa.toByte(),0xaa.toByte(),0xaa.toByte(),
            0xaa.toByte(),0xaa.toByte(),0xaa.toByte(),0xaa.toByte(),0xaa.toByte(),0xaa.toByte(),0xaa.toByte(),
            0xaa.toByte(),0xaa.toByte(),0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xf0.toByte(),0x00,0x48,0x00,
            0x31,0x00,0x21,0x01,0x3b,0xaa.toByte(),0xaa.toByte(),
            0x33,0x0e
        )
        thread {
            while (true) {
                onData(data)
                Thread.sleep(50)
            }
        }
    }


    //塞入socket发过来的消息
    fun onData(data : ByteArray){
        Log.e("test","${data.toHexString()}")
        for(d in data) receiveQueue.add(d)
    }


    fun sendStartState(workState: WorkState){
        if(this.workState == workState)return
        sendConfigSet(workState,range,gain,perRotateAngle)
    }

    fun sendRange(@IntRange(from = 1, to = 6000) range: Int){
        if(this.range == range)return
        sendConfigSet(workState,range,gain,perRotateAngle)
    }

    fun sendGain(@IntRange(from = 10, to = 100) gain: Int){
        if(this.gain == gain)return
        sendConfigSet(workState,range, gain,perRotateAngle)
    }

    fun sendPerRotateAngle(perRotateAngle: Int){
        if(this.perRotateAngle == perRotateAngle)return
        sendConfigSet(workState,range,gain,perRotateAngle)
    }

    fun sendDownUp(down:Int,up:Int){
        sendConfigSet(workState,range, gain,perRotateAngle,down,up)
    }

    /**
     * 发送
     */
    private fun sendConfigSet(workState: WorkState,range:Int,gain: Int,perRotateAngle:Int,downRange:Int=3000,upRange:Int=3320){
        Log.e(tag,"sendConfigSet")
        if(!isStart())return
        val f = {
            encodeSendCommand(workState,gain,range,perRotateAngle,downRange,upRange)
        }
        sendQueue.add(f)
    }
}

interface OnSendAndRecvListener{
    fun onSendStatus(isSuccess : Boolean)
    fun onSonarStart()
    fun onSonarStop()
    fun onSonarData(sonarData: SonarData)
}
