package com.movtery.zalithlauncher.ui.fragment.download

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.movtery.anim.AnimPlayer
import com.movtery.anim.animations.Animations
import com.movtery.zalithlauncher.event.value.DownloadCheckSearchEvent
import com.movtery.zalithlauncher.event.value.DownloadRecyclerEnableEvent
import com.movtery.zalithlauncher.feature.download.Filters
import com.movtery.zalithlauncher.feature.download.InfoAdapter
import com.movtery.zalithlauncher.feature.download.enums.Category
import com.movtery.zalithlauncher.feature.download.enums.Classify
import com.movtery.zalithlauncher.feature.download.enums.ModLoader
import com.movtery.zalithlauncher.feature.download.enums.Platform
import com.movtery.zalithlauncher.feature.download.enums.Sort
import com.movtery.zalithlauncher.feature.download.utils.ModLoaderUtils
import com.movtery.zalithlauncher.feature.download.utils.SortUtils
import com.movtery.zalithlauncher.ui.dialog.SelectVersionDialog
import com.movtery.zalithlauncher.ui.fragment.FragmentWithAnim
import com.movtery.zalithlauncher.ui.subassembly.adapter.ObjectSpinnerAdapter
import com.movtery.zalithlauncher.ui.subassembly.versionlist.VersionSelectedListener
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.anim.AnimUtils.Companion.setVisibilityAnim
import com.skydoves.powerspinner.PowerSpinnerView
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.databinding.FragmentDownloadResourceBinding
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File

