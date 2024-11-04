package com.movtery.zalithlauncher.ui.fragment.download

import androidx.fragment.app.Fragment
import com.movtery.zalithlauncher.feature.download.InfoAdapter
import com.movtery.zalithlauncher.feature.download.enums.Classify
import com.movtery.zalithlauncher.feature.download.utils.CategoryUtils
import java.io.File

class ResourcePackDownloadFragment() : AbstractResourceDownloadFragment(
    2,
    Classify.RESOURCE_PACK,
    CategoryUtils.getResourcePackCategory(),
    false
) {
    constructor(parentFragment: Fragment): this() {
        this.mInfoAdapter = InfoAdapter(parentFragment, this, File(sGameDir, "/resourcepacks"))
    }
}