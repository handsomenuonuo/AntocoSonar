package com.antoco.lib_sonar.view.sonarview

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal class SonarLineRender(
    private val context: Context,
) : GLSurfaceView.Renderer {

    private val bgRender = BgRender(context)

    private val dataRender = LineRender(context)
    private val dataRender1 = ObstaclesLineRender(context,0xff7a4209.toInt())

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES30.glClearColor(0f,0f,0f,0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT )
        bgRender.onSurfaceCreated(gl,config)
        dataRender.onSurfaceCreated(gl,config)
        dataRender1.onSurfaceCreated(gl,config)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES30.glViewport(0,0,width,height)
        bgRender.onSurfaceChanged(gl,width,height)
        dataRender.onSurfaceChanged(gl,width,height)
        dataRender1.onSurfaceChanged(gl,width,height)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT )

        bgRender.onDrawFrame(gl)
        dataRender.onDrawFrame(gl)
        dataRender1.onDrawFrame(gl)
    }

    fun updateData(src :FloatArray,src1 :MutableList<FloatArray>,tranX : Float, tranY : Float,angle : Float){
        dataRender.updateData(src,tranX,tranY)
        dataRender1.updateData(src1,tranX,tranY)
        bgRender.angle = angle
    }

    fun smoothAngle(){
        bgRender.smoothAngle()
    }

    fun updateBgTexture(zoom:Float) {
        bgRender.updateTexture(zoom)
    }

    fun startScan() {
        bgRender.startScan()
    }

    fun stopScan() {
        bgRender.stopScan()
    }

    fun clear(){
        dataRender.clear()
        dataRender1.clear()
    }
}