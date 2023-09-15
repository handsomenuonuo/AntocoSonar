package com.antoco.lib_sonar.socket

import android.annotation.SuppressLint
import android.util.Log
import android.util.SparseArray
import androidx.core.util.forEach
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
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
internal class Filter5 {

    private val originList = mutableListOf<FloatArray>()
    private var tempArray = FloatArray(120)
    private var tempRevealArray : FloatArray ?=null
    private var revealArray : FloatArray ?=null
    private var oRevealArray : FloatArray = FloatArray(120)
    private var count = 0

    private var coefList = mutableListOf<Float>()

    private var lastCheckShape = -1 //0是圆 1是方

    private var skip = 1

    private var fangTempArray = SparseArray<Float>()
    private var fangArray = FloatArray(120)
    private var lastFilterData :FloatArray ?=null
    private var rectRes :FloatArray?=null

    interface FilterListener {
        fun onFilterData(filterData:Float,originData:Float)
        fun onFilterArray(filterData:FloatArray)
    }

//    fun filter(degree: Int, data: FloatArray, filterListener : FilterListener){
//        data.forEachIndexed { index, fl ->
//            var d = degree - index*60
//            if(d < 0)d += 360
//            filter(d,fl,filterListener)
//        }
//    }


    fun filter(currentDegree: Int, data: Float, filterListener : FilterListener){
        tempArray[currentDegree / 3] = data
        count++
        var frData = data
        var o = frData
        if(lastCheckShape != -1){
            //以下代码用于输出用于显示的值
            if(lastCheckShape == 0){
                if(tempRevealArray==null)tempRevealArray = FloatArray(120)
                frData =  filterRevealZeroData(currentDegree, data)
                o = frData
                tempRevealArray!![currentDegree / 3] = frData
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
                    if(o > frData*0.97f&&o < frData*1.3f){
                        o = frData
                    }else if(o > frData*1.5f){
                        o = frData
                    }
                }
                tempRevealArray!![currentDegree / 3] = frData
                oRevealArray[currentDegree / 3] = o
            }else {
                frData =  filterRevealZeroData(currentDegree, data)
                fangTempArray[currentDegree / 3] = frData
                if(lastFilterData!=null) {
                    frData = lastFilterData!![currentDegree / 3]
                    o = frData
                }
                rectRes?.let {
                    frData = it[currentDegree / 3]
                    if(o > frData*0.97f&&o < frData*1.3f){
                        o = frData
                    }else if(o > frData*1.5f){
                        o = frData
                    }
                }
                if(fangTempArray.size() == 120){
                    fangTempArray.forEach { key, value ->
                        fangArray[key] = value
                    }
                    fangTempArray = SparseArray()

                    val tempArray13_1 = getRecentAvg(fangArray,13)
                    val tempArray13_2 = getRecentAvg(tempArray13_1,13)
                    fangArray.forEachIndexed { index, fl ->
                        if(fl > tempArray13_2[index] * 1.1f){
                            fangArray[index] = tempArray13_2[index]
                        }
                    }
                    lastFilterData = fangArray.clone()
                    //方形图形拟合
                    //n个值平均
                    val avgArray9 = getRecentAvg(fangArray, 13)
                    val avgArray9_1 = getRecentAvg(avgArray9, 13)
                    val avgArray9_2 = getRecentAvg(avgArray9_1, 13)
                    val avgArray9_3 = getRecentAvg(avgArray9_2, 13)
                    //对半相加
                    val halfArray = FloatArray(64)
                    halfArray[0] = avgArray9_3[58]+avgArray9_3[118]
                    halfArray[1] = avgArray9_3[59]+avgArray9_3[119]
                    for(i in 2..61){
                        halfArray[i] = avgArray9_3[i-2] + avgArray9_3[i + 58]
                    }
                    halfArray[62] = avgArray9_3[0]+avgArray9_3[60]
                    halfArray[63] = avgArray9_3[1]+avgArray9_3[61]
                    //寻找极值
                    val jiList = mutableListOf<Int>()
                    for(i in 2..61){
                        if(halfArray[i] < halfArray[i-1]
                            && halfArray[i-1] < halfArray[i-2]
                            && halfArray[i] < halfArray[i+1]
                            && halfArray[i+1] < halfArray[i+2] ){
                            jiList.add(i-2)
                        }
                    }
//
                    var fInd = 0
                    var sInd = 0
//                    var minL = 1000
                    for(i in jiList.indices){
                        Log.e("test","找到极值   ${jiList[i]}")
//                        var c = jiList.size-1
//                        while (c!=i){
//                            val m = abs(30 - abs(jiList[c]-jiList[i]))
//                            if(m< minL){
//                                minL = m
//                                fInd = jiList[i]
//                                sInd = jiList[c]
//                            }
//                            c--
//                        }
                    }
                    if(jiList.size > 0){
                        fInd = jiList[0]
                        if(fInd - 30 < 0){
                            sInd = fInd+30
                        }else{
                            sInd = fInd
                            fInd = sInd-30
                        }
                        Log.e("test","找到最接近30的索引   $fInd  $sInd")
//                    val a = 30-sInd+fInd
//                    if(a != 30){
//                        //寻找间隔30的两个值
//
//                    }
                        rectRes = FloatArray(120)
                        val array = FloatArray(sInd-fInd+1)
                        avgArray9.copyInto(array,0,fInd,sInd+1)
                        val res = deduce(array)
                        res.copyInto(rectRes!!,fInd,0,res.lastIndex)

                        val array1 = FloatArray(fInd+60-sInd+1)
                        avgArray9.copyInto(array1,0,sInd,fInd+60+1)
                        val res1 = deduce(array1)
                        res1.copyInto(rectRes!!,sInd,0,res1.lastIndex)

                        val array2 = FloatArray(sInd-fInd+1)
                        avgArray9.copyInto(array2,0,fInd+60,sInd+60+1)
                        val res2 = deduce(array2)
                        res2.copyInto(rectRes!!,fInd+60,0,res2.lastIndex)

                        val array3 = FloatArray(fInd + avgArray9.size -sInd-60+1)
                        avgArray9.copyInto(array3,0,sInd+60,avgArray9.size)
                        avgArray9.copyInto(array3,avgArray9.size -sInd-60,0,fInd+1)
                        val res3 = deduce(array3)
                        res3.copyInto(rectRes!!,sInd+60,0,60-sInd)
                        res3.copyInto(rectRes!!,0,60-sInd,res3.lastIndex+1)

                        filterListener.onFilterArray(rectRes!!)
                    }
                }
            }
        }

