package com.movtery.zalithlauncher.support.touch_controller

import android.view.MotionEvent
import android.view.View
import com.movtery.zalithlauncher.feature.log.Logging
import top.fifthlight.touchcontroller.proxy.client.LauncherSocketProxyClient
import top.fifthlight.touchcontroller.proxy.data.Offset
import top.fifthlight.touchcontroller.proxy.message.AddPointerMessage
import top.fifthlight.touchcontroller.proxy.message.ClearPointerMessage
import top.fifthlight.touchcontroller.proxy.message.RemovePointerMessage

/**
 * 单独在这里处理触点，为TouchController模组的控制代理提供信息
 */
object ContactHandler {
    private val pointerIdMap: MutableMap<Int, Int> = HashMap()
    private var nextPointerId = 1

    fun progressEvent(motionEvent: MotionEvent, touchView: View) {
        val proxy = ControllerProxy.getProxyClient() ?: return

        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> handlePointerDown(motionEvent, proxy, 0, touchView)
            MotionEvent.ACTION_POINTER_DOWN -> handlePointerDown(motionEvent, proxy, motionEvent.actionIndex, touchView)
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until motionEvent.pointerCount) {
                    val pointerId = pointerIdMap[motionEvent.getPointerId(i)] ?: 0
                    if (pointerId == 0) {
                        Logging.d("InGameEventProcessor", "PointerId is 0, skipping.")
                    }
                    trySendPointer(proxy, pointerId,
                        motionEvent.getX(i) / touchView.width,
                        motionEvent.getY(i) / touchView.height
                    )
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> clearPointer(proxy)
            MotionEvent.ACTION_POINTER_UP -> {
                val i = motionEvent.actionIndex
                val pointerId = pointerIdMap[motionEvent.getPointerId(i)] ?: 0
                if (pointerId == 0) {
                    Logging.d("InGameEventProcessor", "PointerId is 0, skipping.")
                } else {
                    pointerIdMap.remove(motionEvent.getPointerId(i))
                    proxy.trySend(RemovePointerMessage(pointerId))
                }
            }
        }
    }

    private fun handlePointerDown(motionEvent: MotionEvent, proxy: LauncherSocketProxyClient, index: Int, touchView: View) {
        val pointerId = nextPointerId++
        pointerIdMap[motionEvent.getPointerId(index)] = pointerId
        trySendPointer(proxy, pointerId,
            motionEvent.getX(index) / touchView.width,
            motionEvent.getY(index) / touchView.height
        )
    }

    private fun trySendPointer(proxy: LauncherSocketProxyClient, pointerId: Int, x: Float, y: Float) {
        proxy.trySend(AddPointerMessage(pointerId, Offset(x, y)))
    }

    private fun clearPointer(proxy: LauncherSocketProxyClient) {
        proxy.trySend(ClearPointerMessage)
        pointerIdMap.clear()
    }

    fun clearPointer() {
        ControllerProxy.getProxyClient()?.let { clearPointer(it) }
        pointerIdMap.clear()
    }
}