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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.singularitycoder.sqlitecodesnippets.db.DbOps
import com.singularitycoder.sqlitecodesnippets.ui.theme.Teal200
import com.singularitycoder.sqlitecodesnippets.ui.theme.Teal201
import com.singularitycoder.sqlitecodesnippets.ui.theme.Teal900

@Composable
fun MainActivityUI(dbOps: DbOps) {
    val queryState = remember { mutableStateOf("Query") }

    @Composable
    fun DefaultButton(
        queryText: String,
        paddingTop: Dp,
        btnText: String,
        onBtnClick: () -> Unit
    ) {
        Button(
            onClick = {
                queryState.value = queryText
                onBtnClick.invoke()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 0.dp, top = paddingTop, end = 0.dp, bottom = 0.dp),
        ) {
            Text(btnText)
        }
    }

    Surface(color = MaterialTheme.colors.background) {

        Column(modifier = Modifier.fillMaxWidth()) {

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

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 0.dp)
            ) {

                Spacer(Modifier.height(16.dp))

                // CREATE --------------------------------------------------------------------------------------------------------------------------------------------------------

                DefaultButton(
                    queryText = "INSERT INTO todo_table (todo_uid,todo_task_name,todo_completed) VALUES (Todo)",
                    paddingTop = 0.dp,
                    btnText = "Create Todo",
                    onBtnClick = { dbOps.createATodo() }
                )

                DefaultButton(
                    queryText = "INSERT INTO todo_table (Todo)",
                    paddingTop = 8.dp,
                    btnText = "Create Multiple Todos",
                    onBtnClick = { dbOps.createMultipleTodos() }
                )

                // READ --------------------------------------------------------------------------------------------------------------------------------------------------------

                DefaultButton(
                    queryText = "SELECT * FROM todo_table WHERE todo_uid LIKE uid",
                    paddingTop = 16.dp,
                    btnText = "Read Todo By Id",
                    onBtnClick = { dbOps.readTodoById() }
                )

                DefaultButton(
                    queryText = "SELECT * FROM todo_table",
                    paddingTop = 8.dp,
                    btnText = "Read All Todos",
                    onBtnClick = { dbOps.readAllTodos() }
                )

                DefaultButton(
                    queryText = "SELECT * FROM todo_table WHERE todo_completed LIKE todoState",
                    paddingTop = 8.dp,
                    btnText = "Read All Completed Todos",
                    onBtnClick = { dbOps.readAllCompletedTodos() }
                )

                // UPDATE --------------------------------------------------------------------------------------------------------------------------------------------------------

                DefaultButton(
                    queryText = "UPDATE todo_table SET (todo_uid,todo_task_name,todo_completed) = Todo",
                    paddingTop = 16.dp,
                    btnText = "Update Todo By Object",
                    onBtnClick = { dbOps.updateATodo() }
                )

                DefaultButton(
                    queryText = "UPDATE todo_table SET todo_completed = todoState",
                    paddingTop = 8.dp,
                    btnText = "Update All Todos State",
                    onBtnClick = { dbOps.updateAllTodosIncomplete() }
                )

                DefaultButton(
                    queryText = "UPDATE todo_table SET todo_task_name = todoName WHERE todo_completed LIKE 1",
                    paddingTop = 8.dp,
                    btnText = "Update All Completed Todo Names",
                    onBtnClick = { dbOps.updateAllCompletedTodoNames() }
                )

                // DELETE --------------------------------------------------------------------------------------------------------------------------------------------------------

                DefaultButton(
                    queryText = "DELETE FROM todo_table WHERE (todo_uid,todo_task_name,todo_completed) = Todo",
                    paddingTop = 16.dp,
                    btnText = "Delete Todo By Object",
                    onBtnClick = { dbOps.deleteATodo() }
                )

                DefaultButton(
                    queryText = "DELETE FROM todo_table WHERE todo_completed = todoState",
                    paddingTop = 8.dp,
                    btnText = "Delete All Todos By State",
                    onBtnClick = { dbOps.deleteAllTodosByState() }
                )

                DefaultButton(
                    queryText = "DELETE FROM todo_table",
                    paddingTop = 8.dp,
                    btnText = "Delete All Todos",
                    onBtnClick = { dbOps.deleteAllTodos() }
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    MainActivityUI(dbOps = DbOps())
}