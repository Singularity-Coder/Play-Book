package com.singularitycoder.sqlitecodesnippets.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.singularitycoder.sqlitecodesnippets.model.Todo

@Dao
interface TodoDao {
    // CREATE --------------------------------------------------------------------------------------------------------------------------------------------------------
    @Insert
    suspend fun createTodo(todo: Todo?)

    @Insert
    suspend fun createMultipleTodos(todoList: List<Todo?>?)

    // READ --------------------------------------------------------------------------------------------------------------------------------------------------------
    @Query("SELECT * FROM todo_table WHERE todo_uid LIKE :uid")
    suspend fun readTodoById(uid: Int): Todo?

    @Query("SELECT * FROM todo_table")
    suspend fun readAllTodos(): List<Todo?>?

    @Query("SELECT * FROM todo_table WHERE todo_completed LIKE :todoState")
    suspend fun readAllCompletedTodos(todoState: Int): List<Todo?>?

    // UPDATE --------------------------------------------------------------------------------------------------------------------------------------------------------
    @Update
    suspend fun updateTodoByObject(todo: Todo?)

    @Query("UPDATE todo_table SET todo_completed = :todoState")
    suspend fun updateAllTodosState(todoState: Int)

    @Query("UPDATE todo_table SET todo_task_name = :todoName WHERE todo_completed LIKE 1")
    suspend fun updateAllCompletedTodoNames(todoName: String?)

    // DELETE --------------------------------------------------------------------------------------------------------------------------------------------------------
    @Delete
    suspend fun deleteTodoByObject(todo: Todo?)

    @Query("DELETE FROM todo_table WHERE todo_completed = :todoState")
    suspend fun deleteAllTodosByTodoState(todoState: Int)

    @Query("DELETE FROM todo_table")
    suspend fun deleteAllTodos()
}