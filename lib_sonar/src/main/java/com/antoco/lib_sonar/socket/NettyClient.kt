package com.antoco.lib_sonar.socket

import android.util.Log
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import kotlinx.coroutines.*
import java.lang.Exception
import java.net.InetSocketAddress

/**********************************
 * @Name:         NettyClient
 * @Copyright：
 * @CreateDate： 2023/1/30 14:30
 * @author:      HuangFeng
 * @Version：    1.0
 * @Describe:
 *
 **********************************/
internal class NettyClient {
    private val TAG = NettyClient::class.java.name

    private var address : String = "192.168.1.8"
    private var port = 23

    private var group: EventLoopGroup? = null
    // 客户端启动类
    private var bootstrap: Bootstrap? = null
    private var channel: Channel? = null

    private var destroy = false
    private var isConnect = false
    private val reconnectIntervalTime: Long = 5000
    private var reconnectNum = Int.MAX_VALUE

    private var nettyConnectListener: NettyConnectListener?=null


    private var job : Job?=null
    private val scope : CoroutineScope by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        CoroutineScope(Dispatchers.IO)
    }

    companion object {
        val instance: NettyClient by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            NettyClient()
        }
    }


    fun setAddress(address:String,port:Int){
        this.port = port
        this.address = if (address.startsWith("http")||address.startsWith("https")) {
            val arr = address.split(":").toTypedArray()
            // ip地址 "127.0.0.1"
            arr[1].replace("//", "")
        }else{
            address
        }
        Log.d(TAG, "address=$address")
    }

    private fun init(){
        group = NioEventLoopGroup()
        bootstrap = Bootstrap().apply {
            group(group) // 设置线程组
                .channel(NioSocketChannel::class.java) // 设置通道类型
                .remoteAddress(InetSocketAddress(address, port))
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.RCVBUF_ALLOCATOR, FixedRecvByteBufAllocator(1024 * 1024))
                .handler(object : ChannelInitializer<SocketChannel>() {
                    @Throws(Exception::class)
                    override fun initChannel(socketChannel: SocketChannel) {
                        socketChannel.pipeline()
                            .addLast(ClientHandler())
                    }
                })
        }
    }

    private fun doConnect(intervalTime:Long){
        if(job !=null) job!!.cancel()
        job = scope.launch {
            if(intervalTime>0) delay(intervalTime)
            try {
                init()
                // 阻塞通道
                val channelFuture = bootstrap!!.connect().addListener(ChannelFutureListener { future ->
                    if (future.isSuccess) {
                        isConnect = true
                        nettyConnectListener!!.onConnected()
                        channel = future.channel()
                    } else {
                        isConnect = false
                        nettyConnectListener!!.onConnectedFailed()
                    }
                }).sync()
                channelFuture.channel().closeFuture().sync()
            } catch (e: Exception) {
                e.printStackTrace()
                isConnect = false
                reConnect()
            }
        }
    }

    fun connect(listener: NettyConnectListener){
        if (isConnect) return
        destroy = false
        this.nettyConnectListener = listener
        doConnect(0)
    }

    fun disconnectAndReConnect(listener: NettyConnectListener) {
        this.nettyConnectListener = listener
        isConnect = false
        nettyConnectListener!!.onDisConnected()
        reConnect()
    }

    fun disconnectAndReConnect() {
        Log.i(TAG,"手动进行SOCKET重新连接")
        isConnect = false
        nettyConnectListener!!.onDisConnected()
        reConnect()
    }

    fun disconnect() {
        scope.launch(Dispatchers.IO){
            try {
                destroy = true
                nettyConnectListener?.onDisConnected()
                nettyConnectListener = null
                channel?.close()
                group?.shutdownGracefully()
                job?.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun reConnect(){
        if (destroy) return
        if (reconnectNum > 0 && !isConnect) {
            reconnectNum--
            job!!.cancel()
            group!!.shutdownGracefully()
            doConnect(reconnectIntervalTime)
            Log.e(TAG,  "${address}:$port 重新连接")
            nettyConnectListener!!.onReConnect()
        } else {
            job!!.cancel()
            group!!.shutdownGracefully()
        }
    }

    fun sendMsgToServer(msg: ByteArray): Boolean {
        try {
            val flag = channel != null && isConnect
            if (flag) {
                val buf = Unpooled.copiedBuffer(msg)
                channel!!.writeAndFlush(buf)
                return true
            }
            return false
        }catch (e:Exception){
            e.printStackTrace()
            return false
        }
    }


    inner class ClientHandler : SimpleChannelInboundHandler<ByteBuf>() {

        override fun channelActive(ctx: ChannelHandlerContext) {
            isConnect = true
            super.channelActive(ctx)
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            isConnect = false
            super.channelInactive(ctx)
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            Log.e(TAG,cause.toString())
            isConnect = false
            nettyConnectListener!!.onDisConnected()
            cause.printStackTrace()
            ctx.close()
            reConnect()
        }


        override fun messageReceived(ctx: ChannelHandlerContext, msg: ByteBuf) {
            //创建字节数组,buffer.readableBytes可读字节长度
            val b = ByteArray(msg.readableBytes())
            //复制内容到字节数组b
            msg.readBytes(b)
//            Log.e(TAG, "messageReceived = ${b.toHexString()}" )
            SendAndReceiveManager.instance.onData(b)
        }

    }

}