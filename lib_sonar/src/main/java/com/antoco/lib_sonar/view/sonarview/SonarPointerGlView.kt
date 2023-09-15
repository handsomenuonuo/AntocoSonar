package com.antoco.lib_sonar.view.sonarview

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.SparseArray
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.core.util.forEach
import androidx.core.util.size
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.antoco.lib_sonar.R
import com.antoco.lib_sonar.bean.MFloatArray
import com.antoco.lib_sonar.bean.SonarXY
import java.math.MathContext
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin


class SonarPointerGlView : GLSurfaceView,Runnable,ScaleGestureDetector.OnScaleGestureListener {
    private val mContext : Context

    private var mWidth = 0
    private var mHeight = 0

    var isPointerView = false
    private var renderer : SonarRender

    constructor(context:Context):this(context,null)
    constructor(context: Context,attrs : AttributeSet?):super(context,attrs){
        this.mContext = context
        if(attrs != null){
            val attributes = context.obtainStyledAttributes(attrs, R.styleable.SonarGlView)
            isPointerView = attributes.getBoolean(R.styleable.SonarGlView_isPointerView,false)
            attributes.recycle()
        }
        renderer  = SonarRender(context,isPointerView)

        setEGLContextClientVersion(3)
        setEGLConfigChooser(8,8,8,8,16,0)
//        holder.setFormat(PixelFormat.TRANSLUCENT)
//        setZOrderOnTop(true)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        (context as LifecycleOwner).lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if(event == Lifecycle.Event.ON_RESUME){
                    resume()
                }else if(event == Lifecycle.Event.ON_PAUSE){
                    pause()
                }
            }
        })
    }

    //雷达图的缩放
    private var zoom  = 1f
    private val MAX_ZOOM = 2f

    private var thread : Thread?=null

    @Volatile
    private var isStart = false

    private var pause = true

    private val dataQueue = LinkedBlockingQueue<MFloatArray>()

    private var useOffsetXY = false

    private val scaleGestureDetector = ScaleGestureDetector(context,this)


