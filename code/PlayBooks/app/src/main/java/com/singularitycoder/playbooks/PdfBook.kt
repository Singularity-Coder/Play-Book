package com.singularitycoder.playbooks

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PdfBook(
    var pageCount: Int = 0,
    var text: String? = "",
    var periodCountPerPageList: List<Int>,
    var periodPositionsList: List<Int>
) : Parcelable