package com.example.appadslib

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mobi.libraryads.views.base.BaseActivity
import com.example.appadslib.databinding.ActivityTestAdBinding
import com.example.appadslib.databinding.FragmentTestAdBinding

class TestAdActivity : BaseActivity<ActivityTestAdBinding>() {

    override fun inflateVB(inflater: LayoutInflater): ActivityTestAdBinding {
        return ActivityTestAdBinding.inflate(inflater)
    }

    override fun initView() {
        val pagerAdapter = TestPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // Bắt sự kiện click nút chuyển category không cần vuốt
        binding.btnCat1.setOnClickListener {
            binding.viewPager.currentItem = 0
        }
        binding.btnCat2.setOnClickListener {
            binding.viewPager.currentItem = 1
        }
        binding.btnCat3.setOnClickListener {
            binding.viewPager.currentItem = 2
        }
    }

    class TestPagerAdapter(activity: TestAdActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return TestAdFragment.newInstance(categoryName = "Category #${position + 1}")
        }
    }
}

class TestAdFragment : Fragment() {
    private var _binding: FragmentTestAdBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_CAT = "arg_cat"

        fun newInstance(categoryName: String): TestAdFragment {
            return TestAdFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CAT, categoryName)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestAdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val categoryName = arguments?.getString(ARG_CAT) ?: "Category"
        
        val dummyData = List(50) { index -> "[$categoryName] Item #$index" }
        val adapter = TestAdAdapter(items = dummyData)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
