package com.antoco.lib_sonar.manager

import android.util.SparseArray
import androidx.core.util.forEach
import androidx.core.util.size
import com.antoco.lib_sonar.bean.PerCircleData
import com.antoco.lib_sonar.bean.PipeXYZ
import com.antoco.lib_sonar.bean.MFloatArray
import org.joml.Vector3f
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


/**********************************
 * @Name:         SonarDataManager
 * @Copyright：  Antoco
 * @CreateDate： 2023/5/12 16:11
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:
 **********************************/
internal object Sonar2PipeProcessorManager  {

    //todo 目前先固定每次旋转的角度是6度，因为是6个声呐，那么一圈的数据其实就是一个声呐旋转60度，就是60个点,一个神呐对应的点就是10
    private const val PER_CIRCLE_DATA_COUNT = 60
    private const val PER_CIRCLE_PER_SONAR_DATA_COUNT = 10

    //每圈数据加的z方向的大小，todo 这个应该是声呐每次采集数据的管道长度，暂时写死
    private const val PER_Z = 1f

//    /**满多少圈数后推送数据[pushData]**/
//    private val MIN_PUSH_DATA_COUNT = 10

    /**[list] 缓存最大的圈数据**/
    private const val MAX_CIRCLE_LIST_COUNT = 30

    //圈数据采样率，每隔多少圈采样一次作为绘制数据
    private const val CIRCLE_SIMPLING_RATE = 1

    private const val PI_M_2_P_360 = (2 * PI / 360 ).toFloat()

    //存储解析文件每一圈的数据
    private val list = mutableListOf<PerCircleData>()

    private val dataQueue = LinkedBlockingQueue<MFloatArray>()

    private var decodeThread : Thread?=null

//    private var tempPerCircleData : PerCircleData ? =null

    private var index = 0

    private var onDataListener : ((FloatArray, IntArray, FloatArray,Float,Float) -> Unit?)? = null

    private var lastInterval = -1

    //用于存储和计算每个顶点的法线
    private var vertexMap = HashMap<Int,Vector3f>()

    private var indices = IntArray(0)
    private var vertexArray = FloatArray(0)
    private var normalFloat = FloatArray(0)

    private var totalDistance = 0f
    private var startDistance = 0f
    private var endDistance = 0f

    private val tempCircleData :SparseArray<FloatArray> = SparseArray()

    init {
        decodeThread = thread{
            while (true){
                val data = dataQueue.take()
                val dis1 = data.data[0]
                val dis2 = data.data[1]
                val dis3 = data.data[2]
                val dis4 = data.data[3]
                val dis5 = data.data[4]
                val dis6 = data.data[5]
                val degree = data.data[6].toInt()
                data.recycle()

                //获取当前所在的区间
                val interval = degree/60
                //如果上一次所在的区间是-1，代表第一个数据进来，将上一个区间设为当前区间
                if(lastInterval == -1) lastInterval = interval
                //如果当前区间 跟 上一次区间 不相等，开始记录一次新的圈数据
                if(interval != lastInterval){
                    //记录一次新的数据
                    if(tempCircleData.size() == PER_CIRCLE_PER_SONAR_DATA_COUNT){
                        addOneCircle()
                    }
                    tempCircleData.clear()
                    tempCircleData.put(degree, floatArrayOf(dis1,dis2,dis3,dis4,dis5,dis6))
                }else{
                    //如果上一次区间 和这一次区间相等，则将数据添加到 这一圈
                    tempCircleData.put(degree, floatArrayOf(dis1,dis2,dis3,dis4,dis5,dis6))
                }
                lastInterval = interval
            }
        }
    }

    /**
     * 添加一圈数据
     */
    private fun addOneCircle(){
        if(index % CIRCLE_SIMPLING_RATE == 0){
            val perCircleData = PerCircleData.obtain()
            perCircleData.z = 0.5f
            if(list.size > MAX_CIRCLE_LIST_COUNT){
                val d = list.removeLast()
                d.recycle()
                startDistance += d.z
            }

            //将臂长转化为xyz
            tempCircleData.forEach { key, value ->
                formatXYZ(key,value,perCircleData.z,perCircleData.xyz)
            }
            //计算偏移量
            transXYZ(perCircleData.xyz)
            list.add(0,perCircleData)
            formatData()
        }
        index++
    }

    /**
     * 遍历处理每一圈的数据
     */
    private fun formatData(){
        if(list.size <2)return
        //组装绘制顶点数组
        val verSize = list.size * PER_CIRCLE_DATA_COUNT
        if(vertexArray.size != verSize*5){
            vertexArray = FloatArray(verSize*5)
        }
        var index = 0
        var z = 0f
        vertexMap.clear()
        var listIndex = 0
        list.forEach {
            var verIndex = 0
            val vy = if(listIndex % 2 ==0) 0f else 1f
            it.xyz.forEach { _, value ->
                vertexMap[index/5] = Vector3f(value.x,value.y,z)
                vertexArray[index] = value.x
                index++
                vertexArray[index] = value.y
                index++
                vertexArray[index] = z
                index++

                vertexArray[index] = if(verIndex%2==0) 0f else 1f
                index++
                vertexArray[index] = vy
                index++
                verIndex++
            }
            z -= it.z
            listIndex++
        }

        //组装绘制顺序数组
        val s = list[0].xyz.size
        val size = s*3 *(list.size-1) * 2
        if(indices.size != size){
            indices = IntArray(size)
        }
        index = 0
        for(i in 0 until list.size-1){
            for(j in i*s until (i+1)*s-1){
                indices[index] = j
                index++
                indices[index] = j+1
                index++
                indices[index] = s+j
                index++

                indices[index] = j+1
                index++
                indices[index] = s+j+1
                index++
                indices[index] = s+j
                index++

                if(j == (i+1)*s-2){//将尾部跟首部相连
                    indices[index] = j+1
                    index++
                    indices[index] = i * s
                    index++
                    indices[index] = s+j + 1
                    index++

                    indices[index] =  i*s
                    index++
                    indices[index] =  (i+1)*s
                    index++
                    indices[index] =  s+j+1
                    index++
                }
            }

        }
        val normalArray = formatNormal(vertexArray, indices)

        endDistance = startDistance - vertexArray[vertexArray.size-3]

        pushData(vertexArray, indices,normalArray, startDistance, endDistance)
    }


