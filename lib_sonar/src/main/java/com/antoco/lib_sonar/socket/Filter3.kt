package com.antoco.lib_sonar.socket

import android.annotation.SuppressLint
import android.util.Log
import android.util.SparseArray
import com.antoco.lib_sonar.SonarManager
import com.antoco.lib_sonar.utils.CsvWriter


/**********************************
 * @Name:         Filter
 * @Copyright：  Antoco
 * @CreateDate： 2023/7/20 10:40
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:
 **********************************/
@SuppressLint("UseSparseArrays")
internal class Filter3 {
    private val tempDataArray: SparseArray<FloatArray> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        SparseArray<FloatArray>()
    }

    fun filter(currentDegree:Int,data:Float,orginDegree : Int):Float
    {
        var result = data
        var orginArray = tempDataArray[currentDegree]
        if(orginArray == null){
            orginArray = FloatArray(5){0f}
            orginArray[0] = data
            tempDataArray[currentDegree] = orginArray
            return data
        }
        //过滤0值
        //当前值为0，
        if(data == 0f) {
            //横向查找，取不为0的平均值
            result = getAve(orginArray)
            //横向查找为0，就纵向查找
            if(result == 0f){
                //向前找一个
                var degree = currentDegree-3
                if(degree < 0)degree += 360
                var lastArray1 = tempDataArray[degree]
                //向后查找一个
                degree = currentDegree + 3
                if(degree >= 360)degree -= 360
                var nextArray1 = tempDataArray[degree]
                degree = currentDegree -6
                if(degree < 0)degree += 360
                var lastArray2 = tempDataArray[degree]
                degree = currentDegree + 6
                if(degree >= 360)degree -= 360
                var nextArray2 = tempDataArray[degree]
                result = if(lastArray1[0] != 0f) {
                    lastArray1[0]
                }else if(nextArray1[0] !=0f ){
                    nextArray1[0]
                }else if(lastArray2[0] !=0f ){
                    lastArray2[0]
                }else if(nextArray2[0] !=0f ){
                    nextArray2[0]
                }else if(lastArray1[1] != 0f) {
                    lastArray1[1]
                }else if(nextArray1[1] !=0f ){
                    nextArray1[1]
                }else if(lastArray2[1] !=0f ){
                    lastArray2[1]
                }else if(nextArray2[1] !=0f ) {
                    nextArray2[1]
                }else{
                    0f
                }

            }
        }
        orginArray.copyInto(orginArray,1,0,4)
        orginArray[0] = data
        return result
    }


    //排序后取不是0和-1的最小值，如果没有返回0
    private fun getMin(array : FloatArray):Float{
        var temp = array.clone()
        temp.sort()
        for(f in temp){
            if(f != -1f && f != 0f)return f
        }
        return 0f
    }

    //取不是0的平均值

    private fun getAve(array : FloatArray):Float{
        var sum = 0f
        var count = 0f
        for(f in array){
            if(f !=0f){
                sum += f
                count++
            }
        }
        if(count == 0f)return 0f
        return sum/count
    }
}