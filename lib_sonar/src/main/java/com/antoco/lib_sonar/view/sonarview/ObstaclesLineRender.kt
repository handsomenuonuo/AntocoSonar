package com.antoco.lib_sonar.view.sonarview

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import androidx.core.graphics.toColorLong
import com.antoco.lib_sonar.utils.MGl30Utils
import com.antoco.lib_sonar.utils.toColorArray
import com.antoco.lib_sonar.utils.toFloatBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
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
internal class ObstaclesLineRender(
    private val context : Context,
    private val color : Int = 0xffff7f33.toInt()
    ) : GLSurfaceView.Renderer {

    private var mProgram  : Int = 0
    private var uTextColor : Int = 0

    private  val obj = Any()
    private var vBuffer = mutableListOf<FloatBuffer>()

    private var tranX = 0f
    private var tranY = 0f

    //模型矩阵
    val modelM = FloatArray(16)
    private var colorBuf: FloatBuffer = color.toColorArray().toFloatBuffer()
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        mProgram = MGl30Utils.createProgram(context,"VShader1.glsl","FShader.glsl")
        GLES30.glLineWidth(10.0f)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        Matrix.setIdentityM(modelM, 0) // 初始化投影矩阵
    }

    override fun onDrawFrame(gl: GL10?) {
        synchronized(obj){
            if(vBuffer.isEmpty())return
            //使用着色器程序
            GLES30.glUseProgram(mProgram)

            Matrix.setIdentityM(modelM, 0) // 初始化投影矩阵
            Matrix.translateM(modelM,0,tranX,tranY,0f)
            val matrixLoc = GLES30.glGetUniformLocation(mProgram, "u_Matrix")
            uTextColor =  GLES30.glGetUniformLocation(mProgram, "uTextColor")
            // 输入变换矩阵信息
            GLES30.glUniformMatrix4fv(matrixLoc, 1, false, modelM, 0)
            GLES30.glUniform4fv(uTextColor,1,colorBuf)

            vBuffer.forEach {
                GLES30.glVertexAttribPointer(0,2, GLES30.GL_FLOAT,false,0,it)
                //打开使用数据的开关
                GLES30.glEnableVertexAttribArray(0)
                GLES30.glDrawArrays(GLES30.GL_LINE_STRIP,0,(it.limit()*0.5).toInt())
            }

            GLES30.glDisableVertexAttribArray(0)
        }

    }

    fun updateData(src : MutableList<FloatArray>,tranX : Float, tranY : Float){
        synchronized(obj){
            this.tranX = tranX
            this.tranY = tranY
            vBuffer.clear()
            src.forEach {
                vBuffer.add(it.toFloatBuffer())
            }
        }
    }

    fun clear(){
        synchronized(obj){
            this.tranX = 0f
            this.tranY = 0f
            vBuffer.clear()
        }
    }
}