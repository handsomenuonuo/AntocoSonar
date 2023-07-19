package com.antoco.lib_sonar.view.gyro

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView.Renderer
import android.opengl.Matrix
import com.antoco.lib_sonar.utils.MGl30Utils
import com.antoco.lib_sonar.utils.toFloatBuffer
import com.antoco.lib_sonar.utils.toIntBuffer
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**********************************
 * @Name:         PipeRenderer
 * @Copyright：  Antoco
 * @CreateDate： 2023/5/10 18:36
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:    陀螺仪图形，用于展示实体的事实方向状态
 **********************************/
internal class GyroRenderer(val context: Context): Renderer {

    private var mProgram = 0
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

    private val obj = Any()

    private var mWidth = 0
    private var mHeight = 0

    private val radius = 5f
    private val segments = 32
    private val length = 10f
    private val verticesArray = FloatArray((segments+1) * 3 * 2 )
    private val flankIndices = IntArray(segments * 6)
    private val bottomIndices = IntArray(segments * 3)
    private val topIndices = IntArray(segments * 3)

    private var uTextColor = 0

    private val color1 = floatArrayOf(1f,0f,0f,1f).toFloatBuffer()
    private val color2 = floatArrayOf(0f,1f,0f,1f).toFloatBuffer()
    private val color3 = floatArrayOf(0f,0f,1f,1f).toFloatBuffer()

    init {
        createData()
//        pointerTextureBuffer = pointVer.toFloatBuffer()
    }

    private fun createData() {
        createVertices()
        createIndices()
    }


    //创建顶点
    //底部 顶部  32个顶点 + 中心点
    private fun createVertices() {
        verticesArray[0] = 0f
        verticesArray[1] = 0f
        verticesArray[2] = length/2
        verticesArray[segments*3+3] = 0f
        verticesArray[segments*3+4] = 0f
        verticesArray[segments*3+5] = -length/2
        for (i in 0 until segments) {
            val angle = (i * 2 * Math.PI / segments).toFloat()
            verticesArray[i * 3 + 3] = (radius * cos(angle.toDouble())).toFloat()
            verticesArray[i * 3 + 4] = (radius * sin(angle.toDouble())).toFloat()
            verticesArray[i * 3 + 5] = length/2

            verticesArray[segments*3 + i * 3 + 6] =  verticesArray[i * 3 + 3]
            verticesArray[segments*3 + i * 3 + 7] = verticesArray[i * 3 + 4]
            verticesArray[segments*3 + i * 3 + 8] = -length/2
        }

        vertexBuffer = verticesArray.toFloatBuffer()
    }

    private fun createIndices(){
        for(i in 0 until  segments){
            bottomIndices[i*3] = 0
            bottomIndices[i*3+1] = i+1
            bottomIndices[i*3+2] = i+2

            topIndices[i*3] = segments +1
            topIndices[i*3+1] = segments +1 + i+1
            topIndices[i*3+2] = segments +1 + i+2

            flankIndices[i * 6] = i+1
            flankIndices[i * 6 + 1] = segments + i + 2
            flankIndices[i * 6 + 2] = segments + i + 3

            flankIndices[i * 6 + 3] = segments + i + 3
            flankIndices[i * 6 + 4] = i + 2
            flankIndices[i * 6 + 5] = i + 1

            if(i == segments-1){
                flankIndices[i * 6] = i+1
                flankIndices[i * 6 + 1] = segments + i + 2
                flankIndices[i * 6 + 2] = segments + 2

                flankIndices[i * 6 + 3] = segments + 2
                flankIndices[i * 6 + 4] = 1
                flankIndices[i * 6 + 5] = i + 1
            }
        }

        bottomIndices[bottomIndices.size-1] = 1
        topIndices[topIndices.size-1] = segments + 2
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.12f,0.156f,0.27f,0f)
        mProgram = MGl30Utils.createProgram(context,"GyroVShader.glsl","GyroFShader.glsl")

        GLES30.glUseProgram(mProgram)
        uTextColor = GLES30.glGetUniformLocation(mProgram, "uTextColor")
        initVAO_VBO_EBO()

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mWidth = width
        mHeight = height
        GLES30.glViewport(0,0,width,height)
        // 启用深度测试
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_TEXTURE_2D)

        val aspectRatio = if(width>height)  1.0f*width/height else 1.0f*height/width
        Matrix.setIdentityM(projectionM, 0); // 初始化投影矩阵
        // 计算矩阵的正交投影
