package com.zero.jni;

public class NativeMemoryAllocator {
    static {
        System.loadLibrary("memory_stress");
    }

    /**
     * 开始内存压力测试
     *
     * @param blockSizeMB 每个内存块的大小（MB）
     * @param maxBlocks   最大分配块数，0表示无限制
     * @param mode        测试模式：0-标准模式，1-激进模式
     */
    public native void startStressTest(int blockSizeMB, long maxBlocks, int mode);

    /**
     * 停止内存压力测试
     */
    public native void stopStressTest();

    /**
     * 获取已分配的内存大小（MB）
     *
     * @return 已分配的内存大小
     */
    public native long getAllocatedMemoryMB();
}