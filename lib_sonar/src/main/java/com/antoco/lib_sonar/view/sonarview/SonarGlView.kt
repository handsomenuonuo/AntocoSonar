package com.antoco.lib_sonar.view.sonarview

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.core.util.size
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.antoco.lib_sonar.bean.MFloatArray
import com.antoco.lib_sonar.bean.SonarXY
import java.lang.Exception
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SonarGlView : GLSurfaceView,Runnable {
    private val mContext : Context

    private var mWidth = 0
    private var mHeight = 0

    constructor(context:Context):this(context,null)
    constructor(context: Context,attrs : AttributeSet?):super(context,attrs){
        this.mContext = context
    }
    private val renderer : SonarRender = SonarRender(context)

    private var thread : Thread?=null

    @Volatile
    private var isStart = false

    private var pause = true

    private val dataQueue = LinkedBlockingQueue<MFloatArray>()

    private var useOffsetXY = false

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8,8,8,8,16,0)
//        holder.setFormat(PixelFormat.TRANSLUCENT)
//        setZOrderOnTop(true)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY

        (context as LifecycleOwner).lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                resume()
            }

            override fun onPause(owner: LifecycleOwner) {
                super.onPause(owner)
                pause()
            }
        })
    }

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

    fun setRange(range : Int){
        if(SonarSpec.range != range){
            clear()
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
        dataArray.clear()
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

    private val dataArray = SparseArray<SonarXY>()
    private var renderData = FloatArray(0)
    override fun run() {
        while (isStart){
            try {
               val d =  dataQueue.take()
                //分离和处理数据，计算出xy
                repeat(6){
                    var degree = d.data.last().toInt() + 60*it
                    if(degree>= 360 ) degree -= 360
                    val x = d.data[it] * cos(degree * PI_M_2_P_360) / SonarSpec.range
                    val y = d.data[it] * sin(degree * PI_M_2_P_360) / SonarSpec.range
                    //如果没有存储这个角度的数据，就新建
                    var sonarXY = dataArray.get(degree)
                    if(sonarXY == null){
                        sonarXY = SonarXY(x,y)
                        dataArray.put(degree,sonarXY)
                    }else{
                        //如果这个角度的数据已经存了，就更新
                        sonarXY.x = x
                        sonarXY.y = y
                    }
                }
                //优化数组，不必每次都申请
                if(renderData.size != dataArray.size * 2){
                    renderData = FloatArray(dataArray.size * 2)
                }

                //=======用于计算图形的重心======
                var index = 0
                var sumX = 0f
                var sumY = 0f

                dataArray.forEach { _, value ->
                    renderData[index] = value.x
                    renderData[index+1] = value.y
                    index+=2
                    sumX += value.x
                    sumY += value.y
                }

                val x = sumX / dataArray.size
                val y = sumY / dataArray.size
                if(useOffsetXY){
                    renderer.updateData(renderData,-x,-y,d.data.last())
                }else{
                    renderer.updateData(renderData,0f,0f,d.data.last())
                }
                d.recycle()
                requestRender()
                repeat(5){
                    Thread.sleep(10)
                    renderer.smoothAngle()
                    requestRender()
                }
                val size = dataQueue.size
                if(size > 4){
                    Thread.sleep(30)
                }else if(size > 3){
                    Thread.sleep(40)
                }else if(size > 2){
                    Thread.sleep(50)
                }else {
                    Thread.sleep(60)
                }

//                repeat(2){
//                    Thread.sleep(16)
//                    renderer.smoothAngle()
//                    requestRender()
//                }
//                val size = dataQueue.size
//                if(size > 4){
//                    Thread.sleep(10)
//                }else if(size > 3){
//                    Thread.sleep(15)
//                }else if(size > 2){
//                    Thread.sleep(20)
//                }else {
//                    Thread.sleep(25)
//                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

//    如果是逆时针旋转：
//    x2 = (x1 - x0) * cosa - (y1 - y0) * sina + x0
//    y2 = (y1 - y0) * cosa + (x1 - x0) * sina + y0
//    如果是顺时针旋转：
//    x2 = (x1 - x0) * cosa + (y1 - y0) * sina + x0
//    y2 = (y1 - y0) * cosa - (x1 - x0) * sina + y0

}