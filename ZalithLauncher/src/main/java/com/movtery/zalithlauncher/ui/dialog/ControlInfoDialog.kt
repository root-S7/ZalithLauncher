package com.movtery.zalithlauncher.ui.dialog;

import android.content.Context;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.databinding.DialogControlInfoBinding;
import com.movtery.zalithlauncher.task.Task;
import com.movtery.zalithlauncher.ui.subassembly.customcontrols.ControlInfoData;
import com.movtery.zalithlauncher.ui.subassembly.customcontrols.EditControlData;
import com.movtery.zalithlauncher.utils.path.PathManager;

import net.kdt.pojavlaunch.customcontrols.CustomControls;

import java.io.File;

public class ControlInfoDialog extends FullScreenDialog implements DraggableDialog.DialogInitializationListener {
    private final DialogControlInfoBinding binding = DialogControlInfoBinding.inflate(getLayoutInflater());
    private final ControlInfoData controlInfoData;

    public ControlInfoDialog(@NonNull Context context, ControlInfoData controlInfoData, Task<?> task) {
        super(context);
        this.controlInfoData = controlInfoData;

        setCancelable(false);
        setContentView(binding.getRoot());

        init(context, task);
        DraggableDialog.initDialog(this);
    }

    private void init(Context context, Task<?> task) {
        setTextOrDefault(binding.nameText, R.string.controls_info_name, controlInfoData.name);
        setTextOrDefault(binding.fileNameText, R.string.controls_info_file_name, controlInfoData.fileName);
        setTextOrDefault(binding.authorText, R.string.controls_info_author, controlInfoData.author);
        setTextOrDefault(binding.versionText, R.string.controls_info_version, controlInfoData.version);
        setTextOrDefault(binding.descText, R.string.controls_info_desc, controlInfoData.desc);

        binding.closeButton.setOnClickListener(v -> this.dismiss());
        binding.editButton.setOnClickListener(v -> {
            EditControlInfoDialog editControlInfoDialog = new EditControlInfoDialog(context, false, controlInfoData.fileName, controlInfoData);
            editControlInfoDialog.setTitle(context.getString(R.string.generic_edit));
            editControlInfoDialog.setOnConfirmClickListener((fileName, controlInfoData) -> {
                File controlFile = new File(PathManager.DIR_CTRLMAP_PATH, fileName);

                CustomControls customControls = EditControlData.loadCustomControlsFromFile(context, controlFile);

                if (customControls != null) {
                    customControls.mControlInfoDataList.name = controlInfoData.name;
                    customControls.mControlInfoDataList.author = controlInfoData.author;
                    customControls.mControlInfoDataList.version = controlInfoData.version;
                    customControls.mControlInfoDataList.desc = controlInfoData.desc;

                    EditControlData.saveToFile(context, customControls, controlFile);
                }

                task.execute();
                editControlInfoDialog.dismiss();
            });
            editControlInfoDialog.show();
            this.dismiss();
        });
    }

    private void setTextOrDefault(TextView textView, int stringId, String value) {
        String text = getContext().getString(stringId);
        text += " ";
        if (value != null && !value.isEmpty() && !value.equals("null")) {
            text += value;
        } else {
            text += getContext().getString(R.string.generic_unknown);
        }
        textView.setText(text);
    }

    @Override
    public Window onInit() {
        return getWindow();
    }
}