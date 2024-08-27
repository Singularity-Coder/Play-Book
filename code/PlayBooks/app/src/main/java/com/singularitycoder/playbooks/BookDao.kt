package com.singularitycoder.playbooks

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.singularitycoder.playbooks.helpers.DbTable
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    /** room database will replace data based on primary key */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: Book)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Book>)

    @Query("SELECT EXISTS(SELECT * FROM ${DbTable.BOOK} WHERE id = :id)")
    suspend fun isItemPresent(id: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM ${DbTable.BOOK})")
    suspend fun hasItems(): Boolean

    @Transaction
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(book: Book)

    @Query("UPDATE ${DbTable.BOOK} SET completedPagePosition = :completedPage WHERE id LIKE :id")
    suspend fun updateCompletedPageWithId(completedPage: Int, id: String)

    @Query("SELECT * FROM ${DbTable.BOOK} WHERE id LIKE :id LIMIT 1")
    suspend fun getItemById(id: String): Book

    @Query("SELECT * FROM ${DbTable.BOOK}")
    fun getAllItemsLiveData(): LiveData<List<Book>>

    @Query("SELECT * FROM ${DbTable.BOOK}")
    fun getAllItemsStateFlow(): Flow<List<Book>>

//    @Query("SELECT * FROM ${Table.BOOK} WHERE website = :website")
//    fun getAllItemsByWebsiteStateFlow(website: String?): Flow<List<Book>>
//
//    @Query("SELECT * FROM ${Table.BOOK} WHERE isSaved = 1")
//    fun getAllSavedItemsStateFlow(): Flow<List<Book>>
//
//    @Query("SELECT * FROM ${Table.BOOK} WHERE website = :website")
//    fun getItemByWebsiteStateFlow(website: String?): Flow<Book>

    @Query("SELECT * FROM ${DbTable.BOOK}")
    suspend fun getAll(): List<Book>


    @Transaction
    @Delete
    suspend fun delete(book: Book)

//    @Transaction
//    @Query("DELETE FROM ${Table.BOOK} WHERE website = :website")
//    suspend fun deleteByWebsite(website: String?)

    @Transaction
    @Query("DELETE FROM ${DbTable.BOOK} WHERE time >= :time")
    suspend fun deleteAllByTime(time: Long?)

    @Transaction
    @Query("DELETE FROM ${DbTable.BOOK}")
    suspend fun deleteAll()
}
