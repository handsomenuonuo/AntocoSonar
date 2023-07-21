package com.antoco.lib_sonar.socket

import android.annotation.SuppressLint
import android.util.Log
import android.util.SparseArray

/**********************************
 * @Name:         Filter
 * @Copyright：  Antoco
 * @CreateDate： 2023/7/20 10:40
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:
 **********************************/
@SuppressLint("UseSparseArrays")
internal class Filter {

    private val tempCount = 7
    private val percentArray = floatArrayOf(0.3f,0.2f,0.1f,0.1f,0.1f,0.1f,0.1f)

    private val judgeZeroCount = 5
    private val tempDataArray: SparseArray<FloatArray> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        SparseArray<FloatArray>()
    }
    private val zeroCountArray:SparseArray<Int> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        SparseArray<Int>()
    }


    fun filter(currentDegree:Int,data:Float):Float
    {
        var result = data
        if(judgeZeroCount !=0){
            if(zeroCountArray[currentDegree] == null){
                zeroCountArray[currentDegree] = judgeZeroCount
            }
            if(data == 0f){
                val c  =  zeroCountArray[currentDegree] -1
                zeroCountArray[currentDegree] = c
                if(c == 0){
                    //清除数据缓存
                    tempDataArray[currentDegree] = null
                    return 0f
                }
            }else{
                //初始化统计0的次数
                zeroCountArray[currentDegree] = judgeZeroCount
            }
        }

        if(tempCount != 0){
            if(tempDataArray[currentDegree] == null){
                val array = FloatArray(tempCount){
                    data
                }
                tempDataArray[currentDegree] = array
            }else {
                val ta = tempDataArray[currentDegree]
                result = 0f
                for(pi in percentArray.indices){
                    result += percentArray[pi] * ta[pi]
                }
                for (i in tempCount - 1 downTo 1) {
                    ta[i] = ta[i - 1]
                }
                ta[0] = data
            }
        }
        return result
    }
}


//if (data == 0f) {
//    for (t in tempDataArray[currentDegree]) {
//        if (t != 0f) {
//            result = t
//            break
//        }
//    }
//} else {
//    for (t in ta) {
//        if (t != 0f ) {
//            if((data > t * bigPresent)){
////                                Log.e("test", "发现突变值！角度 $currentDegree 现值 $data   原值 $t")
//                result = t
//                break
//            }else{
//                result = t*0.2f + result*0.8f
//                break
//            }
//        }
//    }
//}
//for (i in tempCount - 1 downTo 1) {
//    ta[i] = ta[i - 1]
//}
//ta[0] = data