package com.movtery.zalithlauncher.ui.fragment

import com.movtery.zalithlauncher.R
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils
import net.kdt.pojavlaunch.modloaders.ModloaderDownloadListener

class DownloadQuiltFragment : DownloadFabricLikeFragment(FabriclikeUtils.QUILT_UTILS, R.drawable.ic_quilt), ModloaderDownloadListener {
    companion object {
        const val TAG: String = "DownloadQuiltFragment"
    }
}