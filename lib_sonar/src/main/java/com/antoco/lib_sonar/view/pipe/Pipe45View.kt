package com.antoco.lib_sonar.view.pipe

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.math.abs
import kotlin.math.min

/**********************************
 * @Name:         PipeView
 * @Copyright：  Antoco
 * @CreateDate： 2023/5/10 18:34
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:
 **********************************/
class Pipe45View : GLSurfaceView, ScaleGestureDetector.OnScaleGestureListener , GestureDetector.OnGestureListener , BasePipeView {

    constructor(context: Context):this(context,null)

    constructor(context: Context, attrs: AttributeSet?):super(context,attrs)

    private var myRender : Pipe45Renderer = Pipe45Renderer(context)

    private val gestureDetector = GestureDetector(context,this)
    private val scaleGestureDetector = ScaleGestureDetector(context,this)

    private var pause = true

    private val canTouch = true

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8,8,8,8,16,0)
        setRenderer(myRender)
        renderMode = RENDERMODE_CONTINUOUSLY

        (context as LifecycleOwner).lifecycle.addObserver(object : LifecycleEventObserver{
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if(event == Lifecycle.Event.ON_RESUME){
                    pause = false
                }else if(event == Lifecycle.Event.ON_PAUSE){
                    pause = true
                }
            }
        })
    }

    var mWidth = 1
    var mHeight = 1
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec)
        mHeight = MeasureSpec.getSize(heightMeasureSpec)
        val size = min(mWidth,mHeight)

        setMeasuredDimension(size, size)
    }


    private var isTranslate  = false
    private var lastX = 0f
    private var lastY = 0f
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(canTouch){
            if(event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_POINTER_DOWN){
                if(abs(event.getX(0)-event.getX(1)) < 200 &&
                    abs(event.getY(0)-event.getY(1)) < 200
                ){//触发旋转
                    Log.e("test","开始旋转")
                    isTranslate = true
                    lastX = event.getX(0)
                    lastY = event.getY(0)
                }
            }
            if(isTranslate){
                if(event.action == MotionEvent.ACTION_UP||event.action == MotionEvent.ACTION_POINTER_UP){
                    isTranslate = false
                    myRender.rotateEnd()
                }else if(event.action == MotionEvent.ACTION_MOVE){
                    var dx = (lastX - event.getX(0))
                    var dy = (lastY - event.getY(0))
                    lastX =  event.getX(0)
                    lastY = event.getY(0)
                    myRender.rotate(dx,dy)
                }
                return super.onTouchEvent(event)
            }else{
                val a  = scaleGestureDetector.onTouchEvent(event)
                val b = if(!scaleGestureDetector.isInProgress){
                    gestureDetector.onTouchEvent(event)
                }else false
                return super.onTouchEvent(event) or a or b
            }
        }else {
            return super.onTouchEvent(event)
        }

    }

    private var curScale //当前的伸缩值
            = 0f
    override fun onScale(detector: ScaleGestureDetector): Boolean {
        curScale = detector.scaleFactor
        myRender.scale(curScale)
        return false
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        myRender.scaleEnd()
    }

    override fun onDown(e: MotionEvent): Boolean {
        return true
    }

    override fun onShowPress(e: MotionEvent) {
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return true
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        myRender.translate(distanceX/mWidth,distanceY/mHeight)
        return true
    }

    override fun onLongPress(e: MotionEvent) {
    }

    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return true
    }

//    fun setData(vertex : FloatArray, indices : IntArray){
//        if(pause)return
//        myRender.setData(vertex,indices)
////        requestRender()
//    }

    override fun setData(
        vertex: FloatArray,
        indices: IntArray,
        normals: FloatArray?,
        startPos: Float?,
        endPos: Float?
    ) {
        if(pause)return
        myRender.setData(vertex,indices)
    }

}