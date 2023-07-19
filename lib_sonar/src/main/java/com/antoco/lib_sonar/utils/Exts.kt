package com.antoco.lib_sonar.utils

import android.annotation.SuppressLint
import android.content.res.Resources
import android.util.Log
import java.nio.charset.Charset
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


/**********************************
 * @Name:         Exts
 * @Copyright：  CreYond
 * @CreateDate： 2022/12/14 8:38
 * @author:      HuangFeng
 * @Version：    1.0
 * @Describe:
 *
 **********************************/
/**
 * byte数组转hex
 * @param
 * @return
 */
fun ByteArray.toHexString():String{
    var strHex: String
    val sb = StringBuilder("")
    for (b in this) {
        strHex = Integer.toHexString(b.toInt() and 0xFF)
        sb.append(if (strHex.length == 1) "0$strHex" else strHex) // 每个字节由两个字符表示，位数不够，高位补0
    }
    return sb.toString().trim()
}

/**
 * byte转hex
 * @param
 * @return
 */
fun Byte.toHexString():String{
    val strHex = Integer.toHexString(this.toInt() and 0xFF)
    return if (strHex.length == 1) "0$strHex" else strHex
}

/**
 * 十六进制字符串转字节码
 *
 * @param
 * @return
 */
fun String.hex2ByteArray(): ByteArray?{
    val b = this.toByteArray()
    if (b.size % 2 != 0) {
        Log.d(
            "hex2ByteArray",
            "ERROR: 转化失败  le= " + b.size + " b:" + this
        )
        return null
    }
    val b2 = ByteArray(b.size / 2)
    var n = 0
    while (n < b.size) {

        // if(n+2<=b.length){
        val item = String(b, n, 2)
        // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个进制字节
        b2[n / 2] = item.toInt(16).toByte()
        n += 2
    }
    return b2
}

/**
 * 将string转为固定长度byte数组，不足前面补0
 * @receiver String
 * @param bytesLen Int
 * @return ByteArray
 */
fun String.toBytes(bytesLen : Int,charset: Charset = Charsets.UTF_8):ByteArray{
    val cis = this.toByteArray(charset)
    return if(cis.size > bytesLen){
        this.toByteArray().copyOfRange(0,bytesLen)
    }else{
        val bs = ByteArray(bytesLen)
        cis.copyInto(bs,bytesLen-cis.size,0,cis.size)
    }
}

fun Int.toBigByteArray2():ByteArray{
    return byteArrayOf((this shr 8).toByte(), (this and 0xFF).toByte())
}

fun Int.toBigByteArray4():ByteArray{
    return byteArrayOf((this shr 24).toByte(), (this shr 16).toByte(),(this shr 8).toByte(),(this and 0xFF).toByte())
}

fun toBigUInt(b1:Byte, b2: Byte):Int{
    return (b1.toInt() and 0xff shl 8) or (b2.toInt() and 0xff)
}

fun toBigInt(b1:Byte, b2: Byte):Int{
    return (b1.toInt()  shl 8) or (b2.toInt() and 0xff )
}

fun toBigInt(b1:Byte, b2: Byte,b3:Byte, b4: Byte):Int{
    return ((b1.toInt() and 0xff)shl 24) or
            ((b2.toInt() and 0xff) shl 16) or
            ((b3.toInt() and 0xff) shl 8) or
            ((b4.toInt() and 0xff))
}


operator fun FloatArray.plus(floatArray: FloatArray):FloatArray{
    val dest = FloatArray(this.size + floatArray.size)
    this.copyInto(dest,0,0,this.size)
    floatArray.copyInto(dest,this.size+1,dest.size)
    return dest
}

operator fun FloatArray.plus(last: Int):FloatArray{
    val dest = FloatArray(this.size + 1)
    this.copyInto(dest,0,0,this.size)
    dest[dest.size-1] = last.toFloat()
    return dest
}

fun Float.sp2px(): Int {
    val fontScale = Resources.getSystem().displayMetrics.scaledDensity
    return (this * fontScale + 0.5f).toInt()
}

fun String.format(vararg args: Any?): String {
    var text = this
    if (args.isNotEmpty()) {
        try {
            text = String.format(this, *args)
        } catch (e: IllegalFormatException) {
            e.printStackTrace()
        }
    }
    return text
}

@SuppressLint("SimpleDateFormat")
fun Long.millis2String(pattern: String): String {
    val simpleDateFormat = SimpleDateFormat(pattern)
    return simpleDateFormat.format(Date(this))
}

@SuppressLint("SimpleDateFormat")
fun String.toMillis(pattern: String): Long {
    try {
        val simpleDateFormat = SimpleDateFormat(pattern)
        return simpleDateFormat.parse(this)?.time ?: -1
    } catch (e: ParseException) {
        e.printStackTrace()
    }
    return -1
}