package com.zero.study.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zero.study.db.entity.BookEntity


/**
 * 数据访问对象
 */
@Dao
interface BookDao {
    // 查询
    @Query("SELECT * FROM t_book")
    fun queryAllBooks(): MutableList<BookEntity>

    //根据姓名参数查询
    @Query("SELECT * FROM t_book WHERE name = :name")
    fun queryBookByName(name: String): BookEntity?

    /**
     * 按名字模拟查询
     */
    @Query("SELECT * FROM t_book WHERE name LIKE :key")
    fun queryFuzzyByName(key: String): MutableList<BookEntity>

    // 添加单条数据
    @Insert
    fun addBook(vararg book: BookEntity)

    // 添加批量数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBookList(list: MutableList<BookEntity>)

    // 更新某一个数据
    @Update
    fun updateBook(vararg book: BookEntity)

    //更新所有数据
    @Query("UPDATE t_book set author='毛毛'")
    fun updateAll()

    //删除某一个数据
    @Delete
    fun deleteSingle(vararg user: BookEntity)

    //删除表里所有数据
    @Query("DELETE FROM t_book")
    fun deleteAllBook()
}
