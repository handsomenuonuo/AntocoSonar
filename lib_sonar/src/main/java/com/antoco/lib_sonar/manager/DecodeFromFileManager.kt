package com.antoco.lib_sonar.manager

import android.util.Log
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.core.util.size
import com.antoco.lib_sonar.bean.PerCircleData1
import com.antoco.lib_sonar.bean.PipeXYZ
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**********************************
 * @Name:         DecodeFromFileManager
 * @Copyright：  Antoco
 * @CreateDate： 2023/6/19 10:05
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:
 **********************************/
class DecodeFromFileManager {
    private val PER_CIRCLE_PER_SONAR_DATA_COUNT = 10
    //存储解析文件每一圈的数据
    private val list = mutableListOf<PerCircleData1>()
    //存储采样后每一圈的xyz的值
    private val perCircleXyzList = mutableListOf<SparseArray<PipeXYZ>>()
    private val PI_M_2_P_360 = (2 * PI / 360 ).toFloat()
    //每圈数据加的z方向的大小，todo 这个应该是声呐每次采集数据的间隔长度，暂时写死
    private val per_z = 1f

    //采样率、每隔多少圈数据采集一次绘图数据
    var simplingRate = 10
    //一共多少个顶点
    private var poiSize = 0
    //采样的圈数
    private var simplingCircleCount = 0

    private var lastDecodeFile : File?= null

    /**
     * 1、解析文件，因为初始角度不一定是0°，为了方便处理，找到第一个最接近0°的数据，作为第一条数据
     * 2、逐步解析第一条之后的数据，每60°作为一圈数据，保存每一圈的数据
     * 3、根据采样间隔筛选需要的圈数据，并转换成XYZ数据，并加上偏移量,就是顶点数据
     * 4、将所有顶点数据加载到一个数组中
     * 5、根据顶点数据，生成对应的绘制顺序数据数组，
     * todo 需要考虑如果每一圈数据的个数不一致怎么办？？？？
     */
    suspend  fun decodeFile(file : File, callback : (FloatArray?, IntArray?, String)->Unit){
        if(lastDecodeFile == file)return
        lastDecodeFile = file
        Log.i("DecodeFromFileManager","解析文件中....")
        list.clear()
        //首先解析文件，读取每一个整圈的数据
        val fileInputStream = FileInputStream(file)
        val inputStream = DataInputStream(fileInputStream)
        val length = file.length()
        Log.i("DecodeFromFileManager","fileSize = $length")
        val count = (length/36).toInt()
        var perCircleData : PerCircleData1? = PerCircleData1()
        var lastInterval = -1

        for(i in 0 until count){
            val time = inputStream.readLong()
            val dis1 = inputStream.readFloat()
            val dis2 = inputStream.readFloat()
            val dis3 = inputStream.readFloat()
            val dis4 = inputStream.readFloat()
            val dis5 = inputStream.readFloat()
            val dis6 = inputStream.readFloat()
            val degree = inputStream.readFloat().toInt()

            //获取当前所在的区间
            val interval = degree/60
            //如果上一次所在的区间是-1，代表第一个数据进来，将上一个区间设为当前区间
            if(lastInterval == -1) lastInterval = interval
            if(interval != lastInterval){
                if(perCircleData?.data?.size() == PER_CIRCLE_PER_SONAR_DATA_COUNT){
                    list.add(perCircleData)
                }
                perCircleData = PerCircleData1()
                perCircleData.data.put(degree, floatArrayOf(dis1,dis2,dis3,dis4,dis5,dis6))
            }else{
                perCircleData?.data?.put(degree, floatArrayOf(dis1,dis2,dis3,dis4,dis5,dis6))
            }
            lastInterval = interval
        }
        inputStream.close()
        fileInputStream.close()

        simplingData(callback)
    }

    suspend fun simplingData(callback : (FloatArray?,IntArray?,String)->Unit){
        if(lastDecodeFile ==null)return
        if(list.size == 0){
            Log.e("PipeViewModel","不满两圈数据，无法绘制管道！")
            callback.invoke(null,null,"不满两圈数据，无法绘制管道！")
            return
        }
        //根据采样率，读取采样圈数据，转换成xyz
        perCircleXyzList.clear()
        poiSize = 0
        simplingCircleCount = 0
        for (i in 0 until list.size step simplingRate){
            val l = SparseArray<PipeXYZ>()
            //将臂长转化为xyz
            list[i].data.forEach { key, value ->
                formatXYZ(key,value,simplingCircleCount*per_z,l)
            }
            simplingCircleCount++
            //计算偏移量
            transXYZ(l)
            //将数据添加到列表
            perCircleXyzList.add(l)
        }

        //将放到一个顶点数组中
        if(simplingCircleCount < 2){
            Log.e("PipeViewModel","不满两圈数据，无法绘制管道！")
            callback.invoke(null,null,"不满两圈数据，无法绘制管道！")
            return
        }
        val vertexArray = FloatArray(poiSize*5)
        var index = 0
        var listIndex = 0
        perCircleXyzList.forEach {
            var verIndex = 0
            val vy = if(listIndex % 2 ==0) 0f else 1f
            it.forEach { d, value ->
                vertexArray[index] = value.x
                index++
                vertexArray[index] = value.y
                index++
                vertexArray[index] = value.z
                index++

                vertexArray[index] = if(verIndex%2==0) 0f else 1f
                index++
                vertexArray[index] = vy
                index++
                verIndex++
            }
            listIndex++
        }

        //组装绘制顺序数组
        val s = perCircleXyzList[0].size
        val indices = IntArray(s*3 *(simplingCircleCount-1) * 2)
        index = 0
        for(i in 0 until simplingCircleCount-1){
            for(j in i*s until (i+1)*s-1){
//                    Log.e("test"," = $j  ${j+1}  ${(i+1)*s+j}")
                indices[index] = j
                index++
                indices[index] = j+1
                index++
                indices[index] = s+j
                index++

                indices[index] = j+ 1
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

                    indices[index] = i*s
                    index++
                    indices[index] =  (i+1)*s
                    index++
                    indices[index] =  s+j+1
                    index++
                }
            }

        }
        callback.invoke(vertexArray,indices,"完成")
    }

    private suspend fun formatXYZ(degree : Int, dists: FloatArray, z:Float, l : SparseArray<PipeXYZ>){
        repeat(dists.size){
            var deg = degree + 60*it
            if(deg>= 360 ) deg -= 360
            val x = dists[it] * cos(deg * PI_M_2_P_360) *4
            val y = dists[it] * sin(deg * PI_M_2_P_360) *4

            l.put(deg, PipeXYZ(x,y,z))
            poiSize ++
        }
    }

    private suspend fun transXYZ(l : SparseArray<PipeXYZ>){
        var sumX = 0f
        var sumY = 0f
        l.forEach { _, value ->
            sumX += value.x
            sumY += value.y
        }
        val tx = sumX/l.size
        val ty = sumY/l.size
        l.forEach { _, value ->
            value.x += tx
            value.y += ty
        }
    }
}