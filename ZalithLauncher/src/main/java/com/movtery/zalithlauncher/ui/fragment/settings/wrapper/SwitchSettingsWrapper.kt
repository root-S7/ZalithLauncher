package com.movtery.zalithlauncher.ui.fragment.settings.wrapper

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.CompoundButton
import android.widget.Switch
import com.movtery.zalithlauncher.setting.SettingUnit
import com.movtery.zalithlauncher.setting.Settings

@SuppressLint("UseSwitchCompatOrMaterialCode")
class SwitchSettingsWrapper(
    private val context: Context,
    private val unit: SettingUnit<Boolean>,
    val mainView: View,
    val switchView: Switch
) : AbstractSettingsWrapper(mainView) {
    private var listener: OnCheckedChangeListener? = null

    init {
        switchView.isChecked = unit.getValue()

        switchView.setOnCheckedChangeListener { buttonView, isChecked ->
            val switchChangeListener = OnSwitchSaveListener {
                Settings.Manager.put(unit, isChecked).save()
                checkShowRebootDialog(context)
            }
            listener?.onChange(buttonView, isChecked, switchChangeListener) ?: switchChangeListener.onSave()
        }
        mainView.setOnClickListener {
            switchView.isChecked = !switchView.isChecked
        }
    }

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener) {
        this.listener = listener
    }

    fun interface OnSwitchSaveListener {
        fun onSave()
    }

    fun interface OnCheckedChangeListener {
        fun onChange(buttonView: CompoundButton, isChecked: Boolean, listener: OnSwitchSaveListener)
    }
}