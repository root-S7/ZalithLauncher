package com.movtery.zalithlauncher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.movtery.anim.AnimPlayer
import com.movtery.anim.animations.Animations
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.FragmentVersionManagerBinding
import com.movtery.zalithlauncher.feature.version.Version
import com.movtery.zalithlauncher.feature.version.VersionsManager
import com.movtery.zalithlauncher.task.Task
import com.movtery.zalithlauncher.task.TaskExecutors
import com.movtery.zalithlauncher.ui.dialog.TipDialog
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.file.FileDeletionHandler
import com.movtery.zalithlauncher.utils.file.FileTools.Companion.mkdirs
import net.kdt.pojavlaunch.Tools
import java.io.File

class VersionManagerFragment : FragmentWithAnim(R.layout.fragment_version_manager), View.OnClickListener {
    companion object {
        const val TAG: String = "VersionManagerFragment"
    }

    private lateinit var binding: FragmentVersionManagerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVersionManagerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val fragment = this
        binding.apply {
            shortcutsMods.setOnClickListener(fragment)
            gamePath.setOnClickListener(fragment)
            resourcePath.setOnClickListener(fragment)
            worldPath.setOnClickListener(fragment)
            shaderPath.setOnClickListener(fragment)
            logsPath.setOnClickListener(fragment)
            crashReportPath.setOnClickListener(fragment)
            versionEdit.setOnClickListener(fragment)
            versionRename.setOnClickListener(fragment)
            versionCopy.setOnClickListener(fragment)
            versionDelete.setOnClickListener(fragment)
        }
    }

    override fun onResume() {
        super.onResume()
        VersionsManager.refresh()
    }

    private fun swapFilesFragment(lockPath: File, listPath: File) {
        if (!lockPath.exists()) {
            mkdirs(lockPath)
        }
        if (!listPath.exists()) {
            mkdirs(listPath)
        }

        val bundle = Bundle()
        bundle.putString(FilesFragment.BUNDLE_LOCK_PATH, lockPath.absolutePath)
        bundle.putString(FilesFragment.BUNDLE_LIST_PATH, listPath.absolutePath)
        bundle.putBoolean(FilesFragment.BUNDLE_QUICK_ACCESS_PATHS, false)

        ZHTools.swapFragmentWithAnim(this, FilesFragment::class.java, FilesFragment.TAG, bundle)
    }

    override fun onClick(v: View) {
        val activity = requireActivity()
        val version: Version = VersionsManager.getCurrentVersion() ?: throw RuntimeException("There is no installed version")
        val gameDirPath = version.getGameDir()

        binding.apply {
            when (v) {
                shortcutsMods -> {
                    val modsPath = File(gameDirPath, "/mods")
                    if (!modsPath.exists()) {
                        mkdirs(modsPath)
                    }

                    val bundle = Bundle()
                    bundle.putString(ModsFragment.BUNDLE_ROOT_PATH, modsPath.absolutePath)
                    ZHTools.swapFragmentWithAnim(this@VersionManagerFragment, ModsFragment::class.java, ModsFragment.TAG, bundle)
                }
                gamePath -> swapFilesFragment(gameDirPath, gameDirPath)
                resourcePath -> swapFilesFragment(gameDirPath, File(gameDirPath, "/resourcepacks"))
                worldPath -> swapFilesFragment(gameDirPath, File(gameDirPath, "/saves"))
                shaderPath -> swapFilesFragment(gameDirPath, File(gameDirPath, "/shaderpacks"))
                logsPath -> swapFilesFragment(gameDirPath, File(gameDirPath, "/logs"))
                crashReportPath -> swapFilesFragment(gameDirPath, File(gameDirPath, "/crash-reports"))

                versionEdit -> ZHTools.swapFragmentWithAnim(this@VersionManagerFragment, VersionConfigFragment::class.java, VersionConfigFragment.TAG, null)
                versionRename -> {
                    VersionsManager.openRenameDialog(activity, version) {
                        Tools.backToMainMenu(activity) //重命名前，为了不出现问题，需要退出当前Fragment
                    }
                }
                versionCopy -> VersionsManager.openCopyDialog(activity, version)
                versionDelete -> {
                    TipDialog.Builder(activity)
                        .setTitle(R.string.generic_warning)
                        .setMessage(R.string.version_manager_delete_tip)
                        .setConfirmClickListener {
                            VersionsManager.getCurrentVersion()?.let {
                                FileDeletionHandler(
                                    activity,
                                    listOf(it.getVersionPath()),
                                    Task.runTask {
                                        VersionsManager.refresh()
                                    }.ended(TaskExecutors.getAndroidUI()) {
                                        Tools.backToMainMenu(activity)
                                    }
                                ).start()
                            }
                        }
                        .buildDialog()
                }
                else -> {}
            }
        }
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(shortcutsLayout, Animations.BounceInRight))
                .apply(AnimPlayer.Entry(editLayout, Animations.BounceInLeft))
        }
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(shortcutsLayout, Animations.FadeOutLeft))
                .apply(AnimPlayer.Entry(editLayout, Animations.FadeOutRight))
        }
    }
}
