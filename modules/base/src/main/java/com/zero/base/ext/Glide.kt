package com.zero.base.ext

import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.bumptech.glide.request.transition.TransitionFactory
import com.bumptech.glide.signature.ObjectKey
import com.zero.library_base.R
import java.io.File

/**
 * @date:2025/6/11 11:13
 * @path:com.zero.base.ext.Glide
 * .transition(getOptionalCrossFadeTransition(crossFadeDuration))
 */


fun AppCompatImageView.loadImage(path: String?, radius: Int = 0) {
    Glide.with(this).load(path).placeholder(R.drawable.base_place_holder).transform(RoundedCorners(radius.dp)).into(this)
}

fun AppCompatImageView.loadImage(path: String?, width: Int, height: Int) {
    Glide.with(this).load(path).override(width, height).placeholder(R.drawable.base_place_holder).transition(getOptionalCrossFadeTransition(500)).centerCrop().into(this)
}

fun AppCompatImageView.loadImageSignature(path: String) {
    Glide.with(this).load(path).signature(ObjectKey(File(path).lastModified())).placeholder(R.drawable.base_place_holder).error(R.drawable.base_place_holder).into(this)
}

fun getOptionalCrossFadeTransition(duration: Int): DrawableTransitionOptions {
    return DrawableTransitionOptions.with(TransitionFactory { dataSource, isFirstResource ->
        if (dataSource == DataSource.RESOURCE_DISK_CACHE) return@TransitionFactory null
        DrawableCrossFadeFactory.Builder(duration).build().build(dataSource, isFirstResource)
    })
}