    /**
     * 计算每个片元的法线，将片元法线与顶点法线相加，获得最终的法线，这样才有真实光照感觉
     */
    private fun formatNormal(vertexArray : FloatArray,indices : IntArray):FloatArray{
        for (i in 0 until indices.size/3){
            val p1 = indices[i*3]
            val x1 = vertexArray[p1 * 5]
            val y1 = vertexArray[p1 * 5+1]
            val z1 = vertexArray[p1 * 5+2]
            val p2 = indices[i*3+1]
            val x2 = vertexArray[p2 * 5]
            val y2 = vertexArray[p2 * 5+1]
            val z2 = vertexArray[p2 * 5+2]
            val p3 = indices[i*3+2]
            val x3 = vertexArray[p3 * 5]
            val y3 = vertexArray[p3 * 5+1]
            val z3 = vertexArray[p3 * 5+2]
            val normal = calNormal(x1,y1,z1,x2,y2,z2,x3,y3,z3)

            val vertex1 = vertexMap[p1]
            val vertex2 = vertexMap[p2]
            val vertex3 = vertexMap[p3]

            vertex1?.add(normal)
            vertex2?.add(normal)
            vertex3?.add(normal)
        }
        if(normalFloat.size != vertexMap.size*3){
            normalFloat = FloatArray(vertexMap.size*3)
        }
        var index = 0
        vertexMap.forEach { (_, u) ->
            normalFloat[index] = -u.x
            index++
            normalFloat[index] = -u.y
            index++
            normalFloat[index] = -u.z
            index++
        }
        return normalFloat
    }


    private val p1 = Vector3f()
    private val p2 = Vector3f()
    private val p3 = Vector3f()
    private val edge1 = Vector3f()
    private val edge2 = Vector3f()
    private val normal = Vector3f()

    /**
     * 计算每个片元的法线
     */
    private fun calNormal(x1:Float,y1:Float,z1:Float,x2:Float,y2:Float,z2:Float,x3:Float,y3:Float,z3:Float):Vector3f{
        p1.x = x1
        p1.y = y1
        p1.z = z1
        p2.x = x2
        p2.y = y2
        p2.z = z2
        p3.x = x3
        p3.y = y3
        p3.z = z3
        // 计算两条边的向量
        edge1.set(p2).sub(p1)
        edge2.set(p2).sub(p3)
        // 计算法线向量（两条边的叉乘）
        normal.set(edge1).cross(edge2)
        // 规范化法线向量
        return normal.normalize()
    }


    private const val pipeR = 3.2f

    /**
     * 将数据转化为XYZ
     */
    private fun formatXYZ(degree : Int, dists: FloatArray, z:Float, l : SparseArray<PipeXYZ>){
        repeat(dists.size){
            var deg = degree + 60*it
            if(deg>= 360 ) deg -= 360
            if(deg in 0 .. 2){
                dists[it] += 0.2f
            }
            val x = dists[it] * cos(deg * PI_M_2_P_360)/ pipeR *5
            val y = dists[it] * sin(deg * PI_M_2_P_360)/ pipeR *5
            if(l[deg]!=null){
                l[deg].x = x
                l[deg].y = y
                l[deg].z = z
            }else{
                l.put(deg, PipeXYZ(x,y,z))
            }
        }
    }

    /**
     * 偏移中心/重心
     */
    private fun transXYZ(l : SparseArray<PipeXYZ>){
        var sumX = 0f
        var sumY = 0f
        l.forEach { _, value ->
            sumX += value.x
            sumY += value.y
        }
        val tx = sumX/l.size
        val ty = sumY/l.size
        l.forEach { _, value ->
            value.x -= tx
            value.y -= ty
        }
    }

    /**
     * 推送数据
     */
    private fun pushData(vertexArray: FloatArray, indices: IntArray,normalArray: FloatArray,startDistance:Float,endDistance : Float) {
        onDataListener?.invoke(vertexArray,indices,normalArray,startDistance,endDistance)
    }

    /**
     * 1、每次重新开始需要寻找第一个点，就是角度 = 0 的点。
     */
    fun analysisData(data: MFloatArray){
        dataQueue.put(data)
    }


    fun clear(){
        index = 0
        list.clear()
        dataQueue.clear()
        tempCircleData.clear()
        vertexMap.clear()
    }


    fun setOnDataListener(onData : ((FloatArray, IntArray ,FloatArray,Float,Float) -> Unit?)?){
        onDataListener = onData
    }

}