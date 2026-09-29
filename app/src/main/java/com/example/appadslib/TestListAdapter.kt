package com.example.appadslib

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mobi.libraryads.ads.utils.EnumAdsNamePosition
import com.mobi.libraryads.views.adapters.BaseListAdapter

class TestListAdapter(
    private val onItemClick: (SampleDataModel) -> Unit
) : BaseListAdapter<SampleDataModel, TestListAdapter.SampleViewHolder>(
    diffCallback = DIFF_CALLBACK,
    scrollingAdLoadDelayMs = 500,
    idleAdLoadDelayMs = 50
) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SampleDataModel>() {
            override fun areItemsTheSame(oldItem: SampleDataModel, newItem: SampleDataModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: SampleDataModel, newItem: SampleDataModel): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getAdPlacementRule(): AdPlacementRule {
        // Chèn lặp lại: Bắt đầu ở vị trí 1, cách 4 phần tử chèn 1 quảng cáo Native
        return AdPlacementRule.Repeating(startPosition = 1, interval = 8)
    }

    override fun getAdName(position: Int): String {
        return EnumAdsNamePosition.NATIVE_ALL.position
    }

    override fun getAdLayoutRes(position: Int): Int {
        return R.layout.admob_layout_native_small
    }

    override fun getIdAd(context: Context, position: Int): String {
        return context.getString(R.string.native_all)
    }

    override fun onCreateDataViewHolder(parent: ViewGroup, viewType: Int): SampleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_test_data, parent, false)
        return SampleViewHolder(view)
    }

    override fun onBindDataViewHolder(holder: SampleViewHolder, position: Int, item: SampleDataModel) {
        holder.txtTitle.text = item.title
        holder.txtDescription.text = item.description

        holder.itemView.setOnClickListener {
            val realItem = getItemForAdapterPosition(holder.bindingAdapterPosition)
            realItem?.let { data ->
                onItemClick(data)
            }
        }
    }

    class SampleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTitle: TextView = view.findViewById(R.id.txtTitle)
        val txtDescription: TextView = view.findViewById(R.id.txtDescription)
    }
}
