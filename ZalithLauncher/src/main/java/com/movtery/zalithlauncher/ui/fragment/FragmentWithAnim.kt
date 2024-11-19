package com.movtery.zalithlauncher.ui.fragment

import androidx.fragment.app.Fragment
import com.movtery.anim.AnimPlayer
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.anim.SlideAnimation
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener

abstract class FragmentWithAnim : Fragment, SlideAnimation, TaskCountListener {
    private var animPlayer: AnimPlayer = AnimPlayer()
    private var mIsTaskRunning: Boolean = false

    constructor()

    constructor(contentLayoutId: Int) : super(contentLayoutId)

    override fun onStart() {
        super.onStart()
        slideIn()
        ProgressKeeper.addTaskCountListener(this)
    }

    override fun onStop() {
        super.onStop()
        ProgressKeeper.removeTaskCountListener(this)
    }

    override fun onUpdateTaskCount(taskCount: Int) {
        this.mIsTaskRunning = taskCount != 0
    }

    fun isTaskRunning() = mIsTaskRunning

    fun slideIn() {
        playAnimation { slideIn(it) }
    }

    fun slideOut() {
        playAnimation { slideOut(it) }
    }

    private fun playAnimation(animationAction: (AnimPlayer) -> Unit) {
        if (AllSettings.animation) {
            animPlayer.clearEntries()
            animPlayer.apply {
                animationAction(this)
                start()
            }
        }
    }
}
