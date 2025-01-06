package com.movtery.zalithlauncher.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.DialogModDependenciesBinding
import com.movtery.zalithlauncher.feature.download.ModDependenciesAdapter
import com.movtery.zalithlauncher.feature.download.item.DependenciesInfoItem
import com.movtery.zalithlauncher.feature.download.item.InfoItem

class ModDependenciesDialog(
    private val parentFragment: Fragment,
    private val infoItem: InfoItem,
    private val mData: List<DependenciesInfoItem>,
    private val install: () -> Unit
) :
    FullScreenDialog(parentFragment.requireContext()) {
    private val binding = DialogModDependenciesBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCancelable(false)
        setContentView(binding.root)

        init(parentFragment, infoItem, mData.toMutableList(), install)

        window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT
            )
            setGravity(Gravity.CENTER)

            //隐藏状态栏
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    private fun init(
        parentFragment: Fragment,
        infoItem: InfoItem,
        mData: MutableList<DependenciesInfoItem>,
        install: () -> Unit
    ) {
        val context = parentFragment.requireContext()

        binding.titleView.text = context.getString(R.string.download_install_dependencies, infoItem.title)
        binding.downloadButton.text = context.getString(R.string.download_install, infoItem.title)

        mData.sort()
        val adapter = ModDependenciesAdapter(parentFragment, infoItem, mData)
        adapter.setOnItemCLickListener { this.dismiss() }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutAnimation = LayoutAnimationController(AnimationUtils.loadAnimation(context, R.anim.fade_downwards))
        binding.recyclerView.adapter = adapter

        binding.closeButton.setOnClickListener { this.dismiss() }
        binding.downloadButton.setOnClickListener {
            install()
            this.dismiss()
        }
    }
}
