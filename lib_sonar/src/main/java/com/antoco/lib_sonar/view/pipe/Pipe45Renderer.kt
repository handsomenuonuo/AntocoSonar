package com.antoco.lib_sonar.view.pipe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLES32
import android.opengl.GLSurfaceView.Renderer
import android.opengl.GLUtils
import android.opengl.Matrix
import com.antoco.lib_sonar.R
import com.antoco.lib_sonar.utils.MGl30Utils
import com.antoco.lib_sonar.utils.toFloatBuffer
import com.antoco.lib_sonar.utils.toIntBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**********************************
 * @Name:         PipeRenderer
 * @Copyright：  Antoco
 * @CreateDate： 2023/5/10 18:36
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:
 **********************************/
internal class Pipe45Renderer(val context: Context): Renderer {

    private var mProgram = 0
    private var texture = 0
    private var textureHandle : Int = 0
    //投影矩阵
    private val projectionM = FloatArray(16)
    //模型矩阵
    private val modelM = FloatArray(16)
    //视图矩阵
    private val viewM = FloatArray(16)
    //结果矩阵
    private val resultM = FloatArray(16)
    //结果矩阵1
    private val resultM1 = FloatArray(16)

    private var vertexBuffer : FloatBuffer? = null
    private var indicesBuffer : IntBuffer? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.12f,0.156f,0.27f,0f)
        mProgram = MGl30Utils.createProgram(context,"PipeVShader.glsl","PipeFShader.glsl")

        GLES30.glUseProgram(mProgram)
        textureHandle = GLES30.glGetUniformLocation(mProgram, "Texture")
        loadTexture()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0,0,width,height)
        // 启用深度测试
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES30.glEnable(GLES20.GL_TEXTURE_2D)

        val aspectRatio = if(width>height)  1.0f*width/height else 1.0f*height/width
        Matrix.setIdentityM(projectionM, 0); // 初始化投影矩阵
        // 计算矩阵的正交投影
//        Matrix.orthoM(projectionM, 0, -aspectRatio, aspectRatio,-1f, 1f,  -1f, 10f)
        //设置透视投影
        Matrix.frustumM(projectionM, 0, -aspectRatio, aspectRatio, -1f, 1f, 1f, 100f)

    }

    private var localEyeLoc = 6f
    private var eyeLoc = localEyeLoc
    fun scale(scale :Float){
        val d = localEyeLoc / scale
        eyeLoc = if(d < 2) 2f else d
    }

    fun scaleEnd() {
        localEyeLoc = eyeLoc
    }

    private var tranX = 0f
    private var tranY = 0f
    fun translate(x :Float,y : Float){
        tranX += x*eyeLoc
        tranY -= y *eyeLoc
    }

    private var locAngleX = 0f
    private var locAngleY = 0f
    private var angleX = locAngleX
    private var angleY = locAngleY
    fun rotate(x :Float,y : Float){
        locAngleX -= x
        angleX = locAngleX
        locAngleY -= y
        angleY = locAngleY
    }

    fun rotateEnd(){
        angleX = locAngleX
    }

    private fun loadTexture(){
        var bitmap :Bitmap?=null
        try {
            bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.shuini)
            val textures = IntArray(1)
            // 创建纹理 指定生成N个纹理（第一个参数指定生成N个纹理），textures数组将负责存储所有纹理的代号。
            GLES30.glGenTextures(1,textures,0)
            //生成第一个纹理
            // 获取textures纹理数组中的第一个纹理
            texture = textures[0]
            // 通知OpenGL将texture纹理绑定到GL10.GL_TEXTURE_2D目标中
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,texture)
            useTexParameter()
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D,0,bitmap,0)

            //修改着色器里的值
            GLES30.glUniform1i(textureHandle, 0)
        }catch (e :Exception){
            e.printStackTrace()
        }finally {
            bitmap?.recycle()
        }
//将纹理单元和纹理对象进行绑定
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
    }

    private fun useTexParameter() {
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES30.glTexParameterf(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_LINEAR.toFloat()
        )
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES30.glTexParameterf(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_LINEAR.toFloat()
        )
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES30.glTexParameterf(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_REPEAT.toFloat()
        )
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES30.glTexParameterf(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES32.GL_REPEAT.toFloat()
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        synchronized(obj){
            vertexBuffer?.let {
                // 清空颜色缓冲区和深度缓冲区
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT);

                // 使用管道着色器程序
                GLES20.glUseProgram(mProgram);

                Matrix.setIdentityM(modelM, 0) // 初始化模型矩阵

//                Matrix.rotateM(modelM, 0, -50f,0f, 1f, 0f) // 旋转模型矩阵
//                Matrix.rotateM(modelM, 0, 25f,1f, 0f, 0f) // 旋转模型矩阵
//                //设置相机位置
//                Matrix.setLookAtM(viewM, 0, 1.7f, 1.25f, eyeLoc, 1.7f, 1.25f, 0f, 0f, 1f, 0f)
//                // 把投影矩阵和模型矩阵相乘，得到最终的变换矩阵
//                Matrix.multiplyMM(resultM, 0, projectionM, 0, viewM, 0)
//                //计算变换矩阵
//                Matrix.multiplyMM(resultM1,0,resultM,0,modelM,0)
                Matrix.translateM(modelM, 0, 0f, 0f, length/2f)
                Matrix.rotateM(modelM, 0, angleX,0f, 1f, 0f) // 旋转模型矩阵
                Matrix.rotateM(modelM, 0, angleY,1f, 0f, 0f) // 旋转模型矩阵
                Matrix.translateM(modelM, 0, 0f, 0f, -length/2f)  // 将矩阵平移回原始位置
                //设置相机位置
                Matrix.setLookAtM(viewM, 0, tranX, tranY, eyeLoc, tranX, tranY, 0f, 0f, 1f, 0f);
                // 把投影矩阵和模型矩阵相乘，得到最终的变换矩阵
                Matrix.multiplyMM(resultM, 0, projectionM, 0, viewM, 0)
                //计算变换矩阵
                Matrix.multiplyMM(resultM1,0,resultM,0,modelM,0)

                val matrixLoc = GLES30.glGetUniformLocation(mProgram, "u_Matrix");
                // 输入变换矩阵信息
                GLES30.glUniformMatrix4fv(matrixLoc, 1, false, resultM1, 0)

                /**===================不使用缓冲=======================**/
                vertexBuffer!!.position(0)
                GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
                GLES20.glEnableVertexAttribArray(0)
                vertexBuffer!!.position(3)
                GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer)
                GLES20.glEnableVertexAttribArray(1)
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesBuffer!!.limit(), GLES20.GL_UNSIGNED_INT, indicesBuffer)

                // 禁用顶点数组
                GLES20.glDisableVertexAttribArray(0)
                GLES20.glDisableVertexAttribArray(1)
            }
        }
    }

    private val obj = Any()
    private var length = 0f
    fun setData(vertex: FloatArray, indices: IntArray) {
        synchronized(obj){
            length = vertex.last()
            vertexBuffer?.clear()
            if(vertexBuffer == null || vertexBuffer!!.limit() != vertex.size){
                vertexBuffer = vertex.toFloatBuffer()
            }else{
                vertexBuffer?.let {
                    it.put(vertex)
                    it.position(0)
                }
            }
            indicesBuffer?.clear()
            if(indicesBuffer == null || indicesBuffer!!.limit() != indices.size){
                indicesBuffer = indices.toIntBuffer()
            }else{
                indicesBuffer?.let {
                    it.put(indices)
                    it.position(0)
                }
            }
        }

    }
}