package com.movtery.zalithlauncher.event.value

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * 使用这个事件进行添加Fragment，并由LauncherActivity接受并处理
 * 保证Fragment添加的时候，父Fragment一定是当前的Fragment
 * @see net.kdt.pojavlaunch.LauncherActivity
 */
class AddFragmentEvent(
    val fragmentClass: Class<out Fragment?>,
    val fragmentTag: String?,
    val bundle: Bundle?,
    val activityCallBack: ActivityCallBack?
) {
    /**
     * 对于当前Fragment的FragmentActivity的一些回调处理
     */
    fun interface ActivityCallBack {
        fun callBack(activity: FragmentActivity)
    }
}