//        Matrix.orthoM(projectionM, 0, -aspectRatio, aspectRatio,-1f, 1f,  -1f, 10f)
        //设置透视投影
        Matrix.frustumM(projectionM, 0, -aspectRatio, aspectRatio, -1f, 1f, 1f, 100f)

    }


    private val vbo = IntArray(1)
    private val vao = IntArray(1)
    private val ebo = IntArray(3)
    private fun initVAO_VBO_EBO(){
        //vao
        //创建vao
        GLES30.glGenVertexArrays(1,vao,0)
        //创建VBO缓冲对象
        GLES30.glGenBuffers(1,vbo,0)
        GLES30.glGenBuffers(3,ebo,0)

        //绑定vao
        GLES30.glBindVertexArray(vao[0])
        //绑定VBO缓冲对象
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vbo[0])
        //给VBO缓冲对象传入顶点数据
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,vertexBuffer!!.limit()*4,vertexBuffer,GLES30.GL_STATIC_DRAW)

        //绑定EBO缓冲对象
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,ebo[0])
        //设置ebo的索引数组
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER,bottomIndices.size*4, bottomIndices.toIntBuffer(),GLES30.GL_STATIC_DRAW)
        vertexBuffer!!.position(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glEnableVertexAttribArray(0)

        //绑定EBO缓冲对象
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,ebo[1])
        //设置ebo的索引数组
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER,topIndices.size*4, topIndices.toIntBuffer(),GLES30.GL_STATIC_DRAW)
        vertexBuffer!!.position(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glEnableVertexAttribArray(0)

        //绑定EBO缓冲对象
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,ebo[2])
        //设置ebo的索引数组
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER,flankIndices.size*4, flankIndices.toIntBuffer(),GLES30.GL_STATIC_DRAW)
        vertexBuffer!!.position(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glEnableVertexAttribArray(0)

        //解绑VBO
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0)
        //解绑VAO
        GLES30.glBindVertexArray(0)
        //解绑ebo
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,0)
    }

    private var angleX = 0f
    private var angleY = 0f
    private var angleZ = 0f
    fun rotate(x :Float,y : Float,z : Float){
        angleX = x
        angleY = y
        angleZ = z
    }

    override fun onDrawFrame(gl: GL10?) {
        synchronized(obj){
            vertexBuffer?.let {
                // 清空颜色缓冲区和深度缓冲区
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT);

                // 使用管道着色器程序
                GLES30.glUseProgram(mProgram)
                val matrixLoc = GLES30.glGetUniformLocation(mProgram, "u_Matrix")
                Matrix.setIdentityM(modelM, 0) // 初始化模型矩阵

                Matrix.rotateM(modelM, 0, angleX,1f, 0f, 0f) // 旋转模型矩阵
                Matrix.rotateM(modelM, 0, angleY,0f, 1f, 0f) // 旋转模型矩阵
                Matrix.rotateM(modelM, 0, angleZ,0f, 0f, 1f) // 旋转模型矩阵
                //设置相机位置
                Matrix.setLookAtM(viewM, 0, 0f, 0f, 15f, 0f, 0f, 0f, 0f, 1f, 0f)
                // 把投影矩阵和模型矩阵相乘，得到最终的变换矩阵
                Matrix.multiplyMM(resultM, 0, projectionM, 0, viewM, 0)
                //计算变换矩阵
                Matrix.multiplyMM(resultM1,0,resultM,0,modelM,0)
                // 输入变换矩阵信息
                GLES30.glUniformMatrix4fv(matrixLoc, 1, false, resultM1, 0)

                //绑定vao
                GLES30.glBindVertexArray(vao[0])

                GLES30.glUniform4fv(uTextColor,1,color1)
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo[0])
                //按照顺序绘制,使用ebo
                GLES30.glDrawElements(GLES30.GL_TRIANGLES,bottomIndices.size,GLES30.GL_UNSIGNED_INT,0)

                GLES30.glUniform4fv(uTextColor,1,color2)
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo[1])
                //按照顺序绘制,使用ebo
                GLES30.glDrawElements(GLES30.GL_TRIANGLES,topIndices.size,GLES30.GL_UNSIGNED_INT,0)

                GLES30.glUniform4fv(uTextColor,1,color3)
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo[2])
                //按照顺序绘制,使用ebo
                GLES30.glDrawElements(GLES30.GL_TRIANGLES,flankIndices.size,GLES30.GL_UNSIGNED_INT,0)

                //解绑VAO
                GLES30.glBindVertexArray(0)
                // 禁用顶点数组
                GLES30.glDisableVertexAttribArray(0)
                GLES30.glDisableVertexAttribArray(1)
            }
        }
    }
}