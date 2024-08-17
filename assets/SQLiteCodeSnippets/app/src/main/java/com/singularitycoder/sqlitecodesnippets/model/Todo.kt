package com.singularitycoder.sqlitecodesnippets.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_table")
data class Todo(
    @ColumnInfo(name = "todo_task_name") var taskName: String,
    @ColumnInfo(name = "todo_completed") var isCompleted: Boolean
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "todo_uid")
    var uid = 0

    override fun toString(): String = "Todo{uid=$uid, text='$taskName', completed=${isCompleted}}"
}
