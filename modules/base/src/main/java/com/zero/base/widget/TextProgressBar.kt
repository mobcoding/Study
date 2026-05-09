package com.zero.base.widget

/**
 * @date:2024/9/30 10:36
 * @path:com.zero.base.widget.TextProgressBar
 */
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.zero.library_base.R
import com.zero.library_base.databinding.BaseTextProgressLayoutBinding
import java.util.Locale

class TextProgressBar(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private val binding by lazy {
        BaseTextProgressLayoutBinding.inflate(LayoutInflater.from(context),this,true)
    }

    fun setProgress(progress: Int) {
        binding.progressBar.progress = progress
        binding.tvProgress.text = String.format(Locale.getDefault(), context.getString(R.string.base_progress_text), progress)
    }
}
