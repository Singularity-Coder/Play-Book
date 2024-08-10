package com.singularitycoder.playbooks

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.singularitycoder.playbooks.helpers.Table
import com.singularitycoder.playbooks.helpers.sanitize
import kotlinx.parcelize.Parcelize

@Parcelize
data class PdfBook(
    var pageCount: Int = 0,
    var text: String? = "",
    var pagePositionsList: List<Int>,
    var periodPositionsList: List<Int>
) : Parcelable