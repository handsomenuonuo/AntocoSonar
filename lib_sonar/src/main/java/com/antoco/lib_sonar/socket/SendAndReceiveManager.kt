package com.antoco.lib_sonar.socket

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.annotation.IntRange
import com.antoco.lib_sonar.bean.SonarData
import com.antoco.lib_sonar.bean.WorkState
import com.antoco.lib_sonar.utils.toHexString
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
            var checkEnd  = false
            val cacheList = mutableListOf<Byte>()
            while (isStart){
                try {
                    val d = receiveQueue.take()
                    if(!checkEnd){//不需要检测结束标志位
                        if(d.toByte() == 0xaa.toByte()){ //第一次找到0xaa
                            checkEnd  = true //开始检测尾
                        }else{//不需要检测尾
                            cacheList.add(d)
                        }
                    }else{
                        if(d.toByte() == 0xaa.toByte()) {//第二次找到0xaa,确定是尾
                            if(cacheList.isNotEmpty()){//数据不为空，代表之前有数据
                                //获取添加结束位
                                cacheList.add(0xaa.toByte())
                                cacheList.add(0xaa.toByte())
                                //获取添加校验位
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
                                //根据收到的数据长度来判断是不是应答
                            }else{//数据为空

                            }
                        }else{//第二次找到不是0xaa，代表是数据中间的数据，加到队列
                            cacheList.add(0xaa.toByte())
                            cacheList.add(d)
                        }
                        checkEnd  = false
                    }

                }catch (e :Exception){
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
            if(sonarData.workState == WorkState.START){
                onSendAndRecvListener?.onSonarStart()
            }else{
                onSendAndRecvListener?.onSonarStop()
            }
            workState = sonarData.workState
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


    //塞入socket发过来的消息
    fun onData(data : ByteArray){
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

    /**
     * 发送
     */
    private fun sendConfigSet(workState: WorkState,range:Int,gain: Int,perRotateAngle:Int){
        Log.e(tag,"sendConfigSet")
        if(!isStart())return
        val f = {
            encodeSendCommand(workState,gain,range,perRotateAngle)
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
