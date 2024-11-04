package com.movtery.zalithlauncher.ui.fragment.download

import androidx.fragment.app.Fragment
import com.movtery.zalithlauncher.feature.download.InfoAdapter
import com.movtery.zalithlauncher.feature.download.enums.Classify
import com.movtery.zalithlauncher.feature.download.utils.CategoryUtils

class ModPackDownloadFragment() : AbstractResourceDownloadFragment(
    1,
    Classify.MODPACK,
    CategoryUtils.getModPackCategory(),
    true
) {
    constructor(parentFragment: Fragment): this() {
        this.mInfoAdapter = InfoAdapter(parentFragment, this, null)
    }
}