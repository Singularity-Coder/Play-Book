package com.singularitycoder.playbooks

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.singularitycoder.playbooks.helpers.DbTable
import kotlinx.parcelize.Parcelize

/** [periodPositionsList] is used to find the start of a period to set the start and end positions of a sentence
 * [periodCountPerPageList] is used for advancing to next page by adding each count in this list to current period position
 * [periodToPageMap] matches period positions and corresponding pages */
@Entity(tableName = DbTable.BOOK_DATA)
@Parcelize
data class BookData(
    @PrimaryKey var id: String,
    var path: String? = "",
    var text: String? = "",
    var pageCount: Int = 0,
    var periodCountPerPageList: List<Int>,
    var periodPositionsList: List<Int>,
    var periodToPageMap: HashMap<Int, Int>
) : Parcelable