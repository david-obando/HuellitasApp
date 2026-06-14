package com.huellitas.huellitasapp.util

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout

class TouchableWrapper(context: Context) : FrameLayout(context) {

    var onTouchAction: ((Int) -> Unit)? = null

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Invoca el callback pasando la acción táctil (DOWN, MOVE, UP, etc.)
        onTouchAction?.invoke(event.action)
        return super.dispatchTouchEvent(event)
    }
}