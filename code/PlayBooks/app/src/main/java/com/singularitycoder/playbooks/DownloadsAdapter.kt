package com.singularitycoder.playbooks

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.singularitycoder.playbooks.databinding.ListItemDownloadBinding
import com.singularitycoder.playbooks.helpers.onCustomLongClick
import com.singularitycoder.playbooks.helpers.onSafeClick
import com.singularitycoder.playbooks.helpers.toLowCase

class DownloadsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var downloadsList = emptyList<Download?>()
    private var itemClickListener: (download: Download?, position: Int) -> Unit = { _, _ -> }
    private var itemLongClickListener: (download: Download?, view: View?, position: Int?) -> Unit = { _, _, _ -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemBinding = ListItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThisViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ThisViewHolder).setData(downloadsList[position])
    }

    override fun getItemCount(): Int = downloadsList.size

    override fun getItemViewType(position: Int): Int = position

    fun setOnItemClickListener(listener: (download: Download?, position: Int) -> Unit) {
        itemClickListener = listener
    }

    fun setOnItemLongClickListener(
        listener: (
            download: Download?,
            view: View?,
            position: Int?
        ) -> Unit
    ) {
        itemLongClickListener = listener
    }

    inner class ThisViewHolder(
        private val itemBinding: ListItemDownloadBinding,
    ) : RecyclerView.ViewHolder(itemBinding.root) {
        @SuppressLint("SetJavaScriptEnabled")
        fun setData(download: Download?) {
            itemBinding.apply {
                tvSource.text = download?.size
                tvTitle.text = download?.title

                val fileExtension = download?.extension?.toLowCase()?.trim()

                root.onSafeClick {
                    itemClickListener.invoke(download, bindingAdapterPosition)
                }
                root.onCustomLongClick {
                    itemLongClickListener.invoke(download, it, bindingAdapterPosition)
                }
            }
        }
    }
}
