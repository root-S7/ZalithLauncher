package com.movtery.zalithlauncher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.movtery.anim.AnimPlayer
import com.movtery.anim.animations.Animations
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.FragmentDownloadBinding
import com.movtery.zalithlauncher.event.value.DownloadPageSwapEvent
import com.movtery.zalithlauncher.event.value.InDownloadFragmentEvent
import com.movtery.zalithlauncher.ui.fragment.download.ModDownloadFragment
import com.movtery.zalithlauncher.ui.fragment.download.ModPackDownloadFragment
import com.movtery.zalithlauncher.ui.fragment.download.ResourcePackDownloadFragment
import com.movtery.zalithlauncher.ui.fragment.download.ShaderPackDownloadFragment
import com.movtery.zalithlauncher.ui.fragment.download.WorldDownloadFragment
import org.greenrobot.eventbus.EventBus

class DownloadFragment : FragmentWithAnim(R.layout.fragment_download) {
    companion object {
        const val TAG = "DownloadFragment"
    }

    private lateinit var binding: FragmentDownloadBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViewPager()

        binding.classifyTab.observeIndexChange { _, toIndex, reselect, _ ->
            if (reselect) return@observeIndexChange
            binding.downloadViewpager.setCurrentItem(toIndex, false)
        }
    }

    private fun initViewPager() {
        binding.downloadViewpager.apply {
            adapter = ViewPagerAdapter(this@DownloadFragment)
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            offscreenPageLimit = 1
            isUserInputEnabled = false
            registerOnPageChangeCallback(object: OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    onFragmentSelect(position)
                    EventBus.getDefault().post(DownloadPageSwapEvent(position))
                }
            })
        }
    }

    private fun onFragmentSelect(position: Int) {
        binding.classifyTab.onPageSelected(position)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().post(InDownloadFragmentEvent(true))
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().post(InDownloadFragmentEvent(false))
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.classifyLayout, Animations.BounceInLeft))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.classifyLayout, Animations.FadeOutRight))
    }

    private class ViewPagerAdapter(private val fragment: Fragment): FragmentStateAdapter(fragment.requireActivity()) {
        override fun getItemCount(): Int = 5
        override fun createFragment(position: Int): Fragment {
            return when(position) {
                1 -> ModPackDownloadFragment(fragment)
                2 -> ResourcePackDownloadFragment(fragment)
                3 -> WorldDownloadFragment(fragment)
                4 -> ShaderPackDownloadFragment(fragment)
                else -> ModDownloadFragment(fragment)
            }
        }
    }
}
