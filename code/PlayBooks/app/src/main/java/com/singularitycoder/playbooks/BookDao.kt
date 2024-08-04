package com.singularitycoder.playbooks

import androidx.lifecycle.LiveData
import androidx.room.*
import com.singularitycoder.playbooks.helpers.Table
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    /** room database will replace data based on primary key */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: Book)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(list: List<Book>)


    @Transaction
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(book: Book)

//    @Query("UPDATE ${Table.BOOK} SET link = :link WHERE website LIKE :website")
//    fun updateLinkWithWebsite(link: String?, website: String)


//    @Transaction
//    @Query("SELECT * FROM ${Table.BOOK} WHERE website LIKE :website LIMIT 1")
//    suspend fun getItemByWebsite(website: String?): Book

    @Query("SELECT * FROM ${Table.BOOK}")
    fun getAllItemsLiveData(): LiveData<List<Book>>

    @Query("SELECT * FROM ${Table.BOOK}")
    fun getAllItemsStateFlow(): Flow<List<Book>>

//    @Query("SELECT * FROM ${Table.BOOK} WHERE website = :website")
//    fun getAllItemsByWebsiteStateFlow(website: String?): Flow<List<Book>>
//
//    @Query("SELECT * FROM ${Table.BOOK} WHERE isSaved = 1")
//    fun getAllSavedItemsStateFlow(): Flow<List<Book>>
//
//    @Query("SELECT * FROM ${Table.BOOK} WHERE website = :website")
//    fun getItemByWebsiteStateFlow(website: String?): Flow<Book>

    @Query("SELECT * FROM ${Table.BOOK}")
    suspend fun getAll(): List<Book>


    @Transaction
    @Delete
    suspend fun delete(book: Book)

//    @Transaction
//    @Query("DELETE FROM ${Table.BOOK} WHERE website = :website")
//    suspend fun deleteByWebsite(website: String?)

    @Transaction
    @Query("DELETE FROM ${Table.BOOK} WHERE time >= :time")
    suspend fun deleteAllByTime(time: Long?)

    @Transaction
    @Query("DELETE FROM ${Table.BOOK}")
    suspend fun deleteAll()
}
