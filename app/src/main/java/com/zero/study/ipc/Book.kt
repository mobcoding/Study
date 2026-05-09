package com.zero.study.ipc

import android.os.Parcel
import android.os.Parcelable

data class Book(
    var id: Int = 0,
    var name: String,
    var author: String = "",
) : Parcelable {

    private constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        name = parcel.readString().orEmpty(),
        author = parcel.readString().orEmpty(),
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(author)
    }

    companion object CREATOR : Parcelable.Creator<Book> {
        override fun createFromParcel(parcel: Parcel): Book = Book(parcel)

        override fun newArray(size: Int): Array<Book?> = arrayOfNulls(size)
    }
}
