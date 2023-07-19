package com.antoco.lib_sonar.socket

import android.util.Log
import androidx.annotation.IntRange
import com.antoco.lib_sonar.bean.BaseData
import com.antoco.lib_sonar.bean.SonarData
import com.antoco.lib_sonar.bean.WorkState
import com.antoco.lib_sonar.utils.toBigByteArray2
import com.antoco.lib_sonar.utils.toBigInt
import com.antoco.lib_sonar.utils.toBigUInt
import com.antoco.lib_sonar.utils.toHexString
import java.text.DecimalFormat
import java.util.Random




internal fun encodeSendCommand(
    state : WorkState,
    gain : Int,
    @IntRange(from = 1, to = 6000) range:Int,
    @IntRange(from = 1, to = 60) perDegree:Int):ByteArray{

    val sendCommand = ByteArray(28)
    sendCommand[0] = 0x55
    sendCommand[1] = 0x55
    sendCommand[2] = 0x15
    when(state){
        WorkState.STOP->{
            sendCommand[3] = 0x00
        }
        WorkState.START ->{
            sendCommand[3] = 0x01
        }
    }
    val r = range.toBigByteArray2()
    sendCommand[4] = r[0]
    sendCommand[5] = r[1]
    sendCommand[6] = gain.toByte()
    sendCommand[7] = perDegree.toByte()
    sendCommand[24] = 0xaa.toByte()
    sendCommand[25] = 0xaa.toByte()
    val crc = getCRC(sendCommand.copyOfRange(0,26))
    val crcBs = crc.toBigByteArray2()
    sendCommand[26] = crcBs[1]
    sendCommand[27] = crcBs[0]

    Log.e("encodeSendCommand",sendCommand.toHexString())
    return sendCommand
}

internal fun decodeCommand(command : ByteArray,callback : (BaseData)-> Unit){
    when (command.size) {
        42 -> {
            doDecode(command,callback)
        }
        28 -> {
            val baseData = BaseData()
            baseData.srcByteArray = command
            callback.invoke(baseData)
        }
        else -> return
    }
}

internal fun init(){
    cacheDistance = floatArrayOf(-1f,-1f,-1f,-1f,-1f,-1f)
    cacheCount = intArrayOf(judgeZeroCount,judgeZeroCount,judgeZeroCount,judgeZeroCount,judgeZeroCount,judgeZeroCount)
}

private const val judgeZeroCount = 0
private var cacheDistance = floatArrayOf(-1f,-1f,-1f,-1f,-1f,-1f)
private var cacheCount = intArrayOf(judgeZeroCount,judgeZeroCount,judgeZeroCount,judgeZeroCount,judgeZeroCount,judgeZeroCount)

//var testDegree = 0
private fun doDecode(command : ByteArray,callback : (SonarData)-> Unit){
    //先判断数据长度
    if(command.size != 42)return
    //再判断校验位
    val crc =  getCRC(command.copyOfRange(0,40))
    val crc1 = (command[41].toInt() and 0xff shl  8) or (command[40].toInt() and 0xff)
    if(crc != crc1){
//        println("校验crc失败")
        Log.e("decodeCommand","校验crc失败  crc = $crc  comCrc = $crc1")
        return
    }
    val sonarData = SonarData.obtain()
    //解析数据
    sonarData.range = toBigUInt(command[3],command[4])
    sonarData.range2M =  sonarData.range.toBigDecimal().multiply(0.001f.toBigDecimal()).toFloat()
    sonarData.workState = if(command[5].toInt() == 0) WorkState.STOP else WorkState.START
    sonarData.gain = command[6].toInt()
//    sonarData.samplingTime = command[7].toInt()
    sonarData.degree = toBigUInt(command[7],command[8])
    sonarData.perDegree = command[9].toInt()

//    if(testDegree >=360) testDegree = 0
//    sonarData.degree = testDegree
//    testDegree+=6

    var sum = 0f
//    Log.e("test","================")
    repeat(6){
        if(command[10+it*2] == 0xFF.toByte() && command[11+it*2] == 0xFF.toByte()){
            sonarData.measureDistance[it] = 0f
        }else{
            sonarData.measureDistance[it] = toBigUInt(command[10+it*2],command[11+it*2]) * 0.1f
        }
        if(sonarData.measureDistance[it] == 0f){
            if(cacheDistance[it] != 0f && cacheCount[it] != 0){
                sonarData.measureDistance[it] = cacheDistance[it]
                cacheCount[it] -= 1
            }
        }else{
            cacheCount[it] = judgeZeroCount
            cacheDistance[it] = sonarData.measureDistance[it]
        }


//        Log.e("test","${sonarData.measureDistance[it]}")
//        sonarData.measureDistance[it] = 1431f + Random().nextInt(200)
        //当距离为0时，代表距离过近
//        if(sonarData.measureDistance[it] == 0f)sonarData.measureDistance[it] = 150f

        sonarData.measureDistance2M[it] =  sonarData.measureDistance[it] * 0.001f
        sum += sonarData.measureDistance[it]
    }
//判断数据包是否有效
//    if(sum == 0f)sonarData.useless = true
    sonarData.gyro_yaw =  toBigUInt(command[22],command[23]) * 0.1f
    sonarData.gyro_roll =  toBigUInt(command[24],command[25]) * 0.1f
    sonarData.gyro_pitch =  toBigUInt(command[26],command[27]) * 0.1f
    repeat(4){
        sonarData.voltages[it] =  String.format("%.1f",toBigUInt(command[28+it*2],command[29+it*2]) * 0.1f)
    }
    val temp = toBigInt(command[36],command[37])
    sonarData.waterTemp = if(temp != -32768){
        "%.1f".format(temp * 0.1f)
    }else "错误"

    callback.invoke(sonarData)
}

/**
 * 计算CRC16校验码
 *
 * @param bytes
 * @return
 */
internal fun getCRC(bytes: ByteArray): Int {
    var CRC = 0x0000ffff
    val POLYNOMIAL = 0x0000a001
    var i = 0
    var j: Int
    while (i < bytes.size) {
        CRC = CRC xor (bytes[i].toInt() and 0x000000ff)
        j = 0
        while (j < 8) {
            if (CRC and 0x00000001 != 0) {
                CRC = CRC shr 1
                CRC = CRC xor POLYNOMIAL
            } else {
                CRC = CRC shr 1
            }
            j++
        }
        i++
    }
    return CRC
}