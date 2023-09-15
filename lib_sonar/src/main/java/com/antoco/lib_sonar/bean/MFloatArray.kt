package com.antoco.lib_sonar.bean

/**********************************
 * @Name:         FloatArrayPool
 * @Copyright：  Antoco
 * @CreateDate： 2023/5/22 15:01
 * @author:      huang feng
 * @Version：    1.0
 * @Describe:
 **********************************/
class MFloatArray(val size : Int) {
    var time = 0L
    var data : FloatArray = FloatArray(size)
    private var next: MFloatArray? = null
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
                next = sPool[size]
                sPool[size] = this
                sPoolSize++
            }
        }
    }

    fun clone(): MFloatArray {
        val result = obtain(size)
        data.copyInto(result.data)
        result.time = time
        return result
    }

    companion object{
        @JvmStatic
        private var sPool: HashMap<Int, MFloatArray?> = HashMap()
        @JvmStatic
        private val sPoolSync = Any()
        @JvmStatic
        private var sPoolSize = 0
        @JvmStatic
        private val FLAG_IN_USE = 1 shl 0
        @JvmStatic
        private val MAX_POOL_SIZE = 500

        fun obtain(size : Int): MFloatArray {
            synchronized(sPoolSync) {
                if (sPool[size] != null) {
                    val m: MFloatArray = sPool[size]!!
                    sPool[size] = m.next
                    m.next = null
                    m.flags = 0 // clear in-use flag
                    sPoolSize--
                    return m
                }
            }
            return MFloatArray(size)
        }

        fun clear(){
            sPool.clear()
            sPoolSize = 0
        }
    }
}