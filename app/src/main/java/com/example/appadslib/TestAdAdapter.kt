package com.example.appadslib

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobi.libraryads.ads.utils.EnumAdsNamePosition
import com.mobi.libraryads.views.adapters.BaseAdAdapter

class TestAdAdapter(
    private val items: List<String>
) : BaseAdAdapter<String, TestAdAdapter.TextViewHolder>() {

    override fun getAdPlacementRule(): AdPlacementRule {
        return AdPlacementRule.Repeating(1, 5)
    }

    override fun getAdName(position: Int): String {
        return EnumAdsNamePosition.NATIVE_ALL.position
    }

    override fun getAdLayoutRes(position: Int): Int {
        return R.layout.admob_layout_native_small
    }

    override fun canShowAd(): Boolean {
        return super.canShowAd()
    }

//    override fun getIdAd(context: Context, position: Int): String {
//        return "context.getString(R.string.native_edit)"
//    }

    override fun onCreateDataViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_test_data, parent, false)
        return TextViewHolder(view)
    }

    override fun onBindDataViewHolder(holder: TextViewHolder, position: Int, item: String) {
        holder.titleView.text = item
        holder.descView.text = "Description for $item inside mock layout details."
    }

    override fun getDataItemCount(): Int = items.size

    override fun getDataItem(position: Int): String = items[position]

    class TextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.findViewById(R.id.txtTitle)
        val descView: TextView = view.findViewById(R.id.txtDescription)
    }
}