abstract class AbstractResourceDownloadFragment(
    private val index: Int,
    private val classify: Classify,
    private val categoryList: List<Category>,
    private val showModloader: Boolean
) : FragmentWithAnim(R.layout.fragment_download_resource), InfoAdapter.SearchResultCallback {
    protected lateinit var mInfoAdapter: InfoAdapter

    private lateinit var binding: FragmentDownloadResourceBinding
    private lateinit var mPlatformAdapter: ObjectSpinnerAdapter<Platform>
    private lateinit var mSortAdapter: ObjectSpinnerAdapter<Sort>
    private lateinit var mCategoryAdapter: ObjectSpinnerAdapter<Category>
    private lateinit var mModLoaderAdapter: ObjectSpinnerAdapter<ModLoader>
    private var mCurrentPlatform: Platform = Platform.CURSEFORGE
    private val mFilters: Filters = Filters()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadResourceBinding.inflate(layoutInflater)

        mPlatformAdapter = ObjectSpinnerAdapter(binding.platformSpinner) { platform -> platform.pName }
        mSortAdapter = ObjectSpinnerAdapter(binding.sortSpinner) { sort -> getString(sort.resNameID) }
        mCategoryAdapter = ObjectSpinnerAdapter(binding.categorySpinner) { category -> getString(category.resNameID) }
        mModLoaderAdapter = ObjectSpinnerAdapter(binding.modloaderSpinner) { modloader ->
            if (modloader == ModLoader.ALL) getString(R.string.generic_all)
            else modloader.loaderName
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                layoutAnimation = LayoutAnimationController(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.fade_downwards)
                )
                addOnScrollListener(object : OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        val lm = layoutManager as LinearLayoutManager
                        val lastPosition = lm.findLastVisibleItemPosition()
                        setVisibilityAnim(backToTop, lastPosition >= 12)
                    }
                })
                adapter = mInfoAdapter
            }

            backToTop.setOnClickListener { recyclerView.smoothScrollToPosition(0) }

            searchView.setOnClickListener { search() }
            nameEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    mFilters.name = s?.toString() ?: ""
                }
            })
            nameEdit.setOnEditorActionListener { _, _, _ ->
                search()
                nameEdit.clearFocus()
                false
            }

            // 打开版本选择弹窗
            mcVersionButton.setOnClickListener {
                val selectVersionDialog = SelectVersionDialog(requireContext())
                selectVersionDialog.setOnVersionSelectedListener(object : VersionSelectedListener() {
                    override fun onVersionSelected(version: String?) {
                        selectedMcVersionView.text = version
                        mFilters.mcVersion = version
                        selectVersionDialog.dismiss()
                    }
                })
                selectVersionDialog.show()
            }
        }

        // 初始化 Spinner
        mPlatformAdapter.setItems(listOf(Platform.CURSEFORGE, Platform.MODRINTH))
        mSortAdapter.setItems(SortUtils.getSortList())
        mCategoryAdapter.setItems(categoryList)
        mModLoaderAdapter.setItems(ModLoaderUtils.getModLoaderList())

        binding.apply {
            platformSpinner.setSpinnerAdapter(mPlatformAdapter)
            platformSpinner.selectItemByIndex(0)
            setSpinnerListener<Platform>(platformSpinner) {
                mCurrentPlatform = it
                checkSearch()
            }

            sortSpinner.setSpinnerAdapter(mSortAdapter)
            sortSpinner.selectItemByIndex(0)
            setSpinnerListener<Sort>(sortSpinner) { mFilters.sort = it }

            categorySpinner.setSpinnerAdapter(mCategoryAdapter)
            categorySpinner.selectItemByIndex(0)
            setSpinnerListener<Category>(binding.categorySpinner) { mFilters.category = it }

            modloaderSpinner.setSpinnerAdapter(mModLoaderAdapter)
            modloaderSpinner.selectItemByIndex(0)
            setSpinnerListener<ModLoader>(modloaderSpinner) {
                mFilters.modloader = it.takeIf { loader -> loader != ModLoader.ALL }
            }

            reset.setOnClickListener {
                nameEdit.setText("")
                platformSpinner.selectItemByIndex(0)
                sortSpinner.selectItemByIndex(0)
                categorySpinner.selectItemByIndex(0)
                modloaderSpinner.selectItemByIndex(0)
                binding.selectedMcVersionView.text = null
                mFilters.mcVersion = null
            }

            returnButton.setOnClickListener { ZHTools.onBackPressed(requireActivity()) }
        }

        showModLoader()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onPause() {
        closeSpinner()
        super.onPause()
    }

    override fun onSearchFinished() {
        binding.apply {
            setStatusText(false)
            setLoadingLayout(false)
            setRecyclerView(true)
        }
    }

    override fun onSearchError(error: Int) {
        binding.apply {
            statusText.text = when (error) {
                InfoAdapter.SearchResultCallback.ERROR_INTERNAL -> getString(R.string.download_search_failed)
                InfoAdapter.SearchResultCallback.ERROR_PLATFORM_NOT_SUPPORTED -> getString(R.string.download_search_platform_not_supported)
                else -> getString(R.string.download_search_no_result)
            }
        }
        setLoadingLayout(false)
        setRecyclerView(false)
        setStatusText(true)
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(operateLayout, Animations.BounceInRight))
                .apply(AnimPlayer.Entry(downloadLayout, Animations.BounceInDown))
        }
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(operateLayout, Animations.FadeOutLeft))
                .apply(AnimPlayer.Entry(downloadLayout, Animations.FadeOutUp))
        }
    }

    private fun setStatusText(shouldShow: Boolean) {
        setVisibilityAnim(binding.statusText, shouldShow)
    }

    private fun setLoadingLayout(shouldShow: Boolean) {
        setVisibilityAnim(binding.loadingLayout, shouldShow)
    }

    private fun setRecyclerView(shouldShow: Boolean) {
        binding.apply {
            recyclerView.visibility = if (shouldShow) View.VISIBLE else View.GONE
            if (shouldShow) recyclerView.scheduleLayoutAnimation()
        }
    }

    private fun <E> setSpinnerListener(spinnerView: PowerSpinnerView, func: (E) -> Unit) {
        spinnerView.setOnSpinnerItemSelectedListener<E> { _, _, _, newItem -> func(newItem) }
    }

    private fun closeSpinner() {
        binding.platformSpinner.dismiss()
        binding.sortSpinner.dismiss()
        binding.categorySpinner.dismiss()
        binding.modloaderSpinner.dismiss()
    }

    private fun showModLoader() {
        binding.apply {
            modloaderLayout.visibility = if (showModloader) View.VISIBLE else View.GONE
            if (showModloader) {
                modloaderSpinner.setSpinnerAdapter(mModLoaderAdapter)
                modloaderSpinner.selectItemByIndex(0)
            } else {
                mFilters.modloader = null
            }
        }
    }

    private fun search() {
        mCurrentPlatform.helper.currentClassify = classify
        mCurrentPlatform.helper.filters = mFilters
        setStatusText(false)
        setRecyclerView(false)
        setLoadingLayout(true)
        binding.apply {
            recyclerView.scrollToPosition(0)
        }
        mInfoAdapter.apply {
            setPlatform(mCurrentPlatform)
            performSearchQuery()
        }
    }

    private fun checkSearch() {
        if (mInfoAdapter.itemCount == 0 || mInfoAdapter.checkPlatform(mCurrentPlatform)) search()
    }

    @Subscribe
    fun event(event: DownloadRecyclerEnableEvent) {
        binding.recyclerView.isEnabled = event.enable
        closeSpinner()
    }

    @Subscribe
    fun event(event: DownloadCheckSearchEvent) {
        slideIn()
        if (event.index == index) checkSearch()
    }

    companion object {
        @JvmField
        val sGameDir: File = ZHTools.getGameDirPath(getDir())

        private fun getDir(): String? {
            var dir: String? = LauncherProfiles.getCurrentProfile().gameDir
            dir?.let {
                if (it.startsWith("./")) dir = it.removePrefix("./")
            }
            return dir
        }
    }
}