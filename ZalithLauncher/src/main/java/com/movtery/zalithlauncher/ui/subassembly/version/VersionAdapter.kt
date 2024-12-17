package com.movtery.zalithlauncher.ui.subassembly.version

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.ItemVersionBinding
import com.movtery.zalithlauncher.databinding.ViewVersionManagerBinding
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathManager
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
    private val popupWindowMap: MutableMap<Version, PopupWindow> = HashMap()

    @SuppressLint("NotifyDataSetChanged")
    fun refreshVersions(versions: List<Version?>) {
        this.versions.clear()
        this.versions.addAll(versions)

        this.popupWindowMap.clear()

        notifyDataSetChanged()
    }

    fun closeAllPopupWindow() {
        popupWindowMap.forEach { (_, popupWindow) -> popupWindow.dismiss() }
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

                if (it.getVersionConfig().isIsolation()) {
                    mContext.getString(R.string.pedit_isolation_enabled).addInfoIfNotBlank()
                }

                it.getVersionInfo()?.let { versionInfo ->
                    binding.versionInfo.addView(getInfoTextView(versionInfo.minecraftVersion))
                    versionInfo.loaderInfo?.forEach { loaderInfo ->
                        loaderInfo.name.addInfoIfNotBlank()
                        loaderInfo.version.addInfoIfNotBlank()
                    }
                }

                binding.operate.visibility = View.VISIBLE
                binding.operate.setOnClickListener { _ ->
                    showPopupWindow(binding.operate, it)
                }

                VersionIconUtils(it).start(binding.versionIcon)

                binding.root.setOnClickListener { _ ->
                    if (it.isValid()) {
                        listener.onVersionClick(it)
                    } else {
                        //版本无效时，不能设置版本，默认点击就会提示用户删除
                        deleteVersion(it, mContext.getString(R.string.version_manager_delete_tip_invalid))
                    }
                }
                return
            }
            binding.versionIcon.setImageDrawable(ContextCompat.getDrawable(binding.root.context, R.drawable.ic_add))
            binding.version.setText(R.string.version_install_new)
            binding.operate.visibility = View.GONE
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
            popupWindowMap[version]?.let {
                it.showAsDropDown(anchorView, anchorView.measuredWidth, 0)
                return
            }
            val context = parentFragment.requireActivity()

            val popupWindow = PopupWindow().apply {
                isFocusable = true
                isOutsideTouchable = true
            }

            val viewBinding = ViewVersionManagerBinding.inflate(LayoutInflater.from(context)).apply {
                val onClickListener = View.OnClickListener { v ->
                    when (v) {
                        gotoView -> swapPath(version.getVersionPath().absolutePath)
                        gamePath -> swapPath(version.getGameDir().absolutePath)
                        rename -> VersionsManager.openRenameDialog(context, version)
                        copy -> VersionsManager.openCopyDialog(context, version)
                        delete -> deleteVersion(version, context.getString(R.string.version_manager_delete_tip, version.getVersionName()))
                        else -> {}
                    }
                    popupWindow.dismiss()
                }
                gotoView.setOnClickListener(onClickListener)
                gamePath.setOnClickListener(onClickListener)
                rename.setOnClickListener(onClickListener)
                copy.setOnClickListener(onClickListener)
                delete.setOnClickListener(onClickListener)
            }
            popupWindow.apply {
                viewBinding.root.measure(0, 0)
                this.contentView = viewBinding.root
                this.width = viewBinding.root.measuredWidth
                this.height = viewBinding.root.measuredHeight
            }
            popupWindowMap[version] = popupWindow
            popupWindow.showAsDropDown(anchorView, anchorView.measuredWidth, 0)
        }

        //删除版本前提示用户，如果版本无效，那么默认点击事件就是删除版本
        private fun deleteVersion(version: Version, deleteMessage: String) {
            val context = parentFragment.requireActivity()

            TipDialog.Builder(context)
                .setTitle(context.getString(R.string.version_manager_delete))
                .setMessage(deleteMessage)
                .setWarning()
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

        private fun swapPath(path: String) {
            val bundle = Bundle()
            bundle.putString(
                FilesFragment.BUNDLE_LOCK_PATH,
                ProfilePathManager.currentPath
            )
            bundle.putString(FilesFragment.BUNDLE_LIST_PATH, path)
            ZHTools.swapFragmentWithAnim(
                parentFragment,
                FilesFragment::class.java, FilesFragment.TAG, bundle
            )
        }
    }

    interface OnVersionItemClickListener {
        fun onVersionClick(version: Version)
        fun onCreateVersion()
    }
}