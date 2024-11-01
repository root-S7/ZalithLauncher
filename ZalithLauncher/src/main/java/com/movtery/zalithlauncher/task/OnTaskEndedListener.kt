package com.movtery.zalithlauncher.task

fun interface OnTaskEndedListener<V> {
    fun onEnded(result: V?)
}