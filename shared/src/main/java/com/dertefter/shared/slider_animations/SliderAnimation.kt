package com.dertefter.shared.slider_animations


interface SliderAnimation {

    var onAnimationEnd: (() -> Unit)?

    var isOpen: Boolean

    fun startAnimation(isOpen: Boolean, color: Int, onEnd: () -> Unit)
}
