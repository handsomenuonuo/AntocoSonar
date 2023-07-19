package com.antoco.lib_sonar.view.pipe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.opengl.GLES10
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLES32
import android.opengl.GLSurfaceView.Renderer
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import com.antoco.lib_sonar.R
import com.antoco.lib_sonar.utils.MGl30Utils
import com.antoco.lib_sonar.utils.sp2px
import com.antoco.lib_sonar.utils.toFloatBuffer
import com.antoco.lib_sonar.utils.toIntBuffer
import com.antoco.lib_sonar.view.sonarview.SonarSpec
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.pow
import kotlin.math.sqrt

/**********************************
 * @Name:         PipeRenderer
 * @Copyright：  Antoco
 * @CreateDate： 2023/5/10 18:36
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:
 **********************************/
internal class PipeCenterRenderer(val context: Context): Renderer {

    private var mProgram = 0
    private var textureHandle : Int = 0
    private var useTextureHandle : Int = 0
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

    private val obj = Any()
    private var length = 0f //当前绘制的管道的总长度

    private var pointerTextureBuffer : FloatBuffer
    private val pointVer = floatArrayOf(
        -1f,-1f,0f,  0f,1f,
        1f,-1f,0f,  1f,1f,
        -1f,1f,0f,  0f,0f,

        1f,-1f,0f,  1f,1f,
        -1f,1f,0f,  0f,0f,
        1f,1f,0f,  1f,0f,
    )

    private var angleTextureBuffer : FloatBuffer
    private val angleVer = floatArrayOf(
        -6f,-6f,0f,  0f,1f,
        6f,-6f,0f,  1f,1f,
        -6f,6f,0f,  0f,0f,

        6f,-6f,0f,  1f,1f,
        -6f,6f,0f,  0f,0f,
        6f,6f,0f,  1f,0f,
    )

    private var coordXVerTextureBuffer : FloatBuffer
    private val coordXVer = floatArrayOf(
        -0.5f,0f,0f,
        0.5f,0f,0f,

        0.5f,-0.1f,0f,
        0.5f,0.1f,0f,
        0.68f,0f,0f,

        0f,-0.5f,0f,
        0f,0.5f,0f,

        -0.1f,0.5f,0f,
        0.1f,0.5f,0f,
        0f,0.68f,0f,

        0f,0f,-0.5f,
        0f,0f,0.5f,

        -0.1f,0f,0.5f,
        0.1f,0f,0.5f,
        0f,0f,0.68f,

        //x
        0.78f,-0.1f,0f,
        0.94f,0.1f,0f,
        0.78f,0.1f,0f,
        0.94f,-0.1f,0f,

        //Y
        -0.07f,0.98f,0f,
        0f,0.88f,0f,
        0.07f,0.98f,0f,
        0f,0.88f,0f,
        0f,0.78f,0f,

        //Z
        -0.07f,0f,0.78f,
        0.07f,0f,0.78f,
        -0.07f,0f,0.98f,
        0.07f,0f,0.98f,
    )

    private var angleCoordVer: FloatArray ? = null

    private var startPos = 0f
    private var endPos = 0f
    private var textHeight = 0
    private var mFontMetrics : Paint.FontMetricsInt?=null

    private var angleTextHeight = 0
    private var angleFontMetrics : Paint.FontMetricsInt?=null

    private var mWidth = 0
    private var mHeight = 0


    private var angleBitmap : Bitmap? = null
    private var distanceBitmap : Bitmap? = null

    private val textures = IntArray(3)

    private val anglePaint : Paint by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        Paint().apply {
            isAntiAlias = true
            color = 0xee00ff00.toInt()
            textSize = 12f.sp2px().toFloat()
            angleFontMetrics = fontMetricsInt
            // 1.用FontMetrics对象计算高度
            angleTextHeight = angleFontMetrics!!.bottom-angleFontMetrics!!.top
        }
    }

    private val textPaint : Paint by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        Paint().apply {
            isAntiAlias = true
            color = 0xee00ff00.toInt()
            textSize = 16f.sp2px().toFloat()
            mFontMetrics = fontMetricsInt
            // 1.用FontMetrics对象计算高度
            textHeight = mFontMetrics!!.bottom-mFontMetrics!!.top
        }
    }

    init {
        angleTextureBuffer = angleVer.toFloatBuffer()
        pointerTextureBuffer = pointVer.toFloatBuffer()
        coordXVerTextureBuffer = coordXVer.toFloatBuffer()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.12f,0.156f,0.27f,0f)
        mProgram = MGl30Utils.createProgram(context,"PipeVShader.glsl","PipeFShader.glsl")

        GLES30.glUseProgram(mProgram)
        textureHandle = GLES30.glGetUniformLocation(mProgram, "Texture")
        useTextureHandle = GLES30.glGetUniformLocation(mProgram, "useTexture")
