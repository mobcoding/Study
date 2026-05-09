package com.zero.study.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zero.study.ipc.Book

@Entity(tableName = "t_book")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "author") var author: String = "",
)

fun BookEntity.toIpcModel(): Book = Book(
    id = id,
    name = name,
    author = author,
)

fun Book.toEntity(): BookEntity = BookEntity(
    id = id,
    name = name,
    author = author,
)
