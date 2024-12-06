package com.movtery.zalithlauncher.ui.subassembly.menu

import android.app.Activity
import android.content.Intent
import android.provider.DocumentsContract
import android.view.View
import android.widget.CompoundButton
import android.widget.SeekBar
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.ViewControlMenuBinding
import com.movtery.zalithlauncher.setting.AllSettings
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.EditorExitable

class ControlMenu(
    private val activity: Activity,
    private val exitListener: EditorExitable,
    private val binding: ViewControlMenuBinding,
    private val controlLayout: ControlLayout,
    private val export: Boolean
) : View.OnClickListener, SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener {
    init {
        val listener = this
        binding.apply {
            snapping.isChecked = AllSettings.buttonSnapping.getValue()

            MenuUtils.initSeekBarValue(snappingDistance, AllSettings.buttonSnappingDistance.getValue(), snappingDistanceValue, "dp")

            addButton.setOnClickListener(listener)
            addDrawer.setOnClickListener(listener)
            addJoystick.setOnClickListener(listener)

            load.setOnClickListener(listener)
            save.setOnClickListener(listener)
            saveAndExit.setOnClickListener(listener)

            snappingLayout.setOnClickListener(listener)
            snapping.setOnCheckedChangeListener(listener)
            snappingDistance.setOnSeekBarChangeListener(listener)
            snappingDistanceAdd.setOnClickListener(listener)
            snappingDistanceRemove.setOnClickListener(listener)
            selectDefault.setOnClickListener(listener)
            export.setOnClickListener(listener)
            exit.setOnClickListener(listener)
            if (this@ControlMenu.export) export.visibility = View.VISIBLE
            else export.visibility = View.GONE
        }
    }

    override fun onClick(v: View?) {
        binding.apply {
            when (v) {
                addButton -> controlLayout.addControlButton(ControlData(activity.getString(R.string.controls_add_control_button)))
                addDrawer -> controlLayout.addDrawer(ControlDrawerData())
                addJoystick -> controlLayout.addJoystickButton(ControlJoystickData())

                load -> controlLayout.openLoadDialog()
                save -> controlLayout.openSaveDialog()
                saveAndExit -> controlLayout.openSaveAndExitDialog(exitListener)

                snappingLayout -> MenuUtils.toggleSwitchState(snapping)
                snappingDistanceAdd -> MenuUtils.adjustSeekbar(snappingDistance, 1)
                snappingDistanceRemove -> MenuUtils.adjustSeekbar(snappingDistance, -1)
                selectDefault -> controlLayout.openSetDefaultDialog()
                export -> {
                    try { // Saving the currently shown control
                        val contentUri = DocumentsContract.buildDocumentUri(
                            activity.getString(R.string.storageProviderAuthorities),
                            controlLayout.saveToDirectory(controlLayout.mLayoutFileName)
                        )

                        val shareIntent = Intent()
                        shareIntent.setAction(Intent.ACTION_SEND)
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        shareIntent.setType("application/json")
                        activity.startActivity(shareIntent)

                        val sendIntent = Intent.createChooser(shareIntent, controlLayout.mLayoutFileName)
                        activity.startActivity(sendIntent)
                    } catch (e: Exception) {
                        Tools.showError(activity, e)
                    }
                }
                exit -> controlLayout.openExitDialog(exitListener)
                else -> {}
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        binding.apply {
            when (seekBar) {
                snappingDistance -> {
                    AllSettings.buttonSnappingDistance.put(progress).save()
                    MenuUtils.updateSeekbarValue(progress, snappingDistanceValue, "dp")
                }
                else -> {}
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        binding.apply {
            when (buttonView) {
                snapping -> AllSettings.buttonSnapping.put(isChecked).save()
                else -> {}
            }
        }
    }
}