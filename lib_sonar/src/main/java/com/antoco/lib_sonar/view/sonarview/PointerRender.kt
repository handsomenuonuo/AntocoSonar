package com.antoco.lib_sonar.view.sonarview

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.antoco.lib_sonar.utils.MGl30Utils
import com.antoco.lib_sonar.utils.toFloatBuffer
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**********************************
 * @Name:         DataRender
 * @Copyright：  Antoco
 * @CreateDate： 2023/4/23 9:32
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:    绘制声呐管道数据
 **********************************/
internal class PointerRender(private val context : Context) : GLSurfaceView.Renderer {

    private var mProgram  : Int = 0

    private  val obj = Any()
    private var vBuffer : FloatBuffer?=null

    private var tranX = 0f
    private var tranY = 0f

    //模型矩阵
    val modelM = FloatArray(16)
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        mProgram = MGl30Utils.createProgram(context,"VShader1.glsl","FShader1.glsl")
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        Matrix.setIdentityM(modelM, 0) // 初始化投影矩阵

    }

    override fun onDrawFrame(gl: GL10?) {
        synchronized(obj){
            if(vBuffer==null)return
            //使用着色器程序
            GLES30.glUseProgram(mProgram)

            Matrix.setIdentityM(modelM, 0) // 初始化投影矩阵
            Matrix.translateM(modelM,0,tranX,tranY,0f)
            val matrixLoc = GLES30.glGetUniformLocation(mProgram, "u_Matrix")
            val uTextColor = GLES30.glGetUniformLocation(mProgram, "uTextColor")
            // 输入变换矩阵信息
            GLES30.glUniformMatrix4fv(matrixLoc, 1, false, modelM, 0);

            GLES30.glVertexAttribPointer(0,2, GLES30.GL_FLOAT,false,0,vBuffer)
            //打开使用数据的开关
            GLES30.glEnableVertexAttribArray(0)
            val size = (vBuffer!!.limit()*0.5).toInt()
            if(size <= 120){
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,1f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,0,size)
            }
            else if(size<=240){
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,0.8f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,0,120)
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,1f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,120,size-120)
            }
            else if(size<=360){
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,0.6f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,0,120)
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,0.8f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,120,120)
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,1f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,240,size - 240)
            }
            else if(size<=480){
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,0.4f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,0,120)
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,0.6f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,120,120)
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,0.8f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,240,120)
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,1f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,360,size - 360)
            }
            else if(size<=600){
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,0.2f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,0,120)
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,0.4f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,120,120)
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,0.6f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,240,120)
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,0.8f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,360,120)
                GLES30.glUniform4fv(uTextColor,1, floatArrayOf(1.0f,0.5f,0.2f,1f),0)
                GLES30.glDrawArrays(GLES30.GL_POINTS,480,size - 480)
            }

            GLES30.glDisableVertexAttribArray(0)
        }

    }

    fun updateData(src : FloatArray){
        synchronized(obj){
            if(vBuffer == null || vBuffer!!.limit() != src.size){
                vBuffer = src.toFloatBuffer()
            }else{
                vBuffer!!.clear()
                vBuffer!!.put(src)
                vBuffer!!.position(0)
            }
        }
    }

    fun updateData(src : FloatArray,tranX : Float, tranY : Float){
        synchronized(obj){
            this.tranX = tranX
            this.tranY = tranY
            vBuffer?.clear()
            if(vBuffer == null || vBuffer!!.limit() != src.size){
                vBuffer = src.toFloatBuffer()
            }else{
                vBuffer?.let {
                    it.put(src)
                    it.position(0)
                }
            }
        }
    }

    fun clear(){
        synchronized(obj){
            this.tranX = 0f
            this.tranY = 0f
            vBuffer = null
        }
    }
}