package com.antoco.lib_sonar.socket

/**********************************
 * @Name:         NettyConnectListener
 * @Copyright：  CreYond
 * @CreateDate： 2023/1/30 14:32
 * @author:      HuangFeng
 * @Version：    1.0
 * @Describe:
 *
 **********************************/
interface NettyConnectListener {
    fun onConnected()
    fun onConnectedFailed()
    fun onDisConnected()
    fun onReConnect()
}