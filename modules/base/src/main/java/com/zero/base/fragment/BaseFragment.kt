package com.zero.base.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

/**

 * @date:2024/5/14 16:35
 * @des:BaseFragment
 */
abstract class BaseFragment<VB : ViewBinding>(private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> VB) : Fragment() {
    private var _binding: VB? = null
    val binding: VB get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        initView()
        setListener()
    }

    abstract fun initView()
    abstract fun initData()
    abstract fun setListener()
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
