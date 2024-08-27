package com.singularitycoder.playbooks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    private val bookDao: BookDao,
    private val bookDataDao: BookDataDao,
) : ViewModel() {

    fun addBookItem(book: Book) = viewModelScope.launch {
        bookDao.insert(book)
    }

    fun getAllBookItemsFlow() = bookDao.getAllItemsStateFlow()

    suspend fun getAllBookItems() = bookDao.getAll()

    suspend fun hasBooks() = bookDao.hasItems()

    suspend fun getBookItemById(id: String) = bookDao.getItemById(id)

    suspend fun getBookDataItemById(id: String) = bookDataDao.getItemById(id)

    suspend fun updateCompletedPageWithId(completedPage: Int, id: String) = bookDao.updateCompletedPageWithId(completedPage, id)

//    suspend fun getLast3BookItems() = bookDao.getLast3By()

    fun deleteBookItem(book: Book?) = viewModelScope.launch {
        bookDao.delete(book ?: return@launch)
    }

    fun deleteBookDataItem(book: Book?) = viewModelScope.launch {
        bookDataDao.deleteBy(book?.id ?: return@launch)
    }

    fun deleteAllBookItems() = viewModelScope.launch {
        bookDao.deleteAll()
    }

    fun deleteAllBookDataItems() = viewModelScope.launch {
        bookDataDao.deleteAll()
    }
}