//        initTextTexture()

        initVAO_VBO_EBO()

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mWidth = width
        mHeight = height
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

        initDistanceTexture()
        initAngleTexture()
//        initArrowTexture()
        loadTexture()
    }


    private val vbo = IntArray(3)
    private val vao = IntArray(3)
//    private val ebo = IntArray(1)
private fun initVAO_VBO_EBO(){
    //vao
    //创建vao
    GLES30.glGenVertexArrays(3,vao,0)
    //创建VBO缓冲对象
    GLES30.glGenBuffers(3,vbo,0)

    //绑定vao
    GLES30.glBindVertexArray(vao[0])
    //绑定VBO缓冲对象
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vbo[0])
    //给VBO缓冲对象传入顶点数据
    GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,coordXVerTextureBuffer.limit()*4,coordXVerTextureBuffer,GLES30.GL_STATIC_DRAW)
    GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 12, 0)
    GLES20.glEnableVertexAttribArray(0)
    //解绑VBO
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0)
    //解绑VAO
    GLES30.glBindVertexArray(0)

    //绑定vao
    GLES30.glBindVertexArray(vao[1])
    //绑定VBO缓冲对象
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vbo[1])
    //给VBO缓冲对象传入顶点数据
    GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,pointerTextureBuffer.limit()*4,pointerTextureBuffer,GLES30.GL_STATIC_DRAW)
    pointerTextureBuffer.position(0)
    GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 20, 0)
    GLES20.glEnableVertexAttribArray(0)
    pointerTextureBuffer.position(3)
    GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 20, 12)
    GLES20.glEnableVertexAttribArray(1)
    //解绑VBO
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0)
    //解绑VAO
    GLES30.glBindVertexArray(0)

    //绑定vao
    GLES30.glBindVertexArray(vao[2])
    //绑定VBO缓冲对象
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vbo[2])
    //给VBO缓冲对象传入顶点数据
    GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,angleTextureBuffer.limit()*4,angleTextureBuffer,GLES30.GL_STATIC_DRAW)
    angleTextureBuffer.position(0)
    GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 20, 0)
    GLES20.glEnableVertexAttribArray(0)
    angleTextureBuffer.position(3)
    GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 20, 12)
    GLES20.glEnableVertexAttribArray(1)
    //解绑VBO
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0)
    //解绑VAO
    GLES30.glBindVertexArray(0)
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

    private var locAngleX = 0f
    private var locAngleY = 90f
    private var angleX = locAngleX
    private var angleY = locAngleY
    fun rotate(x :Float,y : Float){
        locAngleX -= x
        angleX = locAngleX
        locAngleY -= y
        angleY = locAngleY
    }

    private fun loadTexture(){
        var bitmap :Bitmap?=null
        try {
            bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.shuini)

            // 创建纹理 指定生成N个纹理（第一个参数指定生成N个纹理），textures数组将负责存储所有纹理的代号。
            GLES30.glGenTextures(3,textures,0)
            //生成第一个纹理
            // 获取textures纹理数组中的第一个纹理
            // 通知OpenGL将texture纹理绑定到GL10.GL_TEXTURE_2D目标中
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,textures[0])
            useTexParameter()
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D,0,bitmap,0)

            // 通知OpenGL将texture纹理绑定到GL10.GL_TEXTURE_2D目标中
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,textures[1])
            useTexParameter()
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D,0,distanceBitmap,0)

            // 通知OpenGL将texture纹理绑定到GL10.GL_TEXTURE_2D目标中
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,textures[2])
            useTexParameter()
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D,0,angleBitmap,0)
            //修改着色器里的值
            GLES30.glUniform1i(textureHandle, 0)
        }catch (e :Exception){
            e.printStackTrace()
        }finally {
//            arrowBitmap?.recycle()
            bitmap?.recycle()
        }
