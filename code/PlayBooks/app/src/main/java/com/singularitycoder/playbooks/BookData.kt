package com.singularitycoder.playbooks

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.singularitycoder.playbooks.helpers.DbTable
import kotlinx.parcelize.Parcelize

@Entity(tableName = DbTable.BOOK_DATA)
@Parcelize
data class BookData(
    @PrimaryKey var id: String,
    var path: String? = "",
    var text: String? = "",
    var pageCount: Int = 0,
    var pagePositionsList: List<Int>,
    var periodPositionsList: List<Int>
) : Parcelable