package com.movtery.zalithlauncher.feature.download

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.flexbox.FlexboxLayout
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.ItemDownloadInfoBinding
import com.movtery.zalithlauncher.event.value.DownloadRecyclerEnableEvent
import com.movtery.zalithlauncher.feature.download.enums.Platform
import com.movtery.zalithlauncher.feature.download.item.InfoItem
import com.movtery.zalithlauncher.feature.download.item.ModInfoItem
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.fragment.DownloadModFragment
import com.movtery.zalithlauncher.utils.NumberWithUnits
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.stringutils.StringUtils
import net.kdt.pojavlaunch.Tools
import org.greenrobot.eventbus.EventBus
import org.jackhuang.hmcl.ui.versions.ModTranslations
import java.io.File
import java.util.Collections
import java.util.Locale
import java.util.StringJoiner
import java.util.TimeZone
import java.util.WeakHashMap

class InfoAdapter(
    private val parentFragment: Fragment?,
    private val targetPath: File?,
    private val listener: CallSearchListener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mViewHolderSet: MutableSet<ViewHolder> = Collections.newSetFromMap(WeakHashMap())
    private var mItems: MutableList<InfoItem> = ArrayList()

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        val view: View
        when (viewType) {
            VIEW_TYPE_MOD_ITEM -> {
                // Create a new view, which defines the UI of the list item
                return ViewHolder(ItemDownloadInfoBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false))
            }

            VIEW_TYPE_LOADING -> {
                // Create a new view, which is actually just the progress bar
                view = layoutInflater.inflate(R.layout.view_loading, viewGroup, false)
                return LoadingViewHolder(view)
            }

            else -> throw RuntimeException("Unimplemented view type!")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_MOD_ITEM -> (holder as ViewHolder).setStateLimited(mItems[position])
            VIEW_TYPE_LOADING -> listener.loadMoreResult()
            else -> throw RuntimeException("Unimplemented view type!")
        }
    }

    override fun getItemCount(): Int {
        if (listener.isLastPage() || mItems.isEmpty()) return mItems.size
        return mItems.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position < mItems.size) return VIEW_TYPE_MOD_ITEM
        return VIEW_TYPE_LOADING
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(item: List<InfoItem>) {
        this.mItems.clear()
        this.mItems.addAll(item)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemDownloadInfoBinding) : RecyclerView.ViewHolder(binding.root) {
        private val mContext = binding.root.context
        private var item: InfoItem? = null

        init {
            mViewHolderSet.add(this)
        }

        @SuppressLint("CheckResult")
        fun setStateLimited(item: InfoItem) {
            this.item = item
            val mod = ModTranslations.getTranslationsByRepositoryType(item.classify)
                .getModByCurseForgeId(item.slug)

            binding.apply {
                parentFragment?.let { fragment ->
                    root.setOnClickListener {
                        EventBus.getDefault().post(DownloadRecyclerEnableEvent(false))

                        val infoViewModel = ViewModelProvider(fragment.requireActivity())[InfoViewModel::class.java]
                        infoViewModel.infoItem = item.copy()
                        infoViewModel.platformHelper = item.platform.helper.copy()
                        infoViewModel.targetPath = targetPath

                        ZHTools.addFragment(fragment, DownloadModFragment::class.java, DownloadModFragment.TAG, null)
                    }
                }

                titleTextview.text =
                    if (ZHTools.areaChecks("zh")) {
                        mod?.displayName ?: item.title
                    } else {
                        item.title
                    }
                descriptionTextview.text = item.description
                platformImageview.setImageDrawable(getPlatformIcon(item.platform))
                //设置类别
                categoriesLayout.removeAllViews()
                item.category.forEach { item ->
                    addCategoryView(categoriesLayout, mContext.getString(item.resNameID))
                }
                //设置标签
                tagsLayout.removeAllViews()

                val downloadCount = NumberWithUnits.formatNumberWithUnit(item.downloadCount, ZHTools.isEnglish(mContext))
                tagsLayout.addView(getTagTextView(R.string.download_info_downloads, downloadCount))

                item.author?.let {
                    val authorSJ = StringJoiner(", ")
                    for (s in it) {
                        authorSJ.add(s)
                    }
                    tagsLayout.addView(getTagTextView(R.string.download_info_author, authorSJ.toString()))
                }

                tagsLayout.addView(getTagTextView(R.string.download_info_date, StringUtils.formatDate(item.uploadDate, Locale.getDefault(), TimeZone.getDefault())))
                if (item is ModInfoItem) {
                    val modloaderSJ = StringJoiner(", ")
                    for (s in item.modloaders) {
                        modloaderSJ.add(s.loaderName)
                    }
                    val modloaderText = if (modloaderSJ.length() > 0) modloaderSJ.toString()
                    else mContext.getString(R.string.generic_unknown)
                    tagsLayout.addView(getTagTextView(R.string.download_info_modloader, modloaderText))
                }

                item.iconUrl?.apply {
                    Glide.with(mContext).load(this).apply {
                        if (!AllSettings.resourceImageCache) diskCacheStrategy(DiskCacheStrategy.NONE)
                    }.into(thumbnailImageview)
                }
            }
            binding.tagsLayout
        }

        private fun getPlatformIcon(platform: Platform): Drawable? {
            return when (platform) {
                Platform.MODRINTH -> ContextCompat.getDrawable(mContext, R.drawable.ic_modrinth)
                Platform.CURSEFORGE -> ContextCompat.getDrawable(mContext, R.drawable.ic_curseforge)
            }
        }

        private fun addCategoryView(layout: FlexboxLayout, text: String) {
            val inflater = LayoutInflater.from(mContext)
            val textView = inflater.inflate(R.layout.item_mod_category_textview, layout, false) as TextView
            textView.text = text
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.dpToPx(9F))

            layout.addView(textView)
        }

        private fun getTagTextView(string: Int, value: String): TextView {
            val textView = TextView(mContext)
            textView.text = StringUtils.insertSpace(mContext.getString(string), value)
            val layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 0, Tools.dpToPx(10f).toInt(), 0)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.dpToPx(9F))
            textView.layoutParams = layoutParams
            return textView
        }
    }

    /**
     * The view holder used to hold the progress bar at the end of the list
     */
    private class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)

    /**
     * @see com.movtery.zalithlauncher.ui.fragment.download.AbstractResourceDownloadFragment
     */
    interface CallSearchListener {
        /**
         * 用于判定当前搜索结果是否为最后一页
         * 如果是最后一页，那么将不再展示加载视图，也不会请求搜索更多结果
         */
        fun isLastPage(): Boolean

        /**
         * 请求加载更多结果
         */
        fun loadMoreResult()
    }

    companion object {
        private const val VIEW_TYPE_MOD_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }
}
