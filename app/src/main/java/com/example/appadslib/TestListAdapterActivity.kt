package com.example.appadslib

import android.view.LayoutInflater
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobi.libraryads.views.base.BaseActivity
import com.example.appadslib.databinding.ActivityTestListAdapterBinding

class TestListAdapterActivity : BaseActivity<ActivityTestListAdapterBinding>() {

    private lateinit var adapter: TestListAdapter
    private val itemList = mutableListOf<SampleDataModel>()
    private var nextId = 1

    override fun inflateVB(inflater: LayoutInflater): ActivityTestListAdapterBinding {
        return ActivityTestListAdapterBinding.inflate(inflater)
    }

    override fun initView() {
        adapter = TestListAdapter { item ->
            Toast.makeText(this, "Bạn đã bấm chọn: ${item.title}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Tạo dữ liệu ban đầu (10 items)
        repeat(100) {
            itemList.add(
                SampleDataModel(
                    id = nextId,
                    title = "Item Dữ Liệu #$nextId",
                    description = "Mô tả cho phần tử dữ liệu số $nextId"
                )
            )
            nextId++
        }
        adapter.submitList(itemList.toList())

        // Nút thêm phần tử vào đầu danh sách
        binding.btnAddItem.setOnClickListener {
            itemList.add(
                0,
                SampleDataModel(
                    id = nextId,
                    title = "Item Mới #$nextId",
                    description = "Được thêm vào danh sách tại ID #$nextId"
                )
            )
            nextId++
            adapter.submitList(itemList.toList())
            Toast.makeText(this, "Đã thêm 1 item mới!", Toast.LENGTH_SHORT).show()
        }

        // Nút xóa phần tử đầu tiên
        binding.btnRemoveItem.setOnClickListener {
            if (itemList.isNotEmpty()) {
                val removed = itemList.removeAt(0)
                adapter.submitList(itemList.toList())
                Toast.makeText(this, "Đã xóa: ${removed.title}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Danh sách đang trống!", Toast.LENGTH_SHORT).show()
            }
        }

        // Nút lọc / làm mới danh sách dữ liệu
        binding.btnFilterList.setOnClickListener {
            itemList.clear()
            repeat(50) {
                itemList.add(
                    SampleDataModel(
                        id = nextId,
                        title = "Item Làm Mới #$nextId",
                        description = "Cập nhật qua DiffUtil submitList()"
                    )
                )
                nextId++
            }
            adapter.submitList(itemList.toList(), scrollToTop = false)
            Toast.makeText(this, "Đã reset danh sách qua submitList()!", Toast.LENGTH_SHORT).show()
        }
    }
}
