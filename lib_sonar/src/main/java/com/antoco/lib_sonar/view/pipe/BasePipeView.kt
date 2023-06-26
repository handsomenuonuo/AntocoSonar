package com.antoco.lib_sonar.view.pipe

/**********************************
 * @Name:         BasePipeView
 * @Copyright：  Antoco
 * @CreateDate： 2023/6/17 10:57
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:
 **********************************/
interface BasePipeView {
    fun setData(vertex : FloatArray, indices : IntArray,normals : FloatArray?, startPos : Float? , endPos :Float?)
}