        //以下代码用于计算图形的形状
        if (count == 120) {
            count = 0
            if (originList.size == 6) {
                originList.removeLast()
                originList.add(0, tempArray)
                tempArray = FloatArray(120)

                if(lastCheckShape==-1){
                    //过滤0值
                    val filterArray = filterZero()
                    //过滤突变值
                    filterData(filterArray)

                    //取均值
                    val avgArray5 = getRecentAvg(filterArray, 5)
                    //解方程法求图形
                    var res = fitting(avgArray5)

                    //取均值
//                val avgArray = getRecentAvg(filterArray, 19)
//                //对半相加求求图形
//                var res1 = halfSumAndRemove2MinMaxAndReturnMinMaxRatio(avgArray)

                    lastCheckShape = when (res) {
                        1 -> {
                            Log.e("test","方")
                            1
                        }
                        0 -> {
                            Log.e("test","圆")
                            0
                        }
                        else -> {
                            when (lastCheckShape) {
                                1 -> {
                                    Log.e("test","方")
                                }
                                0 -> {
                                    Log.e("test","圆")
                                }
                            }
                            lastCheckShape
                        }
                    }
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
         filterListener.onFilterData(frData,o,)
//            return if(skip == 0){//这一步的操作主要是因为第一次会出现0值
//                true
//            } else {
//                skip--
//                false
//            }

//        return false
    }

    private fun deduce(array : FloatArray):FloatArray{
        val res = FloatArray(array.size)
        res[0] = array[0]
        res[res.lastIndex] = array[res.lastIndex]
        var start = 1
        var end = array.size-2
        while (start != end){
            var startD = (array[0] / cos(start*3f/180f * PI)).toFloat()
            var endD = (array[res.lastIndex] / cos((array.size-1-end)*3f/180f * PI)).toFloat()
            if(startD<endD){
                res[start] = startD
                start++
            }else{
                res[end] = endD
                end--
            }
        }
        if(res[start-1] < res[start+1]){
            res[start] = (res[0] / cos(start*3f/180f * PI)).toFloat()
        }else{
            res[end] = (res[res.lastIndex] / cos((array.size-1-end)*3f/180f * PI)).toFloat()
        }
        return res
    }

//    private fun deduce(array : FloatArray):FloatArray{
//        val res = IntArray(array.size)
//        var f = 1
//        var l = array.size-2
//        while (f!=l){
//            if(array[f] < array[l]){
//                res[f]=res[f-1]+3
//                f++
//            }else{
//                res[l]=res[l+1]-3
//                l--
//            }
//        }
//        if(res[f-1]+res[l+1] < 0){
//            res[f]=res[f-1]+3
//        }else{
//            res[l]=res[l+1]-3
//        }
//        val result = FloatArray(array.size)
//        result[0] = array[0]
//        result[array.size-1] = array[array.size-1]
//        for (i in 1..array.size-2){
//            if(res[i] >0){
//                result[i] =(result[0] / cos(res[i]/180f * PI)).toFloat()
//            }else {
//                result[i] =(result[array.size-1] / cos(res[i]/180f * PI)).toFloat()
//            }
//        }
//        return result
//    }

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

                            if (result == 0f) {
                                //纵向查找第三次
                                result = getRecent4Avg(originList[3], index)
                            }
                        }
                    }
                }
            }
        }
        return result
    }

    //过滤0
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

    //过滤突变值
    private fun filterData(originArray: FloatArray) {
//        val filterArray = originArray.clone()
        val min =  0.92f
        val minPer = 0.97f
        val max = 1.2f
        val maxPer = 1.05f
        for (i in originArray.indices) {
            if (i == 0) {
                if (originArray[0] < originArray[originArray.size - 1] * min) {
                    originArray[0] = minPer * originArray[originArray.size - 1]
                } else if (originArray[0] > originArray[originArray.size - 1] * max) {
                    originArray[0] = maxPer * originArray[originArray.size - 1]
                }
            } else {
                if (originArray[i] < originArray[i - 1] * min) {
                    originArray[i] = minPer * originArray[i - 1]
                } else if (originArray[i] > originArray[i - 1] * max) {
                    originArray[i] = maxPer * originArray[i - 1]
                }
            }
        }
    }
    private fun filterData1(originArray: FloatArray) {
//        val filterArray = originArray.clone()
        val min =  0.5f
        val max = 2f
        for (i in originArray.indices) {
            if (originArray[0] < originArray[originArray.size - 1] * min) {
                originArray[0] = originArray[originArray.size - 1]
            } else if (originArray[0] > originArray[originArray.size - 1] * max) {
                originArray[0] = originArray[originArray.size - 1]
            }
        }
    }


    //获取4个平均值，返回值
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
    //用于求解范围内的平均值
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
            //每隔固定个数取三个值，进行相对圆心xy的求解
            var currentArray = xyMap[index]
            if (currentArray == null) {
                currentArray = floatArrayOf(
                    fl * cos(index * 3 * PI_M_2_P_360),
                    fl * sin(index * 3 * PI_M_2_P_360)
                )
                xyMap[index] = currentArray
            }
            //去第二个值
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
            //取第三个值
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
            //解方程求解xy
            //参数的计算
            val k0 =
                -(lastXYArray[0] - currentArray[0]) / if ((lastXYArray[1] - currentArray[1]) == 0f) 0.001f else (lastXYArray[1] - currentArray[1])
            val a0 = ((lastXYArray[0] + currentArray[0]) * 0.5f)
            val b0 = ((lastXYArray[1] + currentArray[1]) * 0.5f)
            val k1 =
                -(lastXYArray1[0] - lastXYArray[0]) / if ((lastXYArray1[1] - lastXYArray[1]) == 0f) 0.001f else (lastXYArray1[1] - lastXYArray[1])
            val a1 = ((lastXYArray1[0] + lastXYArray[0]) * 0.5f)
            val b1 = ((lastXYArray1[1] + lastXYArray[1]) * 0.5f)

            val k0_k1 = if (k0 - k1 == 0f) 0.001f else k0 - k1
            //求解得出xy的值
            val x = (k0 * a0 - b0 - k1 * a1 + b1) / k0_k1
            val y = (k1 * b0 - k0 * b1 + k0 * k1 * (a1 - a0)) / -k0_k1

            //根据xy求解所有半径
            result[index] = (sqrt((x - currentArray[0]).pow(2) + (y - currentArray[1]).pow(2)))
        }
        //求解所有半径的平均值
        val avg = result.average()
        var sum = 0f
        var count = 0
        //计算出所有小于平均值的个数和总和，用于求解平均值
        result.forEach {
            if(it < avg){
                sum+=it
                count++
            }
        }
        //计算所有小于平均值的半径的平均值
        val r = sum/count

        var lessCount = 0
        //计算在在范围内的半径的个数
        result.forEach {
//            Log.e("test","value = ${it/avg}")
            if (it / r in 0.95f..1.05f) lessCount++
        }

        //滑窗，平滑结果数据
        fittingList.add(0, lessCount)
        if (fittingList.size > 5) fittingList.removeLast()
        val res = fittingList.average().toInt()
        Log.e("test","$res")
        return if (res < 25) {
            1
        } else if(res > 30){
            0
        }else{
            2
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