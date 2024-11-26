package com.movtery.zalithlauncher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.movtery.anim.AnimPlayer
import com.movtery.anim.animations.Animations
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.FragmentVersionsListBinding
import com.movtery.zalithlauncher.event.single.RefreshVersionsEvent
import com.movtery.zalithlauncher.event.sticky.FileSelectorEvent
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathJsonObject
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathManager.Companion.save
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.version.Version
import com.movtery.zalithlauncher.feature.version.VersionsManager
import com.movtery.zalithlauncher.task.TaskExecutors
import com.movtery.zalithlauncher.ui.dialog.EditTextDialog
import com.movtery.zalithlauncher.ui.subassembly.customprofilepath.ProfileItem
import com.movtery.zalithlauncher.ui.subassembly.customprofilepath.ProfilePathAdapter
import com.movtery.zalithlauncher.ui.subassembly.version.VersionAdapter
import com.movtery.zalithlauncher.utils.PathAndUrlManager
import com.movtery.zalithlauncher.utils.StoragePermissionsUtils
import com.movtery.zalithlauncher.utils.ZHTools
import net.kdt.pojavlaunch.Tools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.util.UUID

class VersionsListFragment : FragmentWithAnim(R.layout.fragment_versions_list) {
    companion object {
        const val TAG: String = "VersionsListFragment"
    }

    private lateinit var binding: FragmentVersionsListBinding
    private val profilePathData: MutableList<ProfileItem> = ArrayList()
    private var versionsAdapter: VersionAdapter? = null
    private var profilePathAdapter: ProfilePathAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val selectorEvent = EventBus.getDefault().getStickyEvent(FileSelectorEvent::class.java)

        selectorEvent?.let { event ->
            event.path?.let { path ->
                if (path.isNotEmpty() && !isAddedPath(path)) {
                    EditTextDialog.Builder(requireContext())
                        .setTitle(R.string.profiles_path_create_new_title)
                        .setAsRequired()
                        .setConfirmListener { editBox, _ ->
                            val string = editBox.text.toString()

                            profilePathData.add(ProfileItem(UUID.randomUUID().toString(), string, path))
                            val nomediaFile = File(path, ".nomedia")
                            if (!nomediaFile.exists()) nomediaFile.createNewFile()

                            save(this.profilePathData)
                            refresh()
                            true
                        }.buildDialog()
                }
            }
            EventBus.getDefault().removeStickyEvent(event)
        }
        binding = FragmentVersionsListBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            versionsAdapter = VersionAdapter(this@VersionsListFragment, object : VersionAdapter.OnVersionItemClickListener {
                override fun onVersionClick(version: Version) {
                    VersionsManager.saveCurrentVersion(version.getVersionName())
                    Tools.backToMainMenu(requireActivity())
                }

                override fun onCreateVersion() {
                    ZHTools.swapFragmentWithAnim(this@VersionsListFragment, VersionSelectorFragment::class.java, VersionSelectorFragment.TAG, null)
                }
            })
            versions.apply {
                layoutAnimation = LayoutAnimationController(
                    AnimationUtils.loadAnimation(view.context, R.anim.fade_downwards)
                )
                layoutManager = LinearLayoutManager(requireContext())
                this.adapter = versionsAdapter
            }

            profilePathAdapter = ProfilePathAdapter(this@VersionsListFragment, profilesPath)
            profilesPath.apply {
                layoutAnimation = LayoutAnimationController(
                    AnimationUtils.loadAnimation(view.context, R.anim.fade_downwards)
                )
                layoutManager = LinearLayoutManager(requireContext())
                this.adapter = profilePathAdapter
            }

            refreshButton.setOnClickListener { refresh() }
            createPathButton.setOnClickListener {
                StoragePermissionsUtils.checkPermissions(requireActivity(), R.string.profiles_path_create_new) {
                    val bundle = Bundle()
                    bundle.putBoolean(FilesFragment.BUNDLE_SELECT_FOLDER_MODE, true)
                    bundle.putBoolean(FilesFragment.BUNDLE_SHOW_FILE, false)
                    bundle.putBoolean(FilesFragment.BUNDLE_REMOVE_LOCK_PATH, false)
                    ZHTools.swapFragmentWithAnim(this@VersionsListFragment, FilesFragment::class.java, FilesFragment.TAG, bundle)
                }
            }
            returnButton.setOnClickListener { ZHTools.onBackPressed(requireActivity()) }
        }

        refresh()
    }

    private fun refresh() {
        VersionsManager.refresh()

        profilePathData.clear()
        profilePathData.add(ProfileItem("default", getString(R.string.profiles_path_default), PathAndUrlManager.DIR_GAME_HOME))

        runCatching {
            val json: String
            if (PathAndUrlManager.FILE_PROFILE_PATH.exists()) {
                json = Tools.read(PathAndUrlManager.FILE_PROFILE_PATH)
                if (json.isEmpty()) return@runCatching
            } else return@runCatching

            val jsonObject = JsonParser.parseString(json).asJsonObject

            for (key in jsonObject.keySet()) {
                val profilePathId = Gson().fromJson(jsonObject[key], ProfilePathJsonObject::class.java)
                val item = ProfileItem(key, profilePathId.title, profilePathId.path)
                profilePathData.add(item)
            }
        }.getOrElse { e -> Logging.e("refresh profile data", Tools.printToString(e)) }

        profilePathAdapter?.updateData(this.profilePathData)
    }

    private fun isAddedPath(path: String): Boolean {
        for (mDatum in this.profilePathData) {
            if (mDatum.path == path) return true
        }
        return false
    }

    private fun refreshVersions() {
        versionsAdapter?.let {
            val versions = VersionsManager.getVersions()
            versions.add(null)
            it.refreshVersions(versions)
            binding.versions.scheduleLayoutAnimation()
        }
    }

    @Subscribe
    fun event(event: RefreshVersionsEvent) {
        TaskExecutors.runInUIThread { refreshVersions() }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        versionsAdapter?.let {
            //为了避免界面初始化的时候连续刷新两次（OnViewCreated最后会刷新一次）
            //这里需要确保VersionAdapter已经完成初始化，才会刷新
            VersionsManager.refresh()
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(versionLayout, Animations.BounceInDown))
                .apply(AnimPlayer.Entry(operateLayout, Animations.BounceInLeft))
                .apply(AnimPlayer.Entry(operateView, Animations.FadeInLeft))
        }
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(versionLayout, Animations.FadeOutUp))
                .apply(AnimPlayer.Entry(operateLayout, Animations.FadeOutRight))
        }
    }
}
