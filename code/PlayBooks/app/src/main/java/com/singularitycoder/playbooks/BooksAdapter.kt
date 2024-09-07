package com.singularitycoder.playbooks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.singularitycoder.playbooks.databinding.ListItemBookBinding
import com.singularitycoder.playbooks.helpers.deviceHeight
import com.singularitycoder.playbooks.helpers.deviceWidth
import com.singularitycoder.playbooks.helpers.getBookCoversFileDir
import com.singularitycoder.playbooks.helpers.onCustomLongClick
import com.singularitycoder.playbooks.helpers.onSafeClick
import java.io.File

class BooksAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var bookList = emptyList<Book?>()
    private var itemClickListener: (book: Book?, position: Int) -> Unit = { _, _ -> }
    private var itemLongClickListener: (book: Book?, view: View?, position: Int?) -> Unit = { _, _, _ -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemBinding = ListItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThisViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ThisViewHolder).setData(bookList[position])
    }

    override fun getItemCount(): Int = bookList.size

    override fun getItemViewType(position: Int): Int = position

    fun setOnItemClickListener(listener: (book: Book?, position: Int) -> Unit) {
        itemClickListener = listener
    }

    fun setOnItemLongClickListener(
        listener: (
            book: Book?,
            view: View?,
            position: Int?
        ) -> Unit
    ) {
        itemLongClickListener = listener
    }

    inner class ThisViewHolder(
        private val itemBinding: ListItemBookBinding,
    ) : RecyclerView.ViewHolder(itemBinding.root) {

        fun setData(book: Book?) {
            itemBinding.apply {
                itemBinding.root.animate().run {
                    withStartAction {
                        // make views visble or not
                    }
                    duration = 750
                    alpha(1.0F) // set default layout alpha to 0. So transition from alpha 0 to 1
                    withEndAction {}
                }
                ivItemImage.layoutParams.height = deviceHeight() / 6
                ivItemImage.layoutParams.width = deviceWidth() / 4
                val bookCover = File(
                    /* parent = */ itemBinding.root.context.getBookCoversFileDir(),
                    /* child = */ "${book?.id}.jpg"
                )
                ivItemImage.load(bookCover)
                tvSource.text = "${book?.extension}  •  ${book?.pageCount} pages  •  ${book?.size}"
                tvTitle.text = book?.title
                root.onSafeClick {
                    itemClickListener.invoke(book, bindingAdapterPosition)
                }
                root.onCustomLongClick {
                    itemLongClickListener.invoke(book, it, bindingAdapterPosition)
                }
            }
        }
    }
}
