package com.singularitycoder.sqlitecodesnippets.db

import android.content.Context
import android.util.Log
import com.singularitycoder.sqlitecodesnippets.MyApp
import com.singularitycoder.sqlitecodesnippets.model.Todo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DbOps(context: Context?, val TAG: String) {

    private val dao: TodoDao = (context?.applicationContext as? MyApp)?.database?.todoDao()!!

    constructor() : this(null, "")

    // CREATE --------------------------------------------------------------------------------------------------------------------------------------------------------

    fun createATodo() = CoroutineScope(Dispatchers.IO).launch {
        val todo = Todo("watch some anime ...", false)
        dao.createTodo(todo)
    }

    fun createMultipleTodos() = CoroutineScope(Dispatchers.IO).launch {
        val todoList = ArrayList<Todo?>().apply {
            add(Todo("Watch Code Geass", false))
            add(Todo("Watch Death Note", true))
            add(Todo("Watch marvel movies", true))
            add(Todo("Watch \"Your Name\" movie", false))
        }
        dao.createMultipleTodos(todoList)
        Log.d(TAG, "Todos created.")
    }

    // READ --------------------------------------------------------------------------------------------------------------------------------------------------------

    fun readTodoById() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val thirdTodo = dao.readTodoById(3)
            Log.d(TAG, "Third Todo: " + thirdTodo.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readAllTodos() = CoroutineScope(Dispatchers.IO).launch {
        val todoList = dao.readAllTodos()
        Log.d(TAG, "All Todos: " + todoList.toString())
    }

    fun readAllCompletedTodos() = CoroutineScope(Dispatchers.IO).launch {
        val todoList = dao.readAllCompletedTodos(1)
        Log.d(TAG, "All Completed Todos: " + todoList.toString())
    }

    // UPDATE --------------------------------------------------------------------------------------------------------------------------------------------------------

    fun updateATodo() = CoroutineScope(Dispatchers.IO).launch {
        val todo = dao.readTodoById(2)
        if (todo != null) {
            todo.isCompleted = true
            dao.updateTodoByObject(todo)
            Log.d(TAG, "Todo is updated")
        }
    }

    fun updateAllTodosIncomplete() = CoroutineScope(Dispatchers.IO).launch {
        dao.updateAllTodosState(0)
        Log.d(TAG, "All todos are incomplete")
    }

    fun updateAllCompletedTodoNames() = CoroutineScope(Dispatchers.IO).launch {
        dao.updateAllCompletedTodoNames("Omae wa mo shinderu!")
        Log.d(TAG, "All completed todo names changed!")
    }

    // DELETE --------------------------------------------------------------------------------------------------------------------------------------------------------

    fun deleteATodo() = CoroutineScope(Dispatchers.IO).launch {
        val todo = dao.readTodoById(2)
        if (todo != null) {
            Log.d(TAG, "Delete Todo: $todo")
            dao.deleteTodoByObject(todo)
            Log.d(TAG, "Todo has been deleted")
        }
    }

    fun deleteAllTodosByState() = CoroutineScope(Dispatchers.IO).launch {
        dao.deleteAllTodosByTodoState(1)
        Log.d(TAG, "Deleting all completed todos.")
    }

    fun deleteAllTodos() = CoroutineScope(Dispatchers.IO).launch {
        dao.deleteAllTodos()
        Log.d(TAG, "Deleting all todos.")
    }
}