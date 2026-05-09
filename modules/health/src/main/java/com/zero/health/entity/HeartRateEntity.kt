package com.zero.health.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zero.health.bean.HeartRateStatus
import java.sql.Date

@Keep
@Entity
data class HeartRateEntity(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    var feeling: Int = 0,

    var medication: Int = 1,
    var status: String = HeartRateStatus.Sitting.name,

    var bpm: Int = 80,
    var stress: Float = 0.0f,
    var energy: Float = 0.0f,
    var amo: Float = 0.0f,
    var pnn50: Float = 0.0f,
    var medsd: Float = 0.0f,
    var meanrr: Float = 0.0f,
    var rmssd: Float = 0.0f,
    var sdnn: Float = 0.0f,

    var time: Date = Date(System.currentTimeMillis()),
    var createTime: Date = Date(System.currentTimeMillis()),
    var updateTime: Date = Date(System.currentTimeMillis()),
)


