package com.antoco.lib_sonar.view.sonarview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.SweepGradient
import android.opengl.GLES30
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import com.antoco.lib_sonar.utils.MGl30Utils
import com.antoco.lib_sonar.utils.sp2px
import com.antoco.lib_sonar.utils.toFloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


/***
 * 雷达图背景
 */
internal class BgRender(private val context: Context) : GLSurfaceView.Renderer {

   private var mProgram  : Int = 0
    private var bgTextureHandle : Int = 0
    private var scanTextureHandle : Int = 0
    private var mWidth = 0
    private var mHeight = 0
    private var bgTexture = 0
    private var scanTexture = 0
    private val rectF  = RectF()
    private val paint  = Paint()
    private val circleLinePaint = Paint()
    private val dashLinePaint = Paint()
    private val scanPaint = Paint()
    private val textPaint = Paint()
    private var perR : Float = 0f

    private var centerX  = 0f
    private var centerY = 0f

    private var bgBitmap :Bitmap? = null
    private var scanBitmap :Bitmap? = null

    //顶点坐标和纹理坐标
    private val vertices = floatArrayOf(
        1.0f, -1.0f , 0f ,     1f,1f,
        -1.0f, -1.0f , 0f ,    0f,1f,
        1.0f, 1.0f , 0f ,      1f,0f,
        -1.0f, 1.0f , 0f ,     0f,0f,
    )
    private val texVertices = floatArrayOf(
        1f,1f,
        0f,1f,
        1f,0f,
        0f,0f
    )

    private var gradient : SweepGradient ? =null
    private val verticesBuffer = vertices.toFloatBuffer()
    private var fontMetrics : Paint.FontMetricsInt
    private var textHeight = 0
    private val rect = Rect()
    //是否需要更新纹理
    private var needUpdateTexture = false

    private val mColors = intArrayOf(
        0x55ffffff.toInt(),0x00ffffff
    )
    //扫描线旋转的角度
    var angle = 0f
        set(value) {
            field = if(value >= 360)  value-360f else value
        }

    init {
        paint.style = Paint.Style.FILL
        paint.color = 0xff000000.toInt()
        paint.isAntiAlias = true

        circleLinePaint.style = Paint.Style.STROKE
        circleLinePaint.color = 0x88ffffff.toInt()
        circleLinePaint.isAntiAlias = true
        circleLinePaint.strokeWidth = 1f

        dashLinePaint.style = Paint.Style.STROKE
        dashLinePaint.color = 0x88ffffff.toInt()
        dashLinePaint.isAntiAlias = true
        dashLinePaint.strokeWidth = 1f
        val effects1 = DashPathEffect(floatArrayOf(10f, 10f), 1f)
        dashLinePaint.pathEffect = effects1

        scanPaint.isAntiAlias = true

        textPaint.isAntiAlias = true
        textPaint.color = 0x88ffffff.toInt()
        textPaint.textSize = 16f.sp2px().toFloat()
        fontMetrics = textPaint.fontMetricsInt
        // 1.用FontMetrics对象计算高度
        textHeight = fontMetrics.bottom-fontMetrics.top
    }

    fun smoothAngle(){
        angle+=1
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        mProgram = MGl30Utils.createProgram(context,"BgVShader.glsl","BgFShader.glsl")
        //使用着色器程序
        bgTextureHandle = GLES30.glGetUniformLocation(mProgram, "bgTexture")
        scanTextureHandle = GLES30.glGetUniformLocation(mProgram, "scanTexture")
        GLES30.glUseProgram(mProgram)

    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mWidth = width
        mHeight = height
        perR = mWidth*0.1f
        centerX = mWidth * 0.5f
        centerY = mHeight * 0.5f

        gradient = SweepGradient(centerX,centerY,mColors, floatArrayOf(0.2f,0.3f))
        scanPaint.shader = gradient

        initBgTexture()
        initScanTexture()
        bgTexture = 0
        loadTexture()
    }

