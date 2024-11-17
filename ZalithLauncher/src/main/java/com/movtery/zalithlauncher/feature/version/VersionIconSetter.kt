package com.movtery.zalithlauncher.feature.version

import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.movtery.zalithlauncher.R

class VersionIconSetter(
    private val imageView: ImageView,
    private val version: Version
) {
    private val mContext = imageView.context

    fun start() {
        var isIconSet = false

        VersionsManager.getVersionIconFile(version).let { icon ->
            if (icon.exists()) {
                Glide.with(imageView)
                    .load(icon)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(imageView)
                isIconSet = true
            }
        }

        version.getVersionInfo()?.let { versionInfo ->
            versionInfo.loaderInfo.forEach { loaderInfo ->
                if (!isIconSet) {
                    getLoaderIcon(loaderInfo.name)?.let { icon ->
                        imageView.setImageDrawable(ContextCompat.getDrawable(mContext, icon))
                        isIconSet = true
                    }
                } else return@forEach
            }
        }

        if (!isIconSet) imageView.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_minecraft))
    }

    private fun getLoaderIcon(name: String): Int? {
        return if (name.equals("fabric", true)) R.drawable.ic_fabric
        else if (name.equals("forge", true)) R.drawable.ic_anvil
        else if (name.equals("quilt", true)) R.drawable.ic_quilt
        else if (name.equals("neoforge", true)) R.drawable.ic_neoforge
        else null
    }
}