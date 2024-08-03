package com.singularitycoder.playbooks

import android.os.Parcelable
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

//@Entity(tableName = Table.DOWNLOAD)
@Parcelize
data class Download(
    @PrimaryKey var id: Long = 0,
    var path: String? = "",
    var title: String = "",
    var time: Long? = 0L,
    var size: String? = "",
    var link: String? = "",
    var extension: String? = "",
    var isDirectory: Boolean = false
) : Parcelable
