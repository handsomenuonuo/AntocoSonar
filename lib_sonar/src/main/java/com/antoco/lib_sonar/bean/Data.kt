package com.antoco.lib_sonar.bean

import android.util.SparseArray
import java.io.File


enum class WorkState{
    STOP, START,
}

open class BaseData(){
    var srcByteArray : ByteArray? = null
}

class SonarData private constructor() : BaseData() {
    var range: Int = 0//声呐量程 0~6000mm
    var range2M: Int = 0//声呐量程 0~6000mm，转成米
    var workState: WorkState = WorkState.STOP//声呐状态 工作/停止(1/0)
    var gain: Int = 0//声呐增益 声呐增益(范围 0~100，单位%)
//    var samplingTime: Int = 0//每次采样时间
    var degree: Int = 0//当前角度值 0~360(度)
    var perDegree : Int = 0//扫描步距角
    var measureDistance: FloatArray = FloatArray(6)//测距值 0-6000 (mm)
    var measureDistance2M: FloatArray = FloatArray(6)//测距值 0-6000 (mm) 转成米
    var gyro_yaw: Float = 0f//陀螺仪航向角
    var gyro_roll: Float = 0f//陀螺仪横滚角
    var gyro_pitch: Float = 0f//陀螺仪俯仰角
    var voltages: FloatArray = FloatArray(4)//电压 4 路电压
    var waterTemp: String = ""//水体温度(-40~135)

    var useless = false

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
    var y : Float
)


data class PipeXYZ(
    var x : Float,
    var y : Float,
    var z : Float,
)


class PerCircleData private constructor(
    val xyz : SparseArray<PipeXYZ> = SparseArray<PipeXYZ>()
){
    var z = 0f

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