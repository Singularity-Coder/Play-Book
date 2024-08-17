package com.singularitycoder.sqlitecodesnippets.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.singularitycoder.sqlitecodesnippets.db.DbOps
import com.singularitycoder.sqlitecodesnippets.ui.MainActivityUI
import com.singularitycoder.sqlitecodesnippets.ui.theme.SQLiteCodeSnippetsTheme

// https://www.sqlite.org/lang.html
// Room ORM - Abstraction layer over SQLite

// 3 steps to work with Room ORM
// 1. DB class
// 2. Entity - POJOs that act as tables
// 3. Dao Interface - has methods for accessing DB for performing DB operations


// 6 components of DB class
// 1. Must be annotated with @Database(entities = {x.class}, version = 1)
// 2. Must be an abstract class
// 3. Must extends RoomDatabase
// 4. Must include all entities
// 5. Must contain 0 arg abstract method & return an interface - annotated with @Dao - we use this to communicate with DB
// 6. Must contain synchronized DB instance a.k.a singleton which contains the DB name


// Components of Entity
// 1. @Entity annotation above class
// 2. @PrimaryKey(autoGenerate = true) for auto-incrementing column row unique ids
// 3. @ColumnInfo(name = "some_column_name") for creating a column with name mentioned
// 4. Empty constructor


// Components of Dao
// 1. Its an interface
// 2. @Insert for inserting row
// 3. @Update for updating row
// 4. @Delete for deleting row
// 5. @Query for custom DB operations
// 6. @Ignore for informing DB to not consider it as a DB column


// DB Operations
// 1. Create single item
// 2. Create multiple items at once

// 3. Read an item by some key
// 4. Read all items
// 5. Read all items with the key "x"

// 6. Update an item
// 7. Update all items at once
// 8. Update all items with the key "x"

// 9. Delete an item
// 10. Delete all items at once
// 11. Delete all items with the key "x"

// Modifier.fillMaxWidth().padding(all = 16.dp).background(color = Color.Cyan).padding(all = 16.dp)
// First padding is margin, second padding is padding

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SQLiteCodeSnippetsTheme { MainActivityUI(dbOps = DbOps(context = this, TAG = TAG)) } }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SQLiteCodeSnippetsTheme { MainActivityUI(dbOps = DbOps()) }
}