//将纹理单元和纹理对象进行绑定
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0])
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[1])
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[2])
    }

    private var distanceCanvas : Canvas ? =null
    private fun initDistanceTexture() {
        if(distanceBitmap == null){
            distanceBitmap = Bitmap.createBitmap(mWidth,mHeight,Bitmap.Config.ARGB_8888)
            distanceCanvas = Canvas()
            distanceCanvas!!.setBitmap(distanceBitmap)
        }
        drawDistance(distanceCanvas!!)
    }

    private fun drawDistance(canvas: Canvas){
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        val s = "管道位置：${startPos} - ${endPos}"
        textPaint
        canvas.drawText(s,5f,5f+textHeight*0.5f - (mFontMetrics!!.descent + mFontMetrics!!.ascent)*0.5f,textPaint)
    }

    private fun updateDistanceTexture(){
        initDistanceTexture()
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,textures[1])
        GLUtils.texSubImage2D(GLES30.GL_TEXTURE_2D,0,0,0,distanceBitmap)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
    }

    private var angleCanvas : Canvas ? =null
    private fun initAngleTexture() {
        if(angleBitmap == null){
            angleBitmap = Bitmap.createBitmap(mWidth,mHeight,Bitmap.Config.ARGB_8888)
            angleCanvas = Canvas()
            angleCanvas!!.setBitmap(angleBitmap)
        }
        drawAngle(angleCanvas!!)
    }

    private fun drawAngle(canvas: Canvas){
        canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR)
        canvas.save()
        angleCoordVer?.let {
            for(i in it.indices step 3){
                if(i  *2 % 10 == 0){
                    val dis = mWidth/12 * sqrt(it[i].pow(2)+it[i+1].pow(2))
                    anglePaint
                    val ang =  i*2
                    canvas.drawText(ang.toString(),mWidth/2f+dis,mHeight/2f+5f+angleTextHeight*0.5f - (angleFontMetrics!!.descent + angleFontMetrics!!.ascent)*0.5f,anglePaint)
                    canvas.rotate(-30f,mWidth/2f,mHeight/2f)
                }
            }
        }
        canvas.restore()
    }

    private fun updateAngleTexture(){
        initAngleTexture()
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,textures[2])
        GLUtils.texSubImage2D(GLES30.GL_TEXTURE_2D,0,0,0,angleBitmap)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
    }

//    private fun initArrowTexture(){
//        arrowBitmap = Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888)
//        val canvas = Canvas()
//        canvas.setBitmap(arrowBitmap)
//        drawArrow(canvas)
//    }

