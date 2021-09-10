package com.singularitycoder.sqlitecodesnippets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.singularitycoder.sqlitecodesnippets.db.DbOps
import com.singularitycoder.sqlitecodesnippets.ui.theme.Teal200
import com.singularitycoder.sqlitecodesnippets.ui.theme.Teal201
import com.singularitycoder.sqlitecodesnippets.ui.theme.Teal900

@Composable
fun MainActivityUI(dbOps: DbOps) {
    Surface(color = MaterialTheme.colors.background) {

        Column(modifier = Modifier.fillMaxWidth()) {
            val queryState = remember { mutableStateOf("Query") }

            TopAppBar(title = { Text("SQLite") })
            Surface(
                modifier = Modifier.background(color = Teal200),
                color = Teal201,
                elevation = 8.dp
            ) {
                Text(
                    text = queryState.value,
                    style = TextStyle(
                        color = Teal900,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp)
                )
            }

            // CREATE --------------------------------------------------------------------------------------------------------------------------------------------------------

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 0.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        queryState.value = "INSERT INTO todo_table (todo_uid,todo_task_name,todo_completed) VALUES (Todo)"
                        dbOps.createATodo()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Create Todo")
                }

                Button(
                    onClick = {
                        queryState.value = "INSERT INTO todo_table (Todo)"
                        dbOps.createMultipleTodos()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 0.dp),
                ) {
                    Text("Create Multiple Todos")
                }

                // READ --------------------------------------------------------------------------------------------------------------------------------------------------------

                Button(
                    onClick = {
                        queryState.value = "SELECT * FROM todo_table WHERE todo_uid LIKE uid"
                        dbOps.readTodoById()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, top = 16.dp, end = 0.dp, bottom = 0.dp),
                ) {
                    Text("Read Todo By Id")
                }

                Button(
                    onClick = {
                        queryState.value = "SELECT * FROM todo_table"
                        dbOps.readAllTodos()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 0.dp),
                ) {
                    Text("Read All Todos")
                }

                Button(
                    onClick = {
                        queryState.value = "SELECT * FROM todo_table WHERE todo_completed LIKE todoState"
                        dbOps.readAllCompletedTodos()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 0.dp),
                ) {
                    Text("Read All Completed Todos")
                }

                // UPDATE --------------------------------------------------------------------------------------------------------------------------------------------------------

                Button(
                    onClick = {
                        queryState.value = "UPDATE todo_table SET (todo_uid,todo_task_name,todo_completed) = Todo"
                        dbOps.updateATodo()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, top = 16.dp, end = 0.dp, bottom = 0.dp),
                ) {
                    Text("Update Todo By Object")
                }

                Button(
                    onClick = {
                        queryState.value = "UPDATE todo_table SET todo_completed = todoState"
                        dbOps.updateAllTodosIncomplete()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 0.dp),
                ) {
                    Text("Update All Todos State")
                }

                Button(
                    onClick = {
                        queryState.value = "UPDATE todo_table SET todo_task_name = todoName WHERE todo_completed LIKE 1"
                        dbOps.updateAllCompletedTodoNames()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 0.dp),
                ) {
                    Text("Update All Completed Todo Names")
                }

                // DELETE --------------------------------------------------------------------------------------------------------------------------------------------------------

                Button(
                    onClick = {
                        queryState.value = "DELETE FROM todo_table WHERE (todo_uid,todo_task_name,todo_completed) = Todo"
                        dbOps.deleteATodo()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, top = 16.dp, end = 0.dp, bottom = 0.dp),
                ) {
                    Text("Delete Todo By Object")
                }

                Button(
                    onClick = {
                        queryState.value = "DELETE FROM todo_table WHERE todo_completed = todoState"
                        dbOps.deleteAllTodosByState()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 0.dp),
                ) {
                    Text("Delete All Todos By State")
                }

                Button(
                    onClick = {
                        queryState.value = "DELETE FROM todo_table"
                        dbOps.deleteAllTodos()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 64.dp),
                ) {
                    Text("Delete All Todos")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    MainActivityUI(dbOps = DbOps())
}