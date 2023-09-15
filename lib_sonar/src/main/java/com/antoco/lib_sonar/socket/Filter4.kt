package com.antoco.lib_sonar.socket

import android.annotation.SuppressLint
import android.util.Log
import android.util.SparseArray
import com.antoco.lib_sonar.SonarManager
import com.antoco.lib_sonar.utils.CsvWriter
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


/**********************************
 * @Name:         Filter
 * @Copyright：  Antoco
 * @CreateDate： 2023/7/20 10:40
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:
 **********************************/
@SuppressLint("UseSparseArrays")
internal class Filter4 {

    private val originList = mutableListOf<FloatArray>()
    private var tempArray = FloatArray(120)
    private var tempRevealArray : FloatArray ?=null
    private var revealArray : FloatArray ?=null
    private var oRevealArray : FloatArray = FloatArray(120)
    private var count = 0

    private var coefList = mutableListOf<Float>()

    private var lastCheckShape = -1 //0是圆 1是方

    private var skip = 1

//    private var obstaclesList = mutableListOf<Float>()
//    private var obstaclesStartDegree = 0
//    private var obstaclesEndDegree = 0

    fun filter(currentDegree: Int, data: Float, callback : (Float,Float)->Unit):Boolean{
        tempArray[currentDegree / 3] = data
        count++
        var frData = data
        var o = frData
        if(lastCheckShape != -1){
            if(tempRevealArray==null)tempRevealArray = FloatArray(120)
            frData =  filterRevealZeroData(currentDegree, data)
            o = frData
            //将数据输出平滑一下
            if(revealArray!=null){
                var sum = frData
                for (i in 1 .. 14){
                    val ti = if(currentDegree / 3 -i < 0)currentDegree / 3 -i+120 else currentDegree / 3 -i
                    val ti1 = if(currentDegree / 3 +i > 119)currentDegree / 3 +i -120 else currentDegree / 3 +i
                    sum += revealArray!![ti]
                    sum += revealArray!![ti1]
                }
                frData = sum/29f
                sum = o
                for (i in 1 .. 4){
                    val ti = if(currentDegree / 3 -i < 0)currentDegree / 3 -i+120 else currentDegree / 3 -i
                    val ti1 = if(currentDegree / 3 +i > 119)currentDegree / 3 +i -120 else currentDegree / 3 +i
                    sum += oRevealArray[ti]
                    sum += oRevealArray[ti1]
                }
                o = sum/9
//                val ti = if(currentDegree / 3 -1 < 0)currentDegree / 3 -1+120 else currentDegree / 3 -1
//                if(frData  > revealArray!![ti]*1.2f){
//                    frData =  revealArray!![ti]*1.05f
//                }else if(frData  < revealArray!![ti]*0.8f){
//                    frData =  revealArray!![ti]*0.95f
//                }
                if(o > frData*0.97f&&o < frData*1.3f){
                    o = frData
                }else if(o > frData*1.5f){
                    o = frData
                }
            }
            tempRevealArray!![currentDegree / 3] = frData
            oRevealArray[currentDegree / 3] = o
//            if(o < frData*0.98f){
//                //故障点
//                if(obstaclesList.size ==0){
//                    obstaclesStartDegree = currentDegree
//                }
//                obstaclesList.add(o)
//            }else{
//                if(obstaclesList.size > 1){
//                    //确认故障点
//                    //如果故障点连续超过2个点，则确定故障点
//                    obstaclesEndDegree = currentDegree
//
//                    Log.e("test","找到故障点  起始角度$obstaclesStartDegree   结束角度$obstaclesEndDegree")
//                }
//                obstaclesList.clear()
//            }
        }

        if (count == 120) {
            count = 0
            if (originList.size == 6) {
                originList.removeLast()
                originList.add(0, tempArray)
                tempArray = FloatArray(120)
                //第一步 过滤0值
                val filterArray = filterZero()
                filterData(filterArray)
                val avgArray5 = getRecentAvg(filterArray, 5)
                var res = fitting(avgArray5)

                //第二步 每25个值取均值
                val avgArray = getRecentAvg(filterArray, 19)

                //第三步 对半相加
                //第四步 去除两次最大最小值
                //第五步 (最大 - 最小)/最小
                var res1 = halfSumAndRemove2MinMaxAndReturnMinMaxRatio(avgArray)
                lastCheckShape = if(res==1 || res1 == 1){
                    Log.e("test","方")
                    1
                }else{
                    Log.e("test","圆")
                    0
                }
            } else {
                originList.add(0, tempArray)
                tempArray = FloatArray(120)
            }

            tempRevealArray?.let {
                revealArray = it
                tempRevealArray = FloatArray(120)
            }

        }
//        if(lastCheckShape == 0){
            callback.invoke(frData,o)
            return if(skip == 0){
                true
            } else {
                skip--
                false
            }

//        }
        return false
    }

