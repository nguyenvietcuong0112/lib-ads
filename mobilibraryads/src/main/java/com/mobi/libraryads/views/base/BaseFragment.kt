package com.mobi.libraryads.views.base

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.mobi.libraryads.commons.sharepreference.SPF
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

abstract class BaseFragment<VB : ViewBinding> : Fragment() {
    private var _binding: VB? = null
    protected val binding: VB
        get() = checkNotNull(_binding) {
            "Binding is only valid between onCreateView and onDestroyView."
        }

    abstract fun inflateVB(inflater: LayoutInflater, container: ViewGroup?): VB

    protected abstract fun initView()

    val mHandler = Handler(Looper.myLooper() ?: Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflateVB(inflater, container)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preLoadData()

        initView()

        if (!SPF(requireContext()).is_app_pro) {
            viewLifecycleOwner.lifecycleScope.launch {
                loadAds()
//                delay(50.milliseconds)
                showAds()
            }
        }

        clickView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    open fun preLoadData() {}
    open fun loadAds() {}
    open fun showAds() {}
    open fun clickView() {}

}