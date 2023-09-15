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
internal class Filter1 {

    private var csvWriter : CsvWriter = CsvWriter(SonarManager.getActivity()!!)
    private val tempDataArray: SparseArray<Float> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        SparseArray<Float>()
    }

    fun filter(currentDegree:Int,data:Float):Float
    {
        csvWriter.write(currentDegree,data)
        var result = data
        if(tempDataArray[currentDegree] == null){
            tempDataArray[currentDegree] = data
            return -1f
        }
        val ta = tempDataArray[currentDegree]
        var lastDegree = if((currentDegree + 6) < 360) (currentDegree + 6) else ((currentDegree +6)-360)
        val taLast = tempDataArray[lastDegree]
        var nextDegree = if((currentDegree - 6)>=0) (currentDegree - 6) else ((currentDegree -6)+360)
        val taNext = tempDataArray[nextDegree]
        //过滤0值
        if(data == 0f){
            result = if(ta != 0f){
                ta
            } else if(taLast != 0f && taNext != 0f){
                (taLast + taNext) *0.5f
            }else if(taLast!=0f) taLast else taNext
        } //过滤突变值
        else{
            if(ta == 0f){
                result = if(taLast !=0f && taNext !=0f){
                    if(data > taLast*1.2f || data > taNext*1.2f){
                        (taLast + taNext) *0.5f
                    }else if(data < taLast*0.8f || data < taNext*0.8f){
                        (taLast + taNext) *0.5f
                    }else data
                }else{
                    if(taLast!=0f) taLast else taNext
                }
            }else{
                if(data > ta*1.2f ){
                    result = ta
                    result = if(data > taLast*1.2f || data > taNext*1.2f){
                        (taLast + taNext) *0.5f
                    }else if(data < taLast*0.8f || data < taNext*0.8f){
                        (taLast + taNext) *0.5f
                    }else ta
                }else if(data < ta*0.8f){
                    result = ta
                    result =if(data > taLast*1.2f || data > taNext*1.2f){
                        (taLast + taNext) *0.5f
                    }else if(data < taLast*0.8f || data < taNext*0.8f){
                        (taLast + taNext) *0.5f
                    }else ta
                }
            }

        }



//        var lastDegree1 = if((currentDegree - 6)>=0) (currentDegree - 6) else ((currentDegree -6)+360)
//        val lastTa1 = tempDataArray[lastDegree1]
//        if(ta > lastTa1*1.2f || ta < lastTa1 *0.8f){
//            result = lastTa1
//        }
//        var lastDegree2 = if((currentDegree - 12)>=0) (currentDegree - 12) else ((currentDegree -12)+360)
//        val lastTa2 = tempDataArray[lastDegree2]
//
//        result = result*0.5f + lastTa1*0.3f + lastTa2*0.2f

        tempDataArray[currentDegree] = data
        return data
    }
}

//
//class KMeans{
//    fun kMeans(data: List<Double>, k: Int, maxIterations: Int): List<Double>? {
//        // 初始化聚类中心
//        val centroids: MutableList<Double> = ArrayList()
//        for (i in 0 until k) {
//            centroids.add(data[i])
//        }
//        for (iter in 0 until maxIterations) {
//            // 聚类分配
//            val clusters: MutableList<MutableList<Double>> = ArrayList()
//            for (i in 0 until k) {
//                clusters.add(ArrayList())
//            }
//            for (point in data) {
//                val nearestCentroidIndex = findNearestCentroidIndex(point, centroids)
//                clusters[nearestCentroidIndex].add(point)
//            }
//
//            // 更新聚类中心
//            for (i in 0 until k) {
//                val newCentroid = calculateMean(clusters[i])
//                centroids[i] = newCentroid
//            }
//        }
//        return centroids
//    }
//
//    private fun findNearestCentroidIndex(point: Double, centroids: List<Double>): Int {
//        var minDistance = Double.MAX_VALUE
//        var nearestIndex = -1
//        for (i in centroids.indices) {
//            val distance = Math.abs(point - centroids[i])
//            if (distance < minDistance) {
//                minDistance = distance
//                nearestIndex = i
//            }
//        }
//        return nearestIndex
//    }
//
//    private fun calculateMean(data: List<Double>): Double {
//        var sum = 0.0
//        for (point in data) {
//            sum += point
//        }
//        return sum / data.size
//    }
//}