//    private fun drawArrow(canvas: Canvas){
//        canvas.drawColor(0x00000000)
//        canvas.drawCircle(50f,50f,4f,pointPaint)
//    }

    private fun useTexParameter() {
        GLES30.glTexParameterf(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_LINEAR .toFloat()
        )
        GLES30.glTexParameterf(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_LINEAR .toFloat()
        )
        GLES30.glTexParameterf(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_MIRRORED_REPEAT.toFloat()
        )
        GLES30.glTexParameterf(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES32.GL_MIRRORED_REPEAT.toFloat()
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        synchronized(obj){
            vertexBuffer?.let {
                // 清空颜色缓冲区和深度缓冲区
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT);

                // 使用管道着色器程序
                GLES20.glUseProgram(mProgram)
                val matrixLoc = GLES30.glGetUniformLocation(mProgram, "u_Matrix")
                Matrix.setIdentityM(modelM, 0) // 初始化模型矩阵

                Matrix.translateM(modelM, 0, 0f, 0f, length/2f)
                Matrix.rotateM(modelM, 0, angleX,0f, 1f, 0f) // 旋转模型矩阵
                Matrix.rotateM(modelM, 0, angleY,1f, 0f, 0f) // 旋转模型矩阵
                Matrix.translateM(modelM, 0, 0f, 0f, -length/2f)  // 将矩阵平移回原始位置
                //设置相机位置
                Matrix.setLookAtM(viewM, 0, 0f, 0f, eyeLoc, 0f, 0f, 0f, 0f, 1f, 0f);
                // 把投影矩阵和模型矩阵相乘，得到最终的变换矩阵
                Matrix.multiplyMM(resultM, 0, projectionM, 0, viewM, 0)
                //计算变换矩阵
                Matrix.multiplyMM(resultM1,0,resultM,0,modelM,0)
                // 输入变换矩阵信息
                GLES30.glUniformMatrix4fv(matrixLoc, 1, false, resultM1, 0)


                /**===================不使用缓冲,绘制管道=======================**/
                GLES20.glBindTexture(GLES30.GL_TEXTURE_2D,textures[0])
                //修改着色器里的值
                GLES30.glUniform1i(textureHandle, 0)
                GLES30.glUniform1i(useTextureHandle, 1)
                vertexBuffer!!.position(0)
                GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
                GLES20.glEnableVertexAttribArray(0)
                vertexBuffer!!.position(3)
                GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer)
                GLES20.glEnableVertexAttribArray(1)
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesBuffer!!.limit(), GLES20.GL_UNSIGNED_INT, indicesBuffer)

                //绑定vao
                GLES30.glBindVertexArray(vao[0])
                //绑定vbo
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vbo[0])
                GLES20.glLineWidth(2f)
                GLES30.glUniform1i(useTextureHandle, 0)
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 2, 3)
                GLES20.glDrawArrays(GLES20.GL_LINES, 5, 2)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 7, 3)
                GLES20.glDrawArrays(GLES20.GL_LINES, 10, 2)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 12, 3)
                GLES20.glLineWidth(4f)
                //x
                GLES20.glDrawArrays(GLES20.GL_LINES, 15, 2)
                GLES20.glDrawArrays(GLES20.GL_LINES, 17, 2)
                //Y
                GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 19, 3)
                GLES20.glDrawArrays(GLES20.GL_LINES, 22, 2)
                //Z
                GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 24, 4)
                //解绑VbO
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0)
                //解绑VAO
                GLES30.glBindVertexArray(0)

                /******************绘制管道角度*************************/
                updateAngleTexture()
                //绑定vao
                GLES30.glBindVertexArray(vao[2])
                //绑定vbo
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vbo[2])

                GLES20.glBindTexture(GLES30.GL_TEXTURE_2D,textures[2])
                //修改着色器里的值
                GLES30.glUniform1i(textureHandle, 2)
                GLES30.glUniform1i(useTextureHandle, 1)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,6)
                //解绑VbO
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0)
                // 解绑VAO
                GLES30.glBindVertexArray(0)

                /******************绘制管道距离*************************/
                updateDistanceTexture()
                Matrix.setIdentityM(modelM, 0) // 初始化模型矩阵
                GLES30.glUniformMatrix4fv(matrixLoc, 1, false, modelM, 0)
                //绑定vao
                GLES30.glBindVertexArray(vao[1])
                //绑定vbo
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vbo[1])

                GLES20.glBindTexture(GLES30.GL_TEXTURE_2D,textures[1])
                //修改着色器里的值
                GLES30.glUniform1i(textureHandle, 1)
                GLES30.glUniform1i(useTextureHandle, 1)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,6)
                //解绑VbO
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0)
                // 解绑VAO
                GLES30.glBindVertexArray(0)


                // 禁用顶点数组
                GLES20.glDisableVertexAttribArray(0)
                GLES20.glDisableVertexAttribArray(1)
            }
        }
    }

    fun setData(vertex: FloatArray, indices: IntArray , startPos : Float , endPos :Float){
        this.startPos = startPos
        this.endPos = endPos
        setData(vertex,indices)
    }

    fun setData(vertex: FloatArray, indices: IntArray) {
        synchronized(obj){
            length = vertex[vertex.size-3]
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

            //在这里取出第一圈的数据进行角度纹理的计算和绘制
             //绘制36个角度值，存储一圈的数组 60 *3
            if(angleCoordVer==null){
                angleCoordVer = FloatArray(180)
            }
            //需要根据三维坐标计算纹理的四个坐标？？？怎么计算？？？
            //采用方法一：将三维坐标转为平面坐标，将角度值一起绘制------由于增加了缩放因子，导致管道的显示距离不一致！！！
            vertex.copyInto(angleCoordVer!!,0,0,180)
//            if(angleCoordVerTextureBuffer == null){
//                angleCoordVerTextureBuffer = angleCoordVer!!.toFloatBuffer()
//            }else{
//                angleCoordVerTextureBuffer?.let {
//                    it.put(angleCoordVer!!)
//                    it.position(0)
//                }
//            }

        }

    }
}