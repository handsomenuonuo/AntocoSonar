package com.antoco.lib_sonar.utils

import android.content.Context
import android.graphics.Color
import android.opengl.GLES30
import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer


internal object MGl30Utils {

    fun createProgram(context: Context,vShaderFileName : String,fShaderFileName : String):Int{
        var mProgram = GLES30.glCreateProgram()
        //初始化着色器
        val vertexShader = initShaderFromAssets(context,GLES30.GL_VERTEX_SHADER,vShaderFileName)
        val fragmentShader = initShaderFromAssets(context,GLES30.GL_FRAGMENT_SHADER,fShaderFileName)
        //将顶点着色器加入程序
        GLES30.glAttachShader(mProgram,vertexShader)
        //将片元着色器加入程序
        GLES30.glAttachShader(mProgram,fragmentShader)
        //链接到着色器程序
        GLES30.glLinkProgram(mProgram)
        //以下为log
        val status = IntArray(1)
        GLES30.glGetProgramiv(mProgram,GLES30.GL_LINK_STATUS,status,0)
        if(status[0] != GLES30.GL_TRUE){
            Log.e("MGl30Utils","glLinkProgram failed")
            mProgram = 0
        }
        return mProgram
    }

    /**
     * 从asset中初始化shader
     */
    fun initShaderFromAssets(context: Context, type:Int, shaderFileName : String):Int{
        val assetManager  = context.applicationContext.assets
        var inputStream : InputStream?=null
        var isr : InputStreamReader?=null
        var br : BufferedReader? = null
        val sb = StringBuilder()
        try {
            inputStream = assetManager.open(shaderFileName)
            isr = InputStreamReader(inputStream)
            br = BufferedReader(isr)

            var line :String ?
            do {
                line = br.readLine()
                if(line != null) sb.append("\n$line")
            }while (line != null)
        }catch (e:Exception){
            e.printStackTrace()
        }finally {
            br?.close()
            isr?.close()
            inputStream?.close()
        }

        val s = sb.toString().trim()
//        Log.e("MGl30Utils","读取结果\n $s")

        return initShader(type,s)
    }

    /**
     * 初始化shader
     */
    fun initShader(type : Int,shaderCode:String):Int{
        //根据type创建着色器
        var shader = GLES30.glCreateShader(type)
        //将代码资源加入到着色器中
        GLES30.glShaderSource(shader,shaderCode)
        //编译
        GLES30.glCompileShader(shader)

        //以下为日志输出
        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS,compiled,0)
        if(compiled[0]==0){
            Log.e("MGl30Utils","Could not compile shader:$type")
            Log.e("MGl30Utils","GLES20 Error:" + GLES30.glGetShaderInfoLog(shader))
            GLES30.glDeleteShader(shader)
            shader = 0
        }
        return shader
    }
}


fun IntArray.toIntBuffer():IntBuffer{
//申请堆外内存
    val byteBuffer = ByteBuffer.allocateDirect(this.size *4)
    byteBuffer.order(ByteOrder.nativeOrder())
    val buffer = byteBuffer.asIntBuffer()
    buffer.put(this)
    buffer.position(0)
    return buffer
}

fun FloatArray.toFloatBuffer():FloatBuffer{
//申请堆外内存
    val byteBuffer = ByteBuffer.allocateDirect(this.size *4)
    byteBuffer.order(ByteOrder.nativeOrder())
    val buffer = byteBuffer.asFloatBuffer()
    buffer.put(this)
    buffer.position(0)
    return buffer
}

fun Int.toColorArray(): FloatArray {
    val result = FloatArray(4)
    // 提取颜色的 RGBA 分量
    val red = Color.red(this)
    val green = Color.green(this)
    val blue = Color.blue(this)
    val alpha = Color.alpha(this)
    // 将整数值转换为浮点值
    result[0] = red / 255f
    result[1] = green / 255f
    result[2] = blue / 255f
    result[3] = alpha / 255f
    return result
}