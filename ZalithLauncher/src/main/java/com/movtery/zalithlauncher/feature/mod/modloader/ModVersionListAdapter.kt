package com.movtery.zalithlauncher.feature.mod.modloader

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.movtery.anim.animations.Animations
import com.movtery.zalithlauncher.utils.anim.ViewAnimUtils
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.databinding.ItemFileListViewBinding
import net.kdt.pojavlaunch.modloaders.FabricVersion
import net.kdt.pojavlaunch.modloaders.ModloaderDownloadListener
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy
import net.kdt.pojavlaunch.modloaders.OptiFineUtils.OptiFineVersion
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener

class ModVersionListAdapter(
    private val modloaderListenerProxy: ModloaderListenerProxy,
    private val listener: ModloaderDownloadListener,
    private val iconDrawable: Int,
    private val mData: List<*>?
) :
    RecyclerView.Adapter<ModVersionListAdapter.ViewHolder>(), TaskCountListener {
    private var mTasksRunning = false
    private var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemFileListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setView(mData!![position])
    }

    override fun getItemCount(): Int {
        return mData?.size ?: 0
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        modloaderListenerProxy.attachListener(this.listener)
        this.onItemClickListener = listener
    }

    fun interface OnItemClickListener {
        fun onClick(version: Any?)
    }

    inner class ViewHolder(private val binding: ItemFileListViewBinding) : RecyclerView.ViewHolder(binding.root) {
        private val context: Context = itemView.context

        init {
            binding.image.setImageResource(iconDrawable)
            binding.check.visibility = View.GONE
        }

        fun setView(version: Any?) {
            when (version) {
                is OptiFineVersion -> binding.name.text = version.versionName
                is FabricVersion -> binding.name.text = version.version
                is String -> binding.name.text = version
            }
            itemView.setOnClickListener { _: View? ->
                if (mTasksRunning) {
                    ViewAnimUtils.setViewAnim(itemView, Animations.Shake)
                    Toast.makeText(context, context.getString(R.string.tasks_ongoing), Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }
                onItemClickListener?.onClick(version)
            }
        }
    }

    override fun onUpdateTaskCount(taskCount: Int) {
        mTasksRunning = taskCount != 0
    }
}
