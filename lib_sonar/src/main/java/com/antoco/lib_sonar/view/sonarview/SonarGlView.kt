package com.antoco.lib_sonar.view.sonarview

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.SurfaceHolder
import androidx.core.util.forEach
import androidx.core.util.size
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.antoco.lib_sonar.bean.MFloatArray
import com.antoco.lib_sonar.bean.SonarXY
import java.lang.Exception
import java.math.MathContext
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SonarGlView : GLSurfaceView,Runnable,ScaleGestureDetector.OnScaleGestureListener {
    private val mContext : Context

    private var mWidth = 0
    private var mHeight = 0

    constructor(context:Context):this(context,null)
    constructor(context: Context,attrs : AttributeSet?):super(context,attrs){
        this.mContext = context
    }

    private var thread : Thread?=null

    @Volatile
    private var isStart = false

    private var pause = true

    private val dataQueue = LinkedBlockingQueue<MFloatArray>()

    private var useOffsetXY = false

    private var isPointerView = false

    private val renderer : SonarRender = SonarRender(context,isPointerView)

    private val scaleGestureDetector = ScaleGestureDetector(context,this)

    init {
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

    fun setZoom(zoom : Float){
        if(SonarSpec.zoom != zoom){
//            clear()
            SonarSpec.zoom = zoom
            renderer.updateBgTexture()
            requestRender()
        }
    }

    fun setRange(range : Float){
        if(SonarSpec.range != range){
            SonarSpec.range = range
            renderer.updateBgTexture()
            requestRender()
        }
    }

    fun setGain(gain : Int){
        if(SonarSpec.gain != gain){
            SonarSpec.gain = gain
            renderer.updateBgTexture()
            requestRender()
        }
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
            tempDataArray.clear()
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

    private val tempDataArray:SparseArray<Float> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        SparseArray<Float>()
    }
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
                if(isPointerView){
                    repeat(6){
                        var degree = d.data.last().toInt() + 60*it
                        if(degree>= 360 ) degree -= 360
                        val x = d.data[it] * cos(degree * PI_M_2_P_360) / SonarSpec.zoom
                        val y = d.data[it] * sin(degree * PI_M_2_P_360) / SonarSpec.zoom
                        drawPointerList.add(x)
                        drawPointerList.add(y)
                        if(drawPointerList.size>=600){
                            drawPointerList.removeFirst()
                            drawPointerList.removeFirst()
                        }
                        renderData  = drawPointerList.toFloatArray()
                        renderer.updateData(renderData,0f,0f,d.data.last())
                    }
                }else{
                    repeat(6){
                        var degree = d.data.last().toInt() + 60*it
                        if(degree>= 360 ) degree -= 360
                        /*********以下代码用于缓存一定数据，过滤一些突变的0*******************/
                        val dis = tempDataArray.get(degree)
                        if(dis != null){
                            var useDis = if(d.data[it] == 0f) dis else d.data[it]

                            val x = useDis * cos(degree * PI_M_2_P_360) / SonarSpec.zoom
                            val y = useDis * sin(degree * PI_M_2_P_360) / SonarSpec.zoom
                            //如果没有存储这个角度的数据，就新建
                            var sonarXY = drawDataArray.get(degree)
                            if(sonarXY == null){
                                sonarXY = SonarXY(x,y)
                                drawDataArray.put(degree,sonarXY)
                            }else{
                                //如果这个角度的数据已经存了，就更新
                                sonarXY.x = x
                                sonarXY.y = y
                            }
                        }
                        tempDataArray.put(degree,d.data[it])
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
                        renderer.updateData(renderData,-x,-y,d.data.last())
                    }else{
                        renderer.updateData(renderData,0f,0f,d.data.last())
                    }
                }
                d.recycle()
                requestRender()
                repeat(5){
                    Thread.sleep(8)
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
        if(curScale >= SonarSpec.MAX_ZOOM){
            SonarSpec.zoom = SonarSpec.MAX_ZOOM
        }else if(curScale<0.1f){
            SonarSpec.zoom = 0.1f
        }else{
            SonarSpec.zoom = curScale
        }
        renderer.updateBgTexture()
        requestRender()
        return false
    }

    private var lastScale =  SonarSpec.zoom
    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        lastScale =  SonarSpec.zoom
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        SonarSpec.zoom = SonarSpec.zoom.toBigDecimal(MathContext(2)).toFloat()
        renderer.updateBgTexture()
        requestRender()
    }

//    如果是逆时针旋转：
//    x2 = (x1 - x0) * cosa - (y1 - y0) * sina + x0
//    y2 = (y1 - y0) * cosa + (x1 - x0) * sina + y0
//    如果是顺时针旋转：
//    x2 = (x1 - x0) * cosa + (y1 - y0) * sina + x0
//    y2 = (y1 - y0) * cosa - (x1 - x0) * sina + y0

}