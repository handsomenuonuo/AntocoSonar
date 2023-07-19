package com.antoco.lib_sonar.view.gyro

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.math.min

/**********************************
 * @Name:         PipeView
 * @Copyright：  Antoco
 * @CreateDate： 2023/5/10 18:34
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:
 **********************************/
class GyroView : GLSurfaceView{

    constructor(context: Context):this(context,null)

    constructor(context: Context, attrs: AttributeSet?):super(context,attrs)

    private var myRender = GyroRenderer(context)

    private var pause = true

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8,8,8,8,16,0)
        setRenderer(myRender)
        renderMode = RENDERMODE_CONTINUOUSLY

        (context as LifecycleOwner).lifecycle.addObserver(object : LifecycleEventObserver {
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

    fun rotate(x: Float, y: Float, z: Float){
        myRender.rotate(x,y,z)
    }
}