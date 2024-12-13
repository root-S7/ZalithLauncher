package com.movtery.zalithlauncher.ui.dialog;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.task.Task;
import com.movtery.zalithlauncher.utils.file.FileDeletionHandler;

import java.io.File;
import java.util.List;

public class DeleteDialog extends TipDialog.Builder {
    private final Context context;

    public DeleteDialog(@NonNull Context context, Task<?> endTask, List<File> files) {
        super(context);
        this.context = context;
        init(files, endTask);
    }

    @SuppressLint("CheckResult")
    private void init(List<File> files, Task<?> endTask) {
        this.setCancelable(false);

        boolean singleFile = files.size() == 1;
        File file = files.get(0);
        boolean isFolder = file.isDirectory();
        setTitle(singleFile ? (isFolder ?
                R.string.file_delete_dir :
                R.string.file_tips) : R.string.file_delete_multiple_items_title);
        setMessage(singleFile ? (isFolder ?
                R.string.file_delete_dir_message :
                R.string.file_delete) : R.string.file_delete_multiple_items_message);
        setConfirm(R.string.generic_delete);
        setWarning();

        setConfirmClickListener(checked -> new FileDeletionHandler(context, files, endTask).start());
    }

    public void show() {
        buildDialog();
    }
}
