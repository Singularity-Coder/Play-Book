package com.singularitycoder.playbooks

import androidx.lifecycle.LiveData
import androidx.room.*
import com.singularitycoder.playbooks.helpers.Table
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDataDao {

    /** room database will replace data based on primary key */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookData: BookData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<BookData>)

    @Query("SELECT EXISTS(SELECT * FROM ${Table.BOOK_DATA} WHERE id = :id)")
    suspend fun isItemPresent(id: String): Boolean

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(bookData: BookData)

//    @Query("UPDATE ${Table.BOOK_DATA} SET link = :link WHERE website LIKE :website")
//    fun updateLinkWithWebsite(link: String?, website: String)

    @Query("SELECT * FROM ${Table.BOOK_DATA} WHERE id LIKE :id LIMIT 1")
    suspend fun getItemById(id: String): BookData

    @Query("SELECT * FROM ${Table.BOOK_DATA}")
    fun getAllItemsLiveData(): LiveData<List<BookData>>

    @Query("SELECT * FROM ${Table.BOOK_DATA}")
    fun getAllItemsStateFlow(): Flow<List<BookData>>

//    @Query("SELECT * FROM ${Table.BOOK_DATA} WHERE website = :website")
//    fun getAllItemsByWebsiteStateFlow(website: String?): Flow<List<BookData>>
//
//    @Query("SELECT * FROM ${Table.BOOK_DATA} WHERE isSaved = 1")
//    fun getAllSavedItemsStateFlow(): Flow<List<BookData>>
//
//    @Query("SELECT * FROM ${Table.BOOK_DATA} WHERE website = :website")
//    fun getItemByWebsiteStateFlow(website: String?): Flow<BookData>

    @Query("SELECT * FROM ${Table.BOOK_DATA}")
    suspend fun getAll(): List<BookData>


    @Delete
    suspend fun delete(bookData: BookData)

    @Query("DELETE FROM ${Table.BOOK_DATA} WHERE id = :id")
    suspend fun deleteBy(id: String)

    @Query("DELETE FROM ${Table.BOOK_DATA}")
    suspend fun deleteAll()
}
