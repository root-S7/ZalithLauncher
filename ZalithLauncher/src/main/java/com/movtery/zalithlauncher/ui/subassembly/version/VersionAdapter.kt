package com.movtery.zalithlauncher.ui.subassembly.version

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.ItemVersionBinding
import com.movtery.zalithlauncher.feature.version.Version
import com.movtery.zalithlauncher.feature.version.VersionIconUtils
import com.movtery.zalithlauncher.feature.version.VersionsManager
import com.movtery.zalithlauncher.task.Task
import com.movtery.zalithlauncher.ui.dialog.TipDialog
import com.movtery.zalithlauncher.ui.fragment.FilesFragment
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.file.FileDeletionHandler
import net.kdt.pojavlaunch.Tools

class VersionAdapter(
    private val parentFragment: Fragment,
    private val listener: OnVersionItemClickListener
) : RecyclerView.Adapter<VersionAdapter.ViewHolder>() {
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
        private val mContext = binding.root.context

        private fun String.addInfoIfNotBlank(setRed: Boolean = false) {
            takeIf { it.isNotBlank() }?.let { string ->
                binding.versionInfo.addView(getInfoTextView(string, setRed))
            }
        }

        fun bind(version: Version?) {
            binding.versionInfo.removeAllViews()

            version?.let {
                binding.version.text = it.getVersionName()

                if (!it.isValid()) {
                    mContext.getString(R.string.version_manager_invalid).addInfoIfNotBlank(true)
                }

                it.getVersionInfo()?.let { versionInfo ->
                    binding.versionInfo.addView(getInfoTextView(versionInfo.minecraftVersion))
                    versionInfo.loaderInfo?.forEach { loaderInfo ->
                        loaderInfo.name.addInfoIfNotBlank()
                        loaderInfo.version.addInfoIfNotBlank()
                    }
                }

                binding.settings.visibility = View.VISIBLE
                binding.settings.setOnClickListener { _ ->
                    showPopupWindow(binding.root, it)
                }

                VersionIconUtils(it).start(binding.versionIcon)

                binding.root.setOnClickListener { _ ->
                    if (it.isValid()) {
                        listener.onVersionClick(it)
                    } else {
                        //版本无效时，不能设置版本，默认点击就会提示用户删除
                        deleteVersion(it, R.string.version_manager_delete_tip_invalid)
                    }
                }
                return
            }
            binding.versionIcon.setImageDrawable(ContextCompat.getDrawable(binding.root.context, R.drawable.ic_add))
            binding.version.setText(R.string.version_install_new)
            binding.settings.visibility = View.GONE
            binding.root.setOnClickListener { listener.onCreateVersion() }
        }

        private fun getInfoTextView(string: String, setRed: Boolean = false): TextView {
            val textView = TextView(mContext)
            textView.text = string
            val layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 0, Tools.dpToPx(8f).toInt(), 0)
            textView.layoutParams = layoutParams
            if (setRed) textView.setTextColor(Color.RED)
            return textView
        }

        private fun showPopupWindow(
            anchorView: View,
            version: Version
        ) {
            val context = parentFragment.requireActivity()

            val popupWindow = ListPopupWindow(context).apply {
                this.anchorView = anchorView
                this.isModal = true
                this.promptPosition = androidx.appcompat.widget.ListPopupWindow.POSITION_PROMPT_ABOVE
                this.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.background_card))
            }

            val settings: MutableList<String> = ArrayList()
            settings.add(context.getString(R.string.profiles_path_settings_goto))
            settings.add(context.getString(R.string.version_manager_rename))
            settings.add(context.getString(R.string.version_manager_delete))
            val adapter = ArrayAdapter(
                context,
                android.R.layout.simple_list_item_1,
                settings.toTypedArray()
            )

            popupWindow.setAdapter(adapter)
            popupWindow.setOnItemClickListener { _, _, position: Int, _ ->
                when (position) {
                    1 -> VersionsManager.openRenameDialog(context, version)

                    2 -> deleteVersion(version)

                    else -> {
                        val bundle = Bundle()
                        bundle.putString(
                            FilesFragment.BUNDLE_LOCK_PATH,
                            Environment.getExternalStorageDirectory().absolutePath
                        )
                        bundle.putString(FilesFragment.BUNDLE_LIST_PATH, version.getVersionPath().absolutePath)
                        ZHTools.swapFragmentWithAnim(
                            parentFragment,
                            FilesFragment::class.java, FilesFragment.TAG, bundle
                        )
                    }
                }
                popupWindow.dismiss()
            }

            popupWindow.show()
        }

        //删除版本前提示用户，如果版本无效，那么默认点击事件就是删除版本
        private fun deleteVersion(version: Version, deleteMessage: Int = R.string.version_manager_delete_tip) {
            val context = parentFragment.requireActivity()

            TipDialog.Builder(context)
                .setTitle(context.getString(R.string.version_manager_delete))
                .setMessage(deleteMessage)
                .setCancelable(false)
                .setConfirmClickListener {
                    FileDeletionHandler(
                        context,
                        listOf(version.getVersionPath()),
                        Task.runTask {
                            VersionsManager.refresh()
                        }
                    ).start()
                }.buildDialog()
        }
    }

    interface OnVersionItemClickListener {
        fun onVersionClick(version: Version)
        fun onCreateVersion()
    }
}