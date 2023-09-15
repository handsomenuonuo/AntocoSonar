package com.antoco.lib_sonar.view.pipe

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector

/**********************************
 * @Name:         PipeView
 * @Copyright：  Antoco
 * @CreateDate： 2023/5/10 18:34
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:
 **********************************/
class PipeView : GLSurfaceView, ScaleGestureDetector.OnScaleGestureListener , GestureDetector.OnGestureListener  , BasePipeView{

    constructor(context: Context):this(context,null)

    constructor(context: Context, attrs: AttributeSet?):super(context,attrs)

    private var myRender : PipeRenderer = PipeRenderer(context)

    private val gestureDetector = GestureDetector(context,this)
    private val scaleGestureDetector = ScaleGestureDetector(context,this)

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8,8,8,8,16,0)
        setRenderer(myRender)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    var mWidth = 1
    var mHeight = 1
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = MeasureSpec.getSize(widthMeasureSpec)
        mHeight = MeasureSpec.getSize(heightMeasureSpec)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val a  = scaleGestureDetector.onTouchEvent(event)
        val b = if(!scaleGestureDetector.isInProgress){
            gestureDetector.onTouchEvent(event)
        }else false
        return super.onTouchEvent(event) or a or b
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
        if(isRotate){
            myRender.rotate(distanceX/2f,distanceY/2f)
        }else{
            myRender.translate(distanceX/mWidth,distanceY/mHeight)
        }

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
    private var isRotate = true
    fun setFingerActionIsRotate(isRotate : Boolean){
        this.isRotate = isRotate
    }

    fun rotateX(d: Float){
        myRender.rotateX(d)
    }

    fun rotateY(d: Float){
        myRender.rotateY(d)
    }

    fun rotateZ(d: Float){
        myRender.rotateZ(d)
    }

//    fun setData(vertex : FloatArray, indices : IntArray){
//        myRender.setData(vertex,indices)
//    }

    fun reset() {
        myRender.reset()
    }

    override fun setData(
        vertex: FloatArray,
        indices: IntArray,
        oVerts:MutableList<FloatArray>?, oIndices:MutableList<IntArray>? ,
        normals: FloatArray?,
        startPos: Float?,
        endPos: Float?
    ) {
        myRender.setData(vertex,indices)
    }

}