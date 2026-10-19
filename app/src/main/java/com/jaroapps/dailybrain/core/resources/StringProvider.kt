package com.jaroapps.dailybrain.core.resources

import android.content.Context
import androidx.annotation.StringRes

fun interface StringProvider {
    fun getString(@StringRes resourceId: Int): String
}

class AndroidStringProvider(
    private val context: Context,
) : StringProvider {
    override fun getString(resourceId: Int): String = context.getString(resourceId)
}
