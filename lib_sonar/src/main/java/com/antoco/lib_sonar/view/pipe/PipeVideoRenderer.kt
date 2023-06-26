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
internal class PipeVideoRenderer(val context: Context): Renderer {

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

    private val whiteColor = floatArrayOf(1f,1f,1f,1f)

    private var vertexBuffer : FloatBuffer? = null
    private var indicesBuffer : IntBuffer? = null
    private var normalBuffer : FloatBuffer? = null
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
        Matrix.setIdentityM(projectionM, 0) // 初始化投影矩阵
        // 计算矩阵的正交投影
//        Matrix.orthoM(projectionM, 0, -aspectRatio, aspectRatio,-1f, 1f,  -1f, 10f)
        //设置透视投影
        Matrix.frustumM(projectionM, 0, -aspectRatio, aspectRatio, -1f, 1f, 1f, 100f)

    }

    var mTextureColor : Int? =null

    private var localEyeLoc = 5f
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

//    private val vbo = IntArray(1)
//    private val vao = IntArray(1)
//    private val ebo = IntArray(1)
//    private fun initVAO_VBO_EBO(){
//        //vao
//        //创建vao
//        GLES30.glGenVertexArrays(1,vao,0)
//        //绑定vao
//        GLES30.glBindVertexArray(vao[0])
//
//        //VBO
//        //创建VBO缓冲对象
//        GLES30.glGenBuffers(1,vbo,0)
//        //绑定VBO缓冲对象
//        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vbo[0])
//        //给VBO缓冲对象传入顶点数据
//        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,vertexBuffer!!.limit()*4,vertexBuffer,GLES30.GL_STATIC_DRAW)
//
//        //ebo
//        //创建ebo
//        GLES30.glGenBuffers(1,ebo,0)
//        //绑定ebo
//        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,ebo[0])
//        //设置ebo的索引数组
//        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER,indicesBuffer!!.limit()*4, indicesBuffer,GLES30.GL_STATIC_DRAW)
//
//        //使用vbo
//        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
//        GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
//
//        //获取到uTextColor在着色器程序中的location
////        mTextureColor = GLES30.glGetUniformLocation(mProgram,"uTextColor")
////        GLES30.glUniform4fv(mTextureColor!!,1,whiteColor,0)
//
//        //打开使用数据的开关
//        GLES30.glEnableVertexAttribArray(0)
//        GLES20.glEnableVertexAttribArray(1)
//
//        //解绑VBO
//        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0)
//        //解绑VAO
//        GLES30.glBindVertexArray(0)
//        //解绑ebo
//        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,0)
//
//        needInitVao = false
//    }

    // 计算纹理宽高比和显示区域宽高比
    var textureAspectRatio = 0f

//    var scale = 1f
    private fun loadTexture(){
        var bitmap :Bitmap?=null
        try {
            bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.shuini)
            textureAspectRatio = (bitmap.width/bitmap.height).toFloat()
//            // 根据宽高比进行适当的缩放和平移
//            scale = if (textureAspectRatio > 1) {
//                1 / textureAspectRatio
//            } else {
//                textureAspectRatio
//            }

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

//                if(needInitVao){
//                    initVAO_VBO_EBO()
//                }

                Matrix.setIdentityM(modelM, 0) // 初始化模型矩阵

                Matrix.rotateM(modelM, 0, angleX,0f, 1f, 0f) // 旋转模型矩阵
                Matrix.rotateM(modelM, 0, angleY,1f, 0f, 0f) // 旋转模型矩阵
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
//            mTextureColor = GLES30.glGetUniformLocation(mProgram,"uTextColor")
//            GLES30.glUniform4fv(mTextureColor!!,1,whiteColor,0)
                vertexBuffer!!.position(0)
                GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
                GLES20.glEnableVertexAttribArray(0)
                vertexBuffer!!.position(3)
                GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer)
                GLES20.glEnableVertexAttribArray(1)

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,texture)

                GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesBuffer!!.limit(), GLES20.GL_UNSIGNED_INT, indicesBuffer)

                /**===================使用缓冲=======================**/
                //绑定vao
//                GLES30.glBindVertexArray(vao[0])
//                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,texture)
//                //按照顺序绘制,使用ebo
//                GLES30.glDrawElements(GLES30.GL_TRIANGLES,indicesBuffer!!.limit(),GLES30.GL_UNSIGNED_INT,0)
//                //解绑VAO
//                GLES30.glBindVertexArray(0)


                // 禁用顶点数组
                GLES20.glDisableVertexAttribArray(0)
                GLES20.glDisableVertexAttribArray(1)
            }
        }
    }

    val obj = Any()
    fun setData(vertex: FloatArray, indices: IntArray,normals : FloatArray) {
        synchronized(obj){
            vertexBuffer = vertex.toFloatBuffer()
            indicesBuffer = indices.toIntBuffer()
            normalBuffer = normals.toFloatBuffer()
        }

    }
}