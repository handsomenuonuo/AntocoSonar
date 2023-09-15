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
internal class Filter2 {
//    private var csvWriter : CsvWriter = CsvWriter(SonarManager.getActivity()!!)
    private val tempDataArray: SparseArray<FloatArray> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        SparseArray<FloatArray>()
    }

    fun filter(currentDegree:Int,data:Float,orginDegree : Int):Float
    {
//        csvWriter.write(currentDegree,data)
        var result = data
        var array = tempDataArray[currentDegree]
        if(array == null){
            array = FloatArray(5){-1f}
            array[0] = data
            tempDataArray[currentDegree] = array
            return data
        }
//        Log.e("test","滤波前 协议角度 $orginDegree  当前角度 $currentDegree  原始值 $data")
//        array.forEach {
//            Log.e("test","滤波前 当前角度 $currentDegree  原始值 $data   缓存 $it")
//        }

        //过滤0值
        //当前值为0，
        if(data == 0f){
            //若前两个值不为0，则使用前所有值的最小值
            //两个值都为0，则返回0
            result = if(array[0] == 0f && array[1] == 0f){
                0f
            }else{
                //第二个值为-1，则代表才缓存了一个值
                if(array[1] == -1f){
                    array[0]
                }else{
                    //取最小值
                    getMin(array)
                }

            }
        }else{
            //取前5值的平均值
            val ave = getAve(array)
            //平均值为0，代表全是0
            //当前值大于平均值的1.5倍，取最小值
            if(ave != 0f && data >= ave *1.5f){
                result = getMin(array)
            }

        }
        //替换数组的值
        array.copyInto(array,1,0,4)
        array[0] = data

//        array.forEach {
//            Log.e("test","滤波后 当前角度 $currentDegree  返回值 $result  缓存 $it")
//        }
        return result
    }

    private var ddd = -1

    //排序后取不是0和-1的最小值，如果没有返回0
    private fun getMin(array : FloatArray):Float{
        var temp = array.clone()
        temp.sort()
        for(f in temp){
            if(f != -1f && f != 0f)return f
        }
        return 0f
    }

    //排序后取不是0和-1的最小值，如果没有返回0
    private fun getAve(array : FloatArray):Float{
        var sum = 0f
        var count = 0f
        for(f in array){
            if(f ==-1f)break
            if(f !=0f){
                sum += f
                count++
            }
        }
        if(count == 0f)return 0f
        return sum/count
    }
}


object Test{
    @JvmStatic
    fun main(args: Array<String>) {
        var count = 5
        var array = floatArrayOf(
            1f,2f,3f,4f,5f,6f,7f,8f,9f,10f,11f,12f,13f,14f,15f,16f
        )

//        var result = FloatArray(array.size)
//        var temp = FloatArray(array.size + count -1 )
//        array.copyInto(temp,0,array.size-(count shr 1),array.size)
//        array.copyInto(temp,(count shr 1),0,array.size)
//        array.copyInto(temp,(count shr 1) + array.size,0,(count shr 1))
//
//        var sum = 0f
//        for(i in 0 until count){
//            sum += temp[i]
//        }
//        result[0] = sum/count
//        for(i in 1 until result.size){
//            sum -= temp[i-1]
//            sum += temp[i + count -1]
//            result[i] = sum/count
//        }
//
//        result.forEach {
//            println(it)
//        }

        var res = FloatArray(array.size shr 1)
        var min1 = Float.MAX_VALUE
        var min2 = Float.MAX_VALUE
        var min3 = Float.MAX_VALUE
        var max1 = -1f
        var max2 = -1f
        var max3 = -1f
        for(i in res.indices){
            res[i] = array[i] + array[i + res.size]
            if(res[i] < min1){
                min3 = min2
                min2 = min1
                min1 = res[i]
            }else if(res[i] < min2){
                min3 = min2
                min2 = res[i]
            }else if(res[i] < min3){
                min3 = res[i]
            }else if(res[i]>max3){
                max1 = max2
                max2 = max3
                max3 = res[i]
            }else if(res[i]>max2){
                max1 = max2
                max2 = res[i]
            }else if(res[i]>max1){
                max1 = res[i]
            }
        }
        println(min1)
        println(min2)
        println(min3)
        println(max1)
        println(max2)
        println(max3)
    }
}