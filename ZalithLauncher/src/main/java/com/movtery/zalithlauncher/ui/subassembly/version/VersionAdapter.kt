package com.movtery.zalithlauncher.ui.subassembly.version

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioButton
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
import com.movtery.zalithlauncher.feature.version.utils.VersionIconUtils
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
    //记录版本路径与之对应的RadioButton的Map
    private val radioButtonMap: MutableMap<String, RadioButton> = HashMap()
    private var currentVersion: String? = null
    private var managerPopupWindow: PopupWindow = PopupWindow().apply {
        isFocusable = true
        isOutsideTouchable = true
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshVersions(versions: List<Version?>) {
        this.versions.clear()
        this.versions.addAll(versions)
        this.radioButtonMap.apply {
            forEach { (_, radioButton) -> radioButton.isChecked = false }
            clear()
        }
        refreshCurrentVersion()
        notifyDataSetChanged()
    }

    fun closePopupWindow() {
        managerPopupWindow.dismiss()
    }

    private fun refreshCurrentVersion() {
        currentVersion = VersionsManager.getCurrentVersion()?.getVersionPath()?.absolutePath
    }

    private fun setCurrentVersion(context: Context, version: Version) {
        if (version.isValid()) {
            listener.onVersionClick(version)
            refreshCurrentVersion()
        } else {
            //版本无效时，不能设置版本，默认点击就会提示用户删除
            deleteVersion(version, context.getString(R.string.version_manager_delete_tip_invalid))
        }
        updateRadioButtonState()
    }

    private fun updateRadioButtonState() {
        for ((key, value) in radioButtonMap) {
            value.isChecked = currentVersion == key
        }
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
            }.showDialog()
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
                binding.versionInfoLayout.addView(getInfoTextView(string, setRed))
            }
        }

        fun bind(version: Version?) {
            binding.apply {
                versionInfoLayout.removeAllViews()
                versionName.isSelected = true

                version?.let {
                    radioButtonMap[it.getVersionPath().absolutePath] = radioButton
                    versionName.text = it.getVersionName()

                    if (!it.isValid()) {
                        mContext.getString(R.string.version_manager_invalid).addInfoIfNotBlank(true)
                    }

                    if (it.getVersionConfig().isIsolation()) {
                        mContext.getString(R.string.pedit_isolation_enabled).addInfoIfNotBlank()
                    }

                    it.getVersionInfo()?.let { versionInfo ->
                        versionInfoLayout.addView(getInfoTextView(versionInfo.minecraftVersion))
                        versionInfo.loaderInfo?.forEach { loaderInfo ->
                            loaderInfo.name.addInfoIfNotBlank()
                            loaderInfo.version.addInfoIfNotBlank()
                        }
                    }

                    operate.setOnClickListener { _ ->
                        showPopupWindow(operate, it)
                    }

                    VersionIconUtils(it).start(versionIcon)

                    val onClickListener = View.OnClickListener { _ ->
                        setCurrentVersion(mContext, it)
                    }
                    radioButton.setOnClickListener(onClickListener)
                    root.setOnClickListener(onClickListener)

                    setViewsVisibility(true)
                    refreshRadioButtons(it)
                    return
                }
                versionIcon.setImageDrawable(ContextCompat.getDrawable(root.context, R.drawable.ic_add))
                versionName.setText(R.string.version_install_new)
                root.setOnClickListener { listener.onCreateVersion() }

                setViewsVisibility(false)
                refreshRadioButtons(null)
            }
        }

        private fun setViewsVisibility(show: Boolean) {
            val visible = if (show) View.VISIBLE else View.GONE
            binding.apply {
                operate.visibility = visible
                radioButton.visibility = visible
                radioButtonSpace.visibility = visible
            }
        }

        private fun refreshRadioButtons(version: Version?) {
            version?.getVersionPath()?.absolutePath.let { versionPath ->
                if (currentVersion == versionPath) {
                    updateRadioButtonState()
                }
            }
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
                    managerPopupWindow.dismiss()
                }
                gotoView.setOnClickListener(onClickListener)
                gamePath.setOnClickListener(onClickListener)
                rename.setOnClickListener(onClickListener)
                copy.setOnClickListener(onClickListener)
                delete.setOnClickListener(onClickListener)
            }
            managerPopupWindow.apply {
                viewBinding.root.measure(0, 0)
                this.contentView = viewBinding.root
                this.width = viewBinding.root.measuredWidth
                this.height = viewBinding.root.measuredHeight
                showAsDropDown(anchorView, anchorView.measuredWidth, 0)
            }
        }

        private fun swapPath(path: String) {
            val bundle = Bundle()
            bundle.putString(FilesFragment.BUNDLE_LOCK_PATH, ProfilePathManager.currentPath)
            bundle.putString(FilesFragment.BUNDLE_LIST_PATH, path)
            bundle.putBoolean(FilesFragment.BUNDLE_QUICK_ACCESS_PATHS, false)
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