package com.movtery.zalithlauncher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.movtery.zalithlauncher.InfoCenter
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.FragmentLauncherEmptyMenuBinding
import com.movtery.zalithlauncher.databinding.FragmentLauncherMenuBinding
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper

class LauncherMenuFragment : BaseFragment(R.layout.fragment_launcher_menu), View.OnClickListener {
    private lateinit var binding: FragmentLauncherMenuBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLauncherMenuBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fragment = this
        binding.apply {
            aboutButton.text = InfoCenter.replaceName(requireActivity(), R.string.about_tab)
            aboutButton.setOnClickListener(fragment)
            customControlButton.setOnClickListener(fragment)
            openMainDirButton.setOnClickListener(fragment)
            installJarButton.setOnClickListener(fragment)
            installJarButton.setOnLongClickListener {
                runInstallerWithConfirmation(true)
                true
            }
            shareLogsButton.setOnClickListener(fragment)
        }
    }

    private fun runInstallerWithConfirmation(isCustomArgs: Boolean) {
        if (ProgressKeeper.getTaskCount() == 0) Tools.installMod(requireActivity(), isCustomArgs)
        else Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show()
    }

    override fun onClick(v: View?) {
        val fragment = this
        binding.apply {
            when (v) {
                aboutButton -> ZHTools.swapFragmentWithAnim(fragment, AboutFragment::class.java, AboutFragment.TAG, null)
                customControlButton -> ZHTools.swapFragmentWithAnim(fragment, ControlButtonFragment::class.java, ControlButtonFragment.TAG, null)
                openMainDirButton -> {
                    val bundle = Bundle()
                    bundle.putString(FilesFragment.BUNDLE_LIST_PATH, PathManager.DIR_GAME_HOME)
                    ZHTools.swapFragmentWithAnim(fragment, FilesFragment::class.java, FilesFragment.TAG, bundle)
                }
                installJarButton -> runInstallerWithConfirmation(false)
                shareLogsButton -> ZHTools.shareLogs(requireActivity())
                else -> {}
            }
        }
    }

    class EmptyMenuFragment() : BaseFragment(R.layout.fragment_launcher_empty_menu) {
        private lateinit var binding: FragmentLauncherEmptyMenuBinding
        private var mViewPager: ViewPager2? = null

        constructor(viewPager: ViewPager2) : this() {
            this.mViewPager = viewPager
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            binding = FragmentLauncherEmptyMenuBinding.inflate(layoutInflater)
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            binding.returnView.setOnClickListener {
                mViewPager?.setCurrentItem(0, true)
            }
        }
    }
}