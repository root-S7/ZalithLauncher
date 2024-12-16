package com.movtery.zalithlauncher.ui.subassembly.view

import android.app.Activity
import android.view.View
import android.widget.TextView
import com.getkeepsafe.taptargetview.TapTargetView
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.task.TaskExecutors
import com.movtery.zalithlauncher.utils.NewbieGuideUtils
import com.movtery.zalithlauncher.utils.file.FileTools.Companion.formatFileSize
import com.movtery.zalithlauncher.utils.platform.MemoryUtils
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.listener.IFxViewLifecycle
import com.petterp.floatingx.util.createFx
import com.petterp.floatingx.view.FxViewHolder
import java.util.Timer
import java.util.TimerTask

class GameMenuViewWrapper(
    private val activity: Activity,
    private val listener: View.OnClickListener
) {
    companion object {
        private const val TAG = "GameMenuView"
    }

    private var timer: Timer? = null
    private var showMemory: Boolean = false

    private val scopeFx by createFx {
        setLayout(R.layout.view_game_menu_window)
        setOnClickListener(0L, listener)
        setOnLongClickListener {
            showMemory = !showMemory
            AllSettings.gameMenuShowMemory.put(showMemory).save()
            setShowMemory()
            true
        }
        setEnableEdgeAdsorption(false)
        addViewLifecycle(object : IFxViewLifecycle {
            override fun initView(holder: FxViewHolder) {
                holder.view.alpha = AllSettings.gameMenuAlpha.getValue().toFloat() / 100f
                showMemory = AllSettings.gameMenuShowMemory.getValue()

                holder.getView<TextView>(R.id.memory_text).apply {
                    updateMemoryText(this)
                }

                startNewbieGuide(holder.view)
            }

            override fun detached(view: View) {
                cancelMemoryTimer()
            }
        })
        setGravity(getCurrentGravity())
        build().toControl(activity)
    }

    private fun startNewbieGuide(mainView: View) {
        if (NewbieGuideUtils.showOnlyOne(TAG)) return
        TapTargetView.showFor(
            activity,
            NewbieGuideUtils.getSimpleTarget(activity, mainView,
                activity.getString(R.string.setting_category_game_menu),
                activity.getString(R.string.newbie_guide_game_menu)
            )
        )
    }

    fun setVisibility(visible: Boolean) {
        if (visible) {
            setShowMemory()
            scopeFx.show()
        } else {
            scopeFx.hide()
            cancelMemoryTimer()
        }
    }

    private fun setShowMemory() {
        scopeFx.getView()?.findViewById<TextView>(R.id.memory_text)?.apply {
            updateMemoryText(this)
        }
    }

    private fun updateMemoryText(memoryText: TextView) {
        cancelMemoryTimer()

        memoryText.apply {
            visibility = if (showMemory) {
                timer = Timer()
                timer?.schedule(object : TimerTask() {
                    override fun run() {
                        val string =
                            "${AllSettings.gameMenuMemoryText.getValue()} ${formatFileSize(MemoryUtils.getUsedDeviceMemory(activity))}/${
                                formatFileSize(MemoryUtils.getTotalDeviceMemory(activity))
                            }".trim()
                        TaskExecutors.runInUIThread { text = string }
                    }
                }, 0, 2000)
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun cancelMemoryTimer() {
        timer?.cancel()
        timer = null
    }

    private fun getCurrentGravity(): FxGravity {
        return when(AllSettings.gameMenuLocation.getValue()) {
            "left_or_top" -> FxGravity.LEFT_OR_TOP
            "left_or_bottom" -> FxGravity.LEFT_OR_BOTTOM
            "right_or_top" -> FxGravity.RIGHT_OR_TOP
            "right_or_bottom" -> FxGravity.RIGHT_OR_BOTTOM
            "top_or_center" -> FxGravity.TOP_OR_CENTER
            "bottom_or_center" -> FxGravity.BOTTOM_OR_CENTER
            "center" -> FxGravity.CENTER
            else -> FxGravity.CENTER
        }
    }
}