    //处理用于展示的原始数据 滤0
    private fun filterRevealZeroData(currentDegree: Int,data:Float):Float{
        var result = data
        if(data == 0f){
            val index = currentDegree / 3
            var sum = 0f
            var count = 0
            //首先横向查找
            for (i in 0..5) {
                val d = originList[i][index]
                if (d != 0f) {
                    sum += d
                    count++
                }
            }
            result = if (sum != 0f) sum / count else 0f
            if (result == 0f) {
                sum = 0f
                count = 0
                var tempIndex = index
                for (i in 1..5){
                    tempIndex = if(index-i<0)index-i+120 else index-i
                    if(tempArray[tempIndex]!=0f){
                        sum+=tempArray[tempIndex]
                        count++
                    }
                }
                result = if (sum != 0f) sum / count else 0f
                if (result == 0f) {
                    //纵向查找第一次
                    result = getRecent4Avg(originList[0], index)

                    if (result == 0f) {
                        //纵向查找第二次
                        result = getRecent4Avg(originList[1], index)

                        if (result == 0f) {
                            //纵向查找第三次
                            result = getRecent4Avg(originList[2], index)
                        }
                    }
                }
            }
        }
        return result
    }

    private fun filterZero(): FloatArray {
        //需要过滤0的数据
        var originArray = originList[0]
        val filterArray = originArray.clone()
        originArray.forEachIndexed { index, fl ->
            if (fl == 0f) {
                var result = 0f
                var sum = 0f
                var count = 0
                //首先横向查找
                for (i in 1..4) {
                    val d = originList[i][index]
                    if (d != 0f) {
                        sum += d
                        count++
                    }
                }
                result = if (sum != 0f) sum / count else 0f
                if (result == 0f) {
                    //纵向查找第一次
                    result = getRecent4Avg(originList[0], index)

                    if (result == 0f) {
                        //纵向查找第二次
                        result = getRecent4Avg(originList[1], index)

                        if (result == 0f) {
                            //纵向查找第三次
                            result = getRecent4Avg(originList[2], index)
                        }
                    }
                }
                filterArray[index] = result
            }
        }
        return filterArray
    }

    private fun filterData(originArray: FloatArray) {
//        val filterArray = originArray.clone()
        for (i in originArray.indices) {
            if (i == 0) {
                if (originArray[0] < originArray[originArray.size - 1] * 0.92f) {
                    originArray[0] = 0.97f * originArray[originArray.size - 1]
                } else if (originArray[0] > originArray[originArray.size - 1] * 1.2f) {
                    originArray[0] = 1.05f * originArray[originArray.size - 1]
                }
            } else {
                if (originArray[i] < originArray[i - 1] * 0.92f) {
                    originArray[i] = 0.97f * originArray[i - 1]
                } else if (originArray[i] > originArray[i - 1] * 1.2f) {
                    originArray[i] = 1.05f * originArray[i - 1]
                }
            }
        }
    }


    private fun getRecent4Avg(array: FloatArray, index: Int): Float {
        var sum = 0f
        var size = array.size
        var count = 0
        var index1 = if (index - 1 < 0) size - 1 + index else index - 1
        if (array[index1] != 0f) {
            sum += array[index1]
            count++
        }
        index1 = if (index - 2 < 0) size - 2 + index else index - 2
        if (array[index1] != 0f) {
            sum += array[index1]
            count++
        }
        index1 = if (index + 1 > size - 1) index + 1 - size else index + 1
        if (array[index1] != 0f) {
            sum += array[index1]
            count++
        }
        index1 = if (index + 2 > size - 1) index + 2 - size else index + 2
        if (array[index1] != 0f) {
            sum += array[index1]
            count++
        }
        return if (sum != 0f) sum / count else 0f
    }

    //count 必须是奇数
    private fun getRecentAvg(array: FloatArray, count: Int): FloatArray {
        var result = FloatArray(array.size)
        var temp = FloatArray(array.size + count - 1)
        array.copyInto(temp, 0, array.size - (count shr 1), array.size)
        array.copyInto(temp, (count shr 1), 0, array.size)
        array.copyInto(temp, (count shr 1) + array.size, 0, (count shr 1))

        var sum = 0f
        for (i in 0 until count) {
            sum += temp[i]
        }
        result[0] = sum / count
        for (i in 1 until result.size) {
            sum -= temp[i - 1]
            sum += temp[i + count - 1]
            result[i] = sum / count
        }
        return result
    }

