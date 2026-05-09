#include <jni.h>
#include <stdlib.h>
#include <unistd.h>
#include <vector>
#include <atomic>
#include <thread>
#include <android/log.h>
#include <cstring>
#include <fstream>
#include <string>
#include <algorithm> // 添加algorithm头文件用于std::min
#include <utility>

#define LOG_TAG "OOMTestNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// 存储分配的内存块（指针 + 大小）
static std::vector<std::pair<void *, size_t>> g_memory_blocks;
static std::atomic<bool> g_is_running{false};
static std::atomic<long long> g_allocated_bytes{0}; // 按字节计数

// 获取当前系统可用内存的函数声明
static size_t getAvailableMemory();

extern "C" {

JNIEXPORT void JNICALL
Java_com_zero_jni_NativeMemoryAllocator_startStressTest(JNIEnv *env, jobject thiz,
                                                                  jint blockSizeMB, jlong maxBlocks,
                                                                  jint mode) {
    g_is_running = true;
    long allocated_blocks = 0;
    int consecutive_failures = 0; // 连续分配失败次数
    const int max_consecutive_failures = 5; // 最大连续失败次数

    // 初始块大小
    int current_block_size_mb = blockSizeMB;

    // 模式：0-标准模式，1-激进模式
    bool aggressive_mode = (mode == 1);

    // 恢复期计数器
    int recovery_period = 0;

    LOGI("开始内存压力测试，初始块大小：%d MB，最大块数：%lld，模式：%s",
         current_block_size_mb, (long long) maxBlocks, aggressive_mode ? "激进" : "标准");

    while (g_is_running && (maxBlocks <= 0 || allocated_blocks < maxBlocks)) {
        // 检查当前可用内存
        size_t available_memory = getAvailableMemory();

        // 检查停止标志
        if (!g_is_running) {
            LOGI("检测到停止标志，退出压力测试循环");
            break;
        }

        if (aggressive_mode) {
            // 激进模式：但仍设置一个最低保护阈值（避免立刻杀掉自身）
            LOGI("激进模式：系统可用内存：%zu MB", available_memory / (1024 * 1024));
            if (available_memory < 200 * 1024 * 1024) { // 如果可用内存小于200MB，短暂暂停并重试
                LOGW("激进模式下可用内存过低(%zu MB)，短暂暂停", available_memory / (1024 * 1024));
                usleep(200000);
                continue;
            }
        } else {
            // 标准模式：保持系统剩余内存至少 ~50MB，避免应用崩溃
            if (available_memory < 50 * 1024 * 1024) { // 小于50MB
                LOGI("标准模式：系统内存极低(%zu MB)，暂停分配", available_memory / (1024 * 1024));
                usleep(100000); // 暂停100ms
                continue;
            }

            // 动态调整块大小以维持系统稳定性
            if (available_memory < 80 * 1024 * 1024) { // 小于80MB
                current_block_size_mb = 1; // 设置为1MB
            } else if (available_memory < 150 * 1024 * 1024) { // 小于150MB
                current_block_size_mb = 2; // 设置为2MB
            } else if (available_memory < 300 * 1024 * 1024) { // 小于300MB
                current_block_size_mb = 5; // 设置为5MB
            } else {
                current_block_size_mb = blockSizeMB; // 恢复原始大小
            }
        }

        // 检查停止标志
        if (!g_is_running) {
            LOGI("检测到停止标志，退出压力测试循环");
            break;
        }

        size_t block_size = (size_t) current_block_size_mb * 1024 * 1024; // 转换为字节

        // 在任何时候都检查是否有足够的内存进行分配（标准模式更保守）
        if (!aggressive_mode && available_memory < block_size * 3) {
            LOGI("标准模式：内存不足，跳过本次分配，当前可用内存：%zu MB，需要：%zu MB",
                 available_memory / (1024 * 1024), block_size / (1024 * 1024));
            usleep(50000); // 等待50ms

            // 检查停止标志
            if (!g_is_running) {
                LOGI("检测到停止标志，退出压力测试循环");
                break;
            }
            continue;
        }

        void *memory_block = malloc(block_size);
        if (memory_block != nullptr) {
            // 填充数据以确保内存被实际使用
            memset(memory_block, (allocated_blocks % 256), block_size);
            g_memory_blocks.emplace_back(memory_block, block_size);
            allocated_blocks++;
            g_allocated_bytes += (long long) block_size;
            consecutive_failures = 0; // 重置失败计数

            if (allocated_blocks % 5 == 0) {
                LOGI("已分配 %ld 块，当前块大小 %d MB，系统可用内存：%zu MB",
                     allocated_blocks, current_block_size_mb, available_memory / (1024 * 1024));
            }

            // 检查停止标志
            if (!g_is_running) {
                LOGI("检测到停止标志，退出压力测试循环");
                break;
            }
        } else {
            LOGE("内存分配失败，尝试块大小：%d MB", current_block_size_mb);
            consecutive_failures++;

            // 检查停止标志
            if (!g_is_running) {
                LOGI("检测到停止标志，退出压力测试循环");
                break;
            }

            // 检查是否是因为内存碎片导致的分配失败
            if (current_block_size_mb > 1) {
                // 如果是大块分配失败，尝试减小块大小
                current_block_size_mb = current_block_size_mb / 2;
                LOGI("大块内存分配失败，尝试减小块大小到：%d MB", current_block_size_mb);
                continue;
            }

            // 如果连续分配失败达到阈值，则暂停一段时间再继续
            if (consecutive_failures >= max_consecutive_failures) {
                LOGE("连续分配失败达到阈值，暂停压力测试一段时间");
                usleep(500000); // 暂停500ms
                consecutive_failures = 0; // 重置失败计数
            } else {
                // 短暂等待后重试
                usleep(50000); // 50ms
            }

            // 检查停止标志
            if (!g_is_running) {
                LOGI("检测到停止标志，退出压力测试循环");
                break;
            }
        }

        // 控制分配速度
        usleep(10000); // 10ms

        // 检查停止标志
        if (!g_is_running) {
            LOGI("检测到停止标志，退出压力测试循环");
            break;
        }
    }

    LOGI("内存压力测试结束，总共分配 %ld 块", allocated_blocks);
}

// 获取系统可用内存的函数
static size_t getAvailableMemory() {
    std::ifstream file("/proc/meminfo");
    std::string line;

    // 读取MemAvailable行
    while (std::getline(file, line)) {
        if (line.substr(0, 13) == "MemAvailable:") {
            size_t startPos = line.find_first_of("0123456789");
            if (startPos != std::string::npos) {
                size_t endPos = line.find_last_of("0123456789");
                std::string valueStr = line.substr(startPos, endPos - startPos + 1);
                long valueKB = std::stol(valueStr); // KB为单位
                return valueKB * 1024; // 转换为字节
            }
            break;
        }
    }

    // 如果无法读取MemAvailable，则返回一个安全值
    return 100 * 1024 * 1024; // 默认返回100MB
}

JNIEXPORT void JNICALL
Java_com_zero_jni_NativeMemoryAllocator_stopStressTest(JNIEnv *env, jobject thiz) {
    LOGI("停止内存压力测试");
    g_is_running = false;

    // 释放所有分配的内存
    for (auto &p: g_memory_blocks) {
        void *block = p.first;
        if (block != nullptr) {
            free(block);
        }
    }
    g_memory_blocks.clear();
    g_allocated_bytes = 0;

    LOGI("已释放所有内存块");
}

JNIEXPORT jlong JNICALL
Java_com_zero_jni_NativeMemoryAllocator_getAllocatedMemoryMB(JNIEnv *env, jobject thiz) {
    return (jlong) (g_allocated_bytes.load() / (1024 * 1024));
}

}