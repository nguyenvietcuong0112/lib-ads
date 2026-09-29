package com.mobi.libraryads.views.base

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.mobi.libraryads.commons.sharepreference.SPF
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    abstract fun inflateVB(inflater: LayoutInflater, container: ViewGroup?): VB

    protected abstract fun initView()

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
                delay(100)
                showAds()
            }
        }

        clickView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            it.window?.setLayout(width, height)
        }
        isCancelable = false
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