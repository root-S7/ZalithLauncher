package com.movtery.zalithlauncher.support.touch_controller

import android.util.Log
import android.util.SparseIntArray
import android.view.MotionEvent
import android.view.View
import top.fifthlight.touchcontroller.proxy.client.LauncherSocketProxyClient
import top.fifthlight.touchcontroller.proxy.data.Offset
import top.fifthlight.touchcontroller.proxy.message.AddPointerMessage
import top.fifthlight.touchcontroller.proxy.message.ClearPointerMessage
import top.fifthlight.touchcontroller.proxy.message.RemovePointerMessage

/**
 * 单独在这里处理触点，为TouchController模组的控制代理提供信息
 */
class ContactHandler {
    fun progressEvent(motionEvent: MotionEvent, touchView: View) {
        ControllerProxy.getProxyClient()?.let { proxy ->
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val pointerId: Int = nextPointerId++
                    pointerIdMap.put(motionEvent.getPointerId(0), pointerId)
                    proxy.trySend(
                        AddPointerMessage(pointerId, Offset(
                            motionEvent.getX(0) / touchView.width,
                            motionEvent.getY(0) / touchView.height
                        ))
                    )
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    val pointerId = nextPointerId++
                    val i = motionEvent.actionIndex
                    pointerIdMap.put(motionEvent.getPointerId(i), pointerId)
                    proxy.trySend(
                        AddPointerMessage(pointerId, Offset(
                            motionEvent.getX(i) / touchView.width,
                            motionEvent.getY(i) / touchView.height
                        ))
                    )
                }
                MotionEvent.ACTION_MOVE -> {
                    for (i in 0 until motionEvent.pointerCount) {
                        val pointerId = pointerIdMap[motionEvent.getPointerId(i)]
                        if (pointerId == 0) {
                            Log.d("InGameEventProcessor", "Move pointerId is 0")
                        }
                        proxy.trySend(
                            AddPointerMessage(pointerId, Offset(
                                motionEvent.getX(i) / touchView.width,
                                motionEvent.getY(i) / touchView.height
                            ))
                        )
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> clearPointer(proxy)
                MotionEvent.ACTION_POINTER_UP -> {
                    val i = motionEvent.actionIndex
                    val pointerId = pointerIdMap[motionEvent.getPointerId(i)]
                    if (pointerId == 0) {
                        Log.d("InGameEventProcessor", "Remove pointerId is 0")
                    } else {
                        pointerIdMap.delete(pointerId)
                        proxy.trySend(RemovePointerMessage(pointerId))
                    }
                }
                else -> {}
            }
        }
    }

    private fun clearPointer(proxy: LauncherSocketProxyClient) {
        proxy.trySend(ClearPointerMessage)
        pointerIdMap.clear()
    }

    fun clearPointer() {
        ControllerProxy.getProxyClient()?.let { clearPointer(it) }
        pointerIdMap.clear()
    }

    companion object {
        private val pointerIdMap: SparseIntArray = SparseIntArray()
        private var nextPointerId = 1
    }
}