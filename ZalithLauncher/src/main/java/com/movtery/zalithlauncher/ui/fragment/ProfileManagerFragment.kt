package com.movtery.zalithlauncher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.movtery.anim.AnimPlayer
import com.movtery.anim.animations.Animations
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.FragmentProfileManagerBinding
import com.movtery.zalithlauncher.event.single.RefreshVersionSpinnerEvent
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathManager.Companion.currentProfile
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.dialog.TipDialog
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.file.FileTools.Companion.mkdirs
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.fragments.ProfileEditorFragment
import net.kdt.pojavlaunch.profiles.ProfileIconCache
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles
import org.greenrobot.eventbus.EventBus
import java.io.File

class ProfileManagerFragment : FragmentWithAnim(R.layout.fragment_profile_manager) {
    companion object {
        const val TAG: String = "ProfileManagerFragment"
    }

    private lateinit var binding: FragmentProfileManagerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileManagerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val gameDirPath = ZHTools.getGameDirPath(LauncherProfiles.getCurrentProfile().gameDir)

        binding.apply {
            shortcutsMods.setOnClickListener {
                val modsPath = File(gameDirPath, "/mods")
                if (!modsPath.exists()) {
                    mkdirs(modsPath)
                }

                val bundle = Bundle()
                bundle.putString(ModsFragment.BUNDLE_ROOT_PATH, modsPath.absolutePath)
                ZHTools.swapFragmentWithAnim(this@ProfileManagerFragment, ModsFragment::class.java, ModsFragment.TAG, bundle)
            }

            instancePath.setOnClickListener { swapFilesFragment(gameDirPath, gameDirPath) }
            resourcePath.setOnClickListener { swapFilesFragment(gameDirPath, File(gameDirPath, "/resourcepacks")) }
            worldPath.setOnClickListener { swapFilesFragment(gameDirPath, File(gameDirPath, "/saves")) }
            logsPath.setOnClickListener { swapFilesFragment(gameDirPath, File(gameDirPath, "/logs")) }
            crashReportPath.setOnClickListener { swapFilesFragment(gameDirPath, File(gameDirPath, "/crash-reports")) }

            profileEdit.setOnClickListener { ZHTools.swapFragmentWithAnim(this@ProfileManagerFragment, ProfileEditorFragment::class.java, ProfileEditorFragment.TAG, null) }
            profileDelete.setOnClickListener {
                val activity = requireActivity()
                TipDialog.Builder(activity)
                    .setTitle(R.string.generic_warning)
                    .setMessage(R.string.profile_manager_delete_message)
                    .setConfirmClickListener {
                        if (LauncherProfiles.mainProfileJson.profiles.size > 1) {
                            ProfileIconCache.dropIcon(AllSettings.currentProfile!!)
                            LauncherProfiles.mainProfileJson.profiles.remove(AllSettings.currentProfile)
                            LauncherProfiles.write(currentProfile)
                            EventBus.getDefault().post(RefreshVersionSpinnerEvent())
                        }
                        Tools.backToMainMenu(activity)
                    }
                    .buildDialog()
            }
        }
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

    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(shortcutsLayout, Animations.BounceInRight))
                .apply(AnimPlayer.Entry(moddedLayout, Animations.BounceInLeft))
        }
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(shortcutsLayout, Animations.FadeOutLeft))
                .apply(AnimPlayer.Entry(moddedLayout, Animations.FadeOutRight))
        }
    }
}
