package com.mobi.libraryads.views.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mobi.libraryads.FOConfigs
import com.mobi.libraryads.R
import com.mobi.libraryads.commons.utils.clickOnce
import com.mobi.libraryads.data.LanguageModel


class LanguageAdapter(
    private val onAdapterClick: OnAdapterClick
) : ListAdapter<LanguageModel, RecyclerView.ViewHolder>(diffCallback) {

    companion object {

        private const val TYPE_NORMAL = 0
        private const val TYPE_SELECTED = 1

        val diffCallback = object : DiffUtil.ItemCallback<LanguageModel>() {

            override fun areItemsTheSame(
                oldItem: LanguageModel,
                newItem: LanguageModel
            ): Boolean {
                return oldItem.code == newItem.code
            }

            override fun areContentsTheSame(
                oldItem: LanguageModel,
                newItem: LanguageModel
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isSelect) {
            TYPE_SELECTED
        } else {
            TYPE_NORMAL
        }
    }

    class SelectLangVH(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {

        fun bindData(
            item: LanguageModel,
            onClick: (LanguageModel) -> Unit
        ) {
            val imgFlag = itemView.findViewById<ImageView>(R.id.imgFlag)
            val txtLanguage = itemView.findViewById<TextView>(R.id.txtLanguage)

            Glide.with(itemView.context)
                .load(item.icFlag)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(imgFlag)

            txtLanguage.setText(item.txtLanguage)

            itemView.rootView.clickOnce {
                onClick(item)
            }
        }

        companion object {
            fun create(parent: ViewGroup): SelectLangVH {
                val view = LayoutInflater.from(parent.context)
                    .inflate(FOConfigs.languageConfig.uiLanguageConfig.itemLangSelected, parent, false)

                return SelectLangVH(view)
            }
        }
    }

    class DefaultLangVH(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {

        fun bindData(
            item: LanguageModel,
            onClick: (LanguageModel) -> Unit
        ) {
            val imgFlag = itemView.findViewById<ImageView>(R.id.imgFlag)
            val txtLanguage = itemView.findViewById<TextView>(R.id.txtLanguage)
            Glide.with(itemView.context)
                .load(item.icFlag)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(imgFlag)

            txtLanguage.setText(item.txtLanguage)

            itemView.rootView.clickOnce {
                onClick(item)
            }
        }

        companion object {

            fun create(parent: ViewGroup): DefaultLangVH {

                val view = LayoutInflater.from(parent.context)
                    .inflate(FOConfigs.languageConfig.uiLanguageConfig.itemLangDefault, parent, false)

                return DefaultLangVH(view)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        return when (viewType) {

            TYPE_SELECTED -> {
                SelectLangVH.create(parent)
            }

            else -> {
                DefaultLangVH.create(parent)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        val item = getItem(position)

        when (holder) {

            is DefaultLangVH -> {
                holder.bindData(item) {
                    selectItem(it)
                }
            }

            is SelectLangVH -> {
                holder.bindData(item) {
                    selectItem(it)
                }
            }
        }
    }

    private fun selectItem(selectedItem: LanguageModel) {

        val newList = currentList.map {
            it.copy(isSelect = it.code == selectedItem.code)
        }

        submitList(newList)

        onAdapterClick.onSelect(selectedItem)
    }
}