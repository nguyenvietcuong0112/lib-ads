package com.mobi.libraryads.views.dialogs

import android.content.DialogInterface
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.mobi.libraryads.R
import com.mobi.libraryads.commons.utils.clickOnce
import com.mobi.libraryads.commons.utils.isInternetConnected
import com.mobi.libraryads.commons.utils.showToastLong
import com.mobi.libraryads.databinding.DialogRequestWifiBinding
import com.mobi.libraryads.views.base.BaseDialogFragment

class DialogRequestInternet :
    BaseDialogFragment<DialogRequestWifiBinding>() {

    interface ConfirmCallback {
        fun clickYes()
        fun clickNo()
    }

    private val wifiSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Khi quay lại từ màn hình Wi-Fi
            if (requireActivity().isInternetConnected()) {
                onNetworkConnectedAfterWifiSettings() // Gọi hàm bạn muốn
                dismiss()
            } else {
                requireActivity()
                    .showToastLong(requireContext().getString(R.string.please_connect_to_internet))
            }
        }

    companion object {
        fun newInstance(): DialogRequestInternet {
            return DialogRequestInternet()
        }
    }

    private var mCallback: ConfirmCallback? = null

    fun setCallback(callback: ConfirmCallback) {
        mCallback = callback
    }


    override fun inflateVB(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = DialogRequestWifiBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.apply {
            root.clickOnce {
                if (!requireActivity().isInternetConnected()) {
                    requireActivity()
                        .showToastLong(requireContext().getString(R.string.please_connect_to_internet))
                } else {
                    onNetworkConnectedAfterWifiSettings()
                }
            }

            btnAccept.clickOnce {
                if (!requireActivity().isInternetConnected()) {
                    wifiSettingsLauncher.launch(Intent(Settings.ACTION_WIFI_SETTINGS))
                } else {
                    onNetworkConnectedAfterWifiSettings()
                }
            }

        }

    }

    private fun onNetworkConnectedAfterWifiSettings() {
        // Gọi hàm bạn muốn sau khi có kết nối mạng
        mCallback?.clickYes()
    }

    override fun onDismiss(dialog: DialogInterface) {
        mCallback?.clickNo()
        super.onDismiss(dialog)

    }

}