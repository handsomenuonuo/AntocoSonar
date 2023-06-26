package com.antoco.lib_sonar.view.sonarview

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal class SonarRender(private val context: Context) : GLSurfaceView.Renderer {

    val bgRender = BgRender(context)
    val dataRender = DataRender(context)

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES30.glClearColor(0f,0f,0f,0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT )
        bgRender.onSurfaceCreated(gl,config)
        dataRender.onSurfaceCreated(gl,config)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES30.glViewport(0,0,width,height)
        bgRender.onSurfaceChanged(gl,width,height)
        dataRender.onSurfaceChanged(gl,width,height)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT )

        bgRender.onDrawFrame(gl)
        dataRender.onDrawFrame(gl)
    }

    fun updateData(src :FloatArray){
        dataRender.updateData(src)
    }

    fun updateData(src :FloatArray,tranX : Float, tranY : Float,angle : Float){
        dataRender.updateData(src,tranX,tranY)
        bgRender.angle = angle
    }

    fun smoothAngle(){
        bgRender.smoothAngle()
    }

    fun updateBgTexture() {
        bgRender.updateTexture()
    }

    fun startScan() {
        bgRender.startScan()
    }

    fun stopScan() {
        bgRender.stopScan()
    }

    fun clear(){
        dataRender.clear()
    }
}