    private val PI_M_2_P_360 = (2 * PI / 360).toFloat()
    private val fittingList = mutableListOf<Int>()
    private fun fitting(array: FloatArray): Int {
        val xyMap = mutableMapOf<Int, FloatArray>()
        val result = FloatArray(120)
        var calIndex = 0
        array.forEachIndexed { index, fl ->
//            Log.e("test","$fl")
            var currentArray = xyMap[index]
            if (currentArray == null) {
                currentArray = floatArrayOf(
                    fl * cos(index * 3 * PI_M_2_P_360),
                    fl * sin(index * 3 * PI_M_2_P_360)
                )
                xyMap[index] = currentArray
            }
            calIndex = index + 18
            if (calIndex > 119) calIndex -= 120
            var lastXYArray = xyMap[calIndex]
            if (lastXYArray == null) {
                lastXYArray = floatArrayOf(
                    array[calIndex] * cos(calIndex * 3 * PI_M_2_P_360),
                    array[calIndex] * sin(calIndex * 3 * PI_M_2_P_360)
                )
                xyMap[calIndex] = lastXYArray
            }
            calIndex += 18
            if (calIndex > 119) calIndex -= 120
            var lastXYArray1 = xyMap[calIndex]
            if (lastXYArray1 == null) {
                lastXYArray1 = floatArrayOf(
                    array[calIndex] * cos(calIndex * 3 * PI_M_2_P_360),
                    array[calIndex] * sin(calIndex * 3 * PI_M_2_P_360)
                )
                xyMap[calIndex] = lastXYArray1
            }
            val k0 =
                -(lastXYArray[0] - currentArray[0]) / if ((lastXYArray[1] - currentArray[1]) == 0f) 0.001f else (lastXYArray[1] - currentArray[1])
            val a0 = ((lastXYArray[0] + currentArray[0]) * 0.5f)
            val b0 = ((lastXYArray[1] + currentArray[1]) * 0.5f)
            val k1 =
                -(lastXYArray1[0] - lastXYArray[0]) / if ((lastXYArray1[1] - lastXYArray[1]) == 0f) 0.001f else (lastXYArray1[1] - lastXYArray[1])
            val a1 = ((lastXYArray1[0] + lastXYArray[0]) * 0.5f)
            val b1 = ((lastXYArray1[1] + lastXYArray[1]) * 0.5f)

            val k0_k1 = if (k0 - k1 == 0f) 0.001f else k0 - k1

            val x = (k0 * a0 - b0 - k1 * a1 + b1) / k0_k1
            val y = (k1 * b0 - k0 * b1 + k0 * k1 * (a1 - a0)) / -k0_k1


            result[index] = (sqrt((x - currentArray[0]).pow(2) + (y - currentArray[1]).pow(2)))
//                            + sqrt((x -lastXYArray[0]).pow(2) + (y-lastXYArray[1]).pow(2))
//                            + sqrt((x -lastXYArray1[0]).pow(2) + (y-lastXYArray1[1]).pow(2))
//                            )/3f
            Log.e("test","   result[index] = ${   result[index]}")
        }
        val avg = result.average()
        var lessCount = 0
        result.forEach {
//            Log.e("test","value = ${it/avg}")
            if (it / avg in 0.8f..1.2f) lessCount++
        }
//        for(i in 0 .. 119){
//            Log.e("test","${originList[0][i]} ${originList[1][i]} ${originList[2][i]} ${originList[3][i]} ${originList[4][i]}")
//        }

        fittingList.add(0, lessCount)
        if (fittingList.size > 5) fittingList.removeLast()
        val res = fittingList.average().toInt()
        return if (res < 35) {
            1
        } else {
            0
        }
    }

    //    private fun halfSumAndRemove2MinMaxAndReturnMinMaxRatio(array : FloatArray):Float{
//        var res = FloatArray(array.size shr 1)
//
//        for(i in res.indices){
//            res[i] = array[i] + array[i + res.size]
//        }
//        res.sort()
//        val max = res[res.size shr 1]*1.1f
//        val min = res[res.size shr 1]*0.9f
//        var count = 0f
////        Log.e("test","=================================")
//        res.forEach {
////            Log.e("test","$it")
//            if(it > min && it < max)count++
//        }
//
//        return count/res.size
//    }
    private fun halfSumAndRemove2MinMaxAndReturnMinMaxRatio(array: FloatArray): Int {
        var res = FloatArray(array.size shr 1)
        var min1 = Float.MAX_VALUE
        var min2 = Float.MAX_VALUE
        var min3 = Float.MAX_VALUE
        var max1 = -1f
        var max2 = -1f
        var max3 = -1f
        for (i in res.indices) {
            res[i] = array[i] + array[i + res.size]
            if (res[i] < min1) {
                min3 = min2
                min2 = min1
                min1 = res[i]
            } else if (res[i] < min2) {
                min3 = min2
                min2 = res[i]
            } else if (res[i] < min3) {
                min3 = res[i]
            } else if (res[i] > max3) {
                max1 = max2
                max2 = max3
                max3 = res[i]
            } else if (res[i] > max2) {
                max1 = max2
                max2 = res[i]
            } else if (res[i] > max1) {
                max1 = res[i]
            }
        }
//        Log.e("test","=================================")
//        res.forEach {
//            Log.e("test","$it")
//        }
        val r = (max1 - min3) / min3
        coefList.add(0, r)
        if (coefList.size > 5) {
            coefList.removeLast()

        }
        val aveCoef = coefList.average()
        return if(aveCoef > 0.3){
            1
        }else{
            0
        }
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