    val PI_M_2_P_360 = (2 * PI / 360 ).toFloat()

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
//        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,verticesBuffer.limit()*4,verticesBuffer,GLES30.GL_STATIC_DRAW)
//
////        //ebo
////        //创建ebo
////        GLES30.glGenBuffers(1,ebo,0)
////        //绑定ebo
////        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,ebo[0])
////        //设置ebo的索引数组
////        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER,indicesBuffer!!.limit()*4, indicesBuffer,GLES30.GL_STATIC_DRAW)
//
//        //使用vbo
//        //顶点坐标
////        verticesBuffer.position(0)
//        GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,20,0)
//        //打开使用数据的开关
//        GLES30.glEnableVertexAttribArray(0)
//        //背景纹理坐标
////        verticesBuffer.position(3)
//        GLES30.glVertexAttribPointer(1,2,GLES30.GL_FLOAT,false,20,12)
//        //打开使用数据的开关
//        GLES30.glEnableVertexAttribArray(1)
//
//
//
//        //解绑VBO
//        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0)
//        //解绑VAO
//        GLES30.glBindVertexArray(0)
////        //解绑ebo
////        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,0)
//
//        //关闭
//        GLES30.glDisableVertexAttribArray(0)
//        GLES30.glDisableVertexAttribArray(1)
//    }

