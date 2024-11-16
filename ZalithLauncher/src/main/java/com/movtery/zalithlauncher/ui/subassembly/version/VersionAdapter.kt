package com.movtery.zalithlauncher.ui.subassembly.version

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.ItemVersionBinding
import com.movtery.zalithlauncher.feature.version.Version
import com.movtery.zalithlauncher.feature.version.VersionsManager

class VersionAdapter(private val listener: OnVersionItemClickListener) : RecyclerView.Adapter<VersionAdapter.ViewHolder>() {
    private val versions: MutableList<Version?> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    fun refreshVersions(versions: List<Version?>) {
        this.versions.clear()
        this.versions.addAll(versions)

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemVersionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(versions[position])
    }

    override fun getItemCount(): Int = versions.size

    inner class ViewHolder(val binding: ItemVersionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(version: Version?) {
            version?.let {
                binding.version.text = it.getVersionName()
                VersionsManager.getVersionIconFile(it).let { icon ->
                    if (icon.exists()) {
                        Glide.with(binding.versionIcon)
                            .load(icon)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(binding.versionIcon)
                    } else {
                        binding.versionIcon.setImageDrawable(ContextCompat.getDrawable(binding.root.context, R.drawable.ic_minecraft))
                    }
                }
                binding.root.setOnClickListener { _ ->
                    listener.onVersionClick(it)
                }
                return
            }
            binding.versionIcon.setImageDrawable(ContextCompat.getDrawable(binding.root.context, R.drawable.ic_add))
            binding.version.setText(R.string.version_install_new)
            binding.root.setOnClickListener { listener.onCreateVersion() }
        }
    }

    interface OnVersionItemClickListener {
        fun onVersionClick(version: Version)
        fun onCreateVersion()
    }
}