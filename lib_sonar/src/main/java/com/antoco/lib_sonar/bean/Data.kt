package com.antoco.lib_sonar.bean

import android.util.SparseArray
import java.io.File


enum class WorkState{
    STOP, ROTATION, STATIC
}

open class BaseData(){
    var srcByteArray : ByteArray? = null
}

class SonarData private constructor() : BaseData() {
    var range: Int = 0//声呐量程 0~6000mm
    var range2M: Float = 0f//声呐量程 0~6000mm，转成米
    var workState: WorkState = WorkState.STOP//声呐状态 工作/停止(1/0)
    var gain: Int = 0//声呐增益 声呐增益(范围 0~100，单位%)
//    var samplingTime: Int = 0//每次采样时间
    var degree: Int = 0//当前角度值 0~360(度)
    var perDegree : Int = 0//扫描步距角
    var foMeasureDistance2M: FloatArray = FloatArray(6)//测距值 0-6000 (mm)
    var measureDistance: FloatArray = FloatArray(6)//测距值 0-6000 (mm)
    var fMeasureDistance: FloatArray = FloatArray(6)//测距值 0-6000 (mm)
    var measureDistance2M: FloatArray = FloatArray(6)//测距值 0-6000 (mm) 转成米
    var fMeasureDistance2M: FloatArray = FloatArray(6)//测距值 0-6000 (mm) 转成米
    var gyro_yaw: Float = 0f//陀螺仪航向角
    var gyro_roll: Float = 0f//陀螺仪横滚角
    var gyro_pitch: Float = 0f//陀螺仪俯仰角
    var voltages: Array<String> = Array(4){""}//电压 4 路电压
    var waterTemp: String = ""//水体温度(-40~135)
    var useless = false
    var hasFilter = false

    var filterArray : FloatArray ?= null

    private var next: SonarData? = null
    private var flags = 0

    private fun isInUse(): Boolean {
        return flags and FLAG_IN_USE == FLAG_IN_USE
    }

    fun recycle() {
        check(!isInUse()) {
            ("This message cannot be recycled because it "
                    + "is still in use.")
        }
        filterArray = null
        hasFilter = false
        recycleUnchecked()
    }

    private fun recycleUnchecked() {
        flags = FLAG_IN_USE
        synchronized(sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool
                sPool = this
                sPoolSize++
            }
        }
    }

    fun clone(): SonarData{
        val data = obtain()

        data.range = range
        data.range2M = range2M
        data.workState = workState
        data.gain = gain
        data.degree = degree
        data.perDegree  = perDegree
        data.foMeasureDistance2M = foMeasureDistance2M
        data.measureDistance = measureDistance
        data.fMeasureDistance = fMeasureDistance
        data.measureDistance2M = measureDistance2M
        data.fMeasureDistance2M = fMeasureDistance2M
        data.gyro_yaw = gyro_yaw
        data.gyro_roll = gyro_roll
        data.gyro_pitch = gyro_pitch
        data.voltages = voltages
        data.waterTemp = waterTemp
        data.hasFilter = hasFilter
        filterArray?.let {
            data.filterArray = it.clone()
        }

        return data
    }

    companion object{
        @JvmStatic
        private var sPool: SonarData? = null
        @JvmStatic
        private val sPoolSync = Any()
        @JvmStatic
        private var sPoolSize = 0
        @JvmStatic
        private val FLAG_IN_USE = 1 shl 0
        @JvmStatic
        private val MAX_POOL_SIZE = 50

        fun obtain(): SonarData {
            synchronized(sPoolSync) {
                if (sPool != null) {
                    val m: SonarData = sPool!!
                    sPool = m.next
                    m.next = null
                    m.flags = 0 // clear in-use flag
                    sPoolSize--
                    return m
                }
            }
            return SonarData()
        }
    }
}


data class SonarXY(
    var x : Float,
    var y : Float,
){
    var degree = 0
    var dis = 0f
    var dis1 = 0f
}


data class PipeXYZ(
    var degree: Int,
    var x : Float,
    var y : Float,
    var z : Float,
)


class PerCircleData private constructor(
    val xyz : SparseArray<PipeXYZ> = SparseArray<PipeXYZ>(),
){
    var z = 0f
    val oXyz : SparseArray<PipeXYZ> = SparseArray<PipeXYZ>()
    val obstacleXyz : MutableList<MutableList<PipeXYZ>> = mutableListOf<MutableList<PipeXYZ>>()
    private var next: PerCircleData? = null
    private var flags = 0

    private fun isInUse(): Boolean {
        return flags and FLAG_IN_USE == FLAG_IN_USE
    }

    fun recycle() {
        check(!isInUse()) {
            ("This message cannot be recycled because it "
                    + "is still in use.")
        }
        obstacleXyz.clear()
        recycleUnchecked()
    }

    private fun recycleUnchecked() {
        flags = FLAG_IN_USE
        synchronized(sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool
                sPool = this
                sPoolSize++
            }
        }
    }
    companion object{
        @JvmStatic
        private var sPool: PerCircleData? = null
        @JvmStatic
        private val sPoolSync = Any()
        @JvmStatic
        private var sPoolSize = 0
        @JvmStatic
        private val FLAG_IN_USE = 1 shl 0
        @JvmStatic
        private val MAX_POOL_SIZE = 100

        fun obtain(): PerCircleData {
            synchronized(sPoolSync) {
                if (sPool != null) {
                    val m: PerCircleData = sPool!!
                    sPool = m.next
                    m.next = null
                    m.flags = 0 // clear in-use flag
                    sPoolSize--
                    return m
                }
            }
            return PerCircleData()
        }
    }
}

data class PerCircleData1(
    val data :SparseArray<FloatArray> = SparseArray()
)


class SonarDate(
    var date : String = "",
    val files : MutableList<File> = mutableListOf<File>()
)