    override fun onDrawFrame(gl: GL10?) {
        //使用着色器程序
        GLES30.glUseProgram(mProgram)
        if(needUpdateTexture)loadTexture()

        //顶点坐标
        verticesBuffer.position(0)
        GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,20,verticesBuffer)
        //打开使用数据的开关
        GLES30.glEnableVertexAttribArray(0)
        //背景纹理坐标
        verticesBuffer.position(3)
        GLES30.glVertexAttribPointer(1,2,GLES30.GL_FLOAT,false,20,verticesBuffer)
        //打开使用数据的开关
        GLES30.glEnableVertexAttribArray(1)
        //扫线纹理坐标
        if(isStartScan){
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,scanTexture)
            changeScanVertices()
            val scanBuffer = texVertices.toFloatBuffer()
            GLES30.glVertexAttribPointer(2,2,GLES30.GL_FLOAT,false,0,scanBuffer)
            //打开使用数据的开关
            GLES30.glEnableVertexAttribArray(2)
        }
        //绘制
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        //关闭
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
    }

    //计算矩形旋转后的坐标
    private fun changeScanVertices(){
        texVertices[0]= (0.5f)*cos(angle * PI_M_2_P_360) - (0.5f) * sin(angle * PI_M_2_P_360) + 0.5f
        texVertices[1]= (0.5f)*cos(angle * PI_M_2_P_360) + (0.5f) * sin(angle * PI_M_2_P_360) + 0.5f

        texVertices[2]= (-0.5f)*cos(angle * PI_M_2_P_360) - (0.5f) * sin(angle * PI_M_2_P_360) + 0.5f
        texVertices[3]= (0.5f)*cos(angle * PI_M_2_P_360) + (-0.5f) * sin(angle * PI_M_2_P_360) + 0.5f

        texVertices[4]= (0.5f)*cos(angle * PI_M_2_P_360) - (-0.5f) * sin(angle * PI_M_2_P_360) + 0.5f
        texVertices[5]= (-0.5f)*cos(angle * PI_M_2_P_360) + (0.5f) * sin(angle * PI_M_2_P_360) + 0.5f

        texVertices[6]= (-0.5f)*cos(angle * PI_M_2_P_360) - (-0.5f) * sin(angle * PI_M_2_P_360) + 0.5f
        texVertices[7]= (-0.5f)*cos(angle * PI_M_2_P_360) + (-0.5f) * sin(angle * PI_M_2_P_360) + 0.5f
    }

    private fun initBgTexture(){
        bgBitmap = Bitmap.createBitmap(mWidth,mHeight,Bitmap.Config.ARGB_8888)
        val canvas = Canvas()
        canvas.setBitmap(bgBitmap)
        drawBg(canvas)
    }

    private fun initScanTexture(){
        scanBitmap = Bitmap.createBitmap(mWidth,mHeight,Bitmap.Config.ARGB_8888)
        val canvas = Canvas()
        canvas.setBitmap(scanBitmap)
        drawScan(canvas)
    }

    private fun drawBg(canvas: Canvas){
        canvas.drawColor(0xff1f2845.toInt())
        rectF.bottom = mHeight.toFloat()
        rectF.right = mWidth.toFloat()
        //绘制黑色圆形背景
        canvas.drawOval(rectF,paint)
        var s :String
        textPaint.color = 0x88ffffff.toInt()
        repeat(5){
            //绘制圆环
            canvas.drawCircle(centerX,centerY,perR*(it+1),circleLinePaint)

            //绘制文字
            s = "%.2f".format(SonarSpec.range *0.2f*(it+1))
            //2.用bounds计算宽度
            textPaint.getTextBounds(s, 0, s.length, rect)
            val textWidth = rect.right-rect.left
            canvas.drawText(s,centerX+perR*(it+1)-textWidth-3,centerY - textHeight*0.5f - fontMetrics.top*0.5f,textPaint)
        }
        s = "增益：${SonarSpec.gain} %"
        textPaint.color = 0xcc00ff00.toInt()
        canvas.drawText(s,
            10f,
            textHeight.toFloat(),
            textPaint)
        //绘制虚线
        repeat(6){
            canvas.drawLine(mWidth.toFloat(),centerY,centerX,centerY,dashLinePaint)
            canvas.rotate(60f,centerX,centerY)
        }
    }

    private fun drawScan(canvas: Canvas){
        rectF.right = mWidth.toFloat()
        rectF.bottom = mHeight.toFloat()
//        canvas.drawCircle(centerX,centerY,centerX,scanPaint)
        repeat(6){
            canvas.drawArc(rectF,60f,60f,true,scanPaint)
            canvas.rotate(60f,centerX,centerY)
        }
    }

    private fun loadTexture(){
        try {
            if(bgTexture!=0){
                needUpdateTexture = false
                initBgTexture()
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,bgTexture)
                GLUtils.texSubImage2D(GLES30.GL_TEXTURE_2D,0,0,0,bgBitmap)
            }else{
                val textures = IntArray(2)
                // 创建纹理 指定生成N个纹理（第一个参数指定生成N个纹理），textures数组将负责存储所有纹理的代号。
                GLES30.glGenTextures(2,textures,0)

                //生成第一个纹理
                // 获取textures纹理数组中的第一个纹理
                bgTexture = textures[0]
                // 通知OpenGL将texture纹理绑定到GL10.GL_TEXTURE_2D目标中
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,bgTexture)
                useTexParameter()
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D,0,bgBitmap,0)

                //生成第二个纹理
                // 获取textures纹理数组中的第一个纹理
                scanTexture = textures[1]
                // 通知OpenGL将texture纹理绑定到GL10.GL_TEXTURE_2D目标中
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,scanTexture)
                useTexParameter()
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D,0,scanBitmap,0)
                //修改着色器里的值
                GLES30.glUniform1i(bgTextureHandle, 0)
                GLES30.glUniform1i(scanTextureHandle, 1)
            }
        }catch (e :Exception){
            e.printStackTrace()
        }finally {
            bgBitmap?.recycle()
            scanBitmap?.recycle()
        }
//将纹理单元和纹理对象进行绑定
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bgTexture)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, scanTexture)
    }

    private fun useTexParameter() {
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES30.glTexParameterf(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_NEAREST.toFloat()
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
            GLES32.GL_CLAMP_TO_BORDER.toFloat()
        )
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES30.glTexParameterf(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES32.GL_CLAMP_TO_BORDER.toFloat()
        )
    }

    private var isStartScan = false
    fun updateTexture() {
        needUpdateTexture = true
    }
    fun startScan() {
        isStartScan = true
    }

    fun stopScan(){
        isStartScan = false
    }
}