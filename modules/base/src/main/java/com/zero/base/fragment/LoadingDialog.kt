package com.zero.base.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import com.zero.library_base.databinding.BaseLoadingDialogBinding

/**
 * @author Admin
 */
class LoadingDialog : DialogFragment() {
    private var _binding: BaseLoadingDialogBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BaseLoadingDialogBinding.inflate(inflater, container, false)
        requireDialog().window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null && dialog?.window != null) {
            dialog?.setCanceledOnTouchOutside(false)
            dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        _binding?.pbLoading?.cancelAnimation()
        _binding = null
        super.onDestroyView()
    }
}