//    fun setIsPointerView(isPointerView : Boolean){
//        this.isPointerView = isPointerView
//    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec)
        mHeight = MeasureSpec.getSize(heightMeasureSpec)
        if(mWidth ==0 && mHeight == 0){
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }else{
            val size = min(mWidth,mHeight)
            setMeasuredDimension(size, size)
        }
    }

    fun setRange(range : Float){
//        if(SonarSpec.range != range){
            SonarSpec.range = range
            renderer.updateBgTexture(zoom)
            requestRender()
//        }
    }

    fun setGain(gain : Int){
//        if(SonarSpec.gain != gain){
            SonarSpec.gain = gain
            renderer.updateBgTexture(zoom)
            requestRender()
//        }
    }

    private fun pause(){
        if(!isStart)return
        if(pause)return
        pause = true
        renderer.stopScan()
    }

    fun stop(){
        if(!isStart)return
        isStart = false
        pause()
        thread?.interrupt()
        thread = null
    }

    fun start(){
        if(isStart && !pause)return
        if(isStart && pause){
            resume()
            return
        }
        isStart = true
        resume()
        thread?.let {
            it.interrupt()
        }
        thread =  Thread(this).apply {
            name = "SonarGlViewThread"
            this.start()
        }

    }

    private fun resume(){
        if(!isStart)return
        if (!pause)return
        pause = false
        dataQueue.clear()
        renderer.startScan()
    }

    fun clear(){
        if(isPointerView){
            drawPointerList.clear()
        }else{
            drawDataArray.clear()
        }
        renderer.clear()
    }

    //这边是两个数组（距离数组和旋转角度数组）合并到一个数组
    fun updateData(distanceArray: MFloatArray){
        if(pause||!isStart){
            distanceArray.recycle()
            return
        }
        dataQueue.add(distanceArray)
    }

    private val PI_M_2_P_360 = (2 * PI / 360 ).toFloat()

    private val drawDataArray:SparseArray<SonarXY> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        SparseArray<SonarXY>()
    }
    private val drawPointerList:MutableList<Float> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        mutableListOf()
    }
    private var renderData = FloatArray(0)
    override fun run() {
        while (isStart){
            try {
               val d =  dataQueue.take()
                //分离和处理数据，计算出xy
                var degree = 0
                degree = d.data.last().toInt()
                if(isPointerView){
                    repeat(6){
                        var cd = degree - it*60
                        if(cd < 0)cd+=360
                        val x = d.data[it] * cos(cd * PI_M_2_P_360) / zoom
                        val y = d.data[it] * sin(cd * PI_M_2_P_360) / zoom

                        drawPointerList.add(x)
                        drawPointerList.add(y)
                        if(drawPointerList.size>1200){
                            drawPointerList.removeFirst()
                            drawPointerList.removeFirst()
                        }
                    }
                    renderData  = drawPointerList.toFloatArray()
                    renderer.updateData(renderData,0f,0f, degree.toFloat())
                }else{
                    repeat(6){
                        var cd = degree - it*60
                        if(cd < 0)cd+=360
                        val x = d.data[it] * cos(cd * PI_M_2_P_360) / zoom
                        val y = d.data[it] * sin(cd * PI_M_2_P_360) / zoom

                        //如果没有存储这个角度的数据，就新建
                        var sonarXY = drawDataArray.get(cd)
                        if(sonarXY == null){
                            sonarXY = SonarXY(x,y)
                            drawDataArray.put(cd,sonarXY)
                        }else{
                            //如果这个角度的数据已经存了，就更新
                            sonarXY.x = x
                            sonarXY.y = y
                        }
                    }
                    if(drawDataArray.size == 0){
                        continue
                    }
                    //优化数组，不必每次都申请
                    if(renderData.size != drawDataArray.size * 2){
                        renderData = FloatArray(drawDataArray.size * 2)
                    }

                    //=======用于计算图形的重心======
                    var index = 0
                    var sumX = 0f
                    var sumY = 0f

                    drawDataArray.forEach { _, value ->
                        renderData[index] = value.x
                        renderData[index+1] = value.y
                        index+=2
                        sumX += value.x
                        sumY += value.y
                    }
                    if(useOffsetXY){
                        val x = sumX / drawDataArray.size
                        val y = sumY / drawDataArray.size
                        renderer.updateData(renderData,-x,-y,degree.toFloat())
                    }else{
                        renderer.updateData(renderData,0f,0f,degree.toFloat())
                    }
                }
                d.recycle()
                requestRender()
                repeat(2){
                    Thread.sleep(20)
                    renderer.smoothAngle()
                    requestRender()
                }
                val size = dataQueue.size
                if(size > 4){
                    Thread.sleep(2)
                }else if(size > 3){
                    Thread.sleep(4)
                }else if(size > 2){
                    Thread.sleep(6)
                }else {
                    Thread.sleep(8)
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val a  = scaleGestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event) or a
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
       val curScale = lastScale/detector.scaleFactor
        if(curScale >= MAX_ZOOM){
            zoom = MAX_ZOOM
        }else if(curScale<0.1f){
            zoom = 0.1f
        }else{
            zoom = curScale
        }
        renderer.updateBgTexture(zoom)
        requestRender()
        return false
    }

    private var lastScale =  zoom
    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        lastScale =  zoom
        pause()
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        zoom = zoom.toBigDecimal(MathContext(2)).toFloat()
        renderer.updateBgTexture(zoom)
        requestRender()
        resume()
        clear()
    }

//    如果是逆时针旋转：
//    x2 = (x1 - x0) * cosa - (y1 - y0) * sina + x0
//    y2 = (y1 - y0) * cosa + (x1 - x0) * sina + y0
//    如果是顺时针旋转：
//    x2 = (x1 - x0) * cosa + (y1 - y0) * sina + x0
//    y2 = (y1 - y0) * cosa - (x1 - x0) * sina + y0

}