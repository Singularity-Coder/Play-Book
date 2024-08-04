package com.singularitycoder.playbooks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    private val bookDao: BookDao,
) : ViewModel() {

    fun addToHistory(book: Book) = viewModelScope.launch {
        bookDao.insert(book)
    }

    fun getAllHistoryFlow() = bookDao.getAllItemsStateFlow()

    suspend fun getAllHistory() = bookDao.getAll()

//    suspend fun getHistoryItemByLink(link: String?) = bookDao.getItemByLink(link)

//    suspend fun getLast3HistoryItems() = bookDao.getLast3By()

    fun deleteItem(book: Book?) = viewModelScope.launch {
        bookDao.delete(book ?: return@launch)
    }

    fun deleteAllHistory() = viewModelScope.launch {
        bookDao.deleteAll()
    }

    fun deleteAllHistoryByTime(elapsedTime: Long?) = viewModelScope.launch {
        bookDao.deleteAllByTime(elapsedTime)
    }
}
