package com.zero.study.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zero.study.db.entity.BookEntity
import com.zero.study.db.entity.Car
import com.zero.study.db.entity.Dog
import com.zero.study.db.entity.User

/**
 * 数据库创建
 * entities: 实体类
 * version: 数据库初始版本号
 * exportSchema: 是否允许数据库架构将导出到给定的文件夹中【 默认true 】
 *
 */
@Database(entities = [User::class, BookEntity::class, Car::class,Dog::class], version = 1, exportSchema = false)
abstract class RoomDB : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun bookDao(): BookDao
    abstract fun carDao(): CarDao
    abstract fun dogDao(): DogDao
}
