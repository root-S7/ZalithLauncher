package com.movtery.zalithlauncher.ui.dialog;

import android.content.Context;
import android.text.Editable;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.databinding.DialogEditTextBinding;
import com.movtery.zalithlauncher.utils.stringutils.StringUtilsKt;

public class EditTextDialog extends FullScreenDialog implements DraggableDialog.DialogInitializationListener {
    private final Context context;
    private final DialogEditTextBinding binding = DialogEditTextBinding.inflate(getLayoutInflater());
    private final String title, message, editText, hintText, checkBox, confirm;
    private final View.OnClickListener cancelListener;
    private final ConfirmListener confirmListener;
    private final boolean showCheckBox;
    private final int inputType;
    private final boolean required;

    private EditTextDialog(@NonNull Context context, String title, String message, String editText, String hintText, String checkBox, String confirm,
                           boolean showCheckBox, int inputType,
                           View.OnClickListener cancelListener, ConfirmListener confirmListener,
                           boolean required) {
        super(context);

        this.context = context;

        this.setCancelable(false);
        this.setContentView(binding.getRoot());

        this.title = title;
        this.message = message;
        this.editText = editText;
        this.hintText = hintText;
        this.checkBox = checkBox;
        this.confirm = confirm;

        this.showCheckBox = showCheckBox;
        this.inputType = inputType;

        this.cancelListener = cancelListener;
        this.confirmListener = confirmListener;

        this.required = required;

        init();
        DraggableDialog.initDialog(this);
    }

    private void init() {
        if (this.title != null) {
            binding.titleView.setText(this.title);
        }
        if (this.message != null) {
            binding.messageView.setText(this.message);
            binding.messageView.setVisibility(View.VISIBLE);
        }

        if (editText != null) binding.textEdit.setText(editText);
        if (hintText != null) binding.textEdit.setHint(hintText);
        else if (required) binding.textEdit.setHint(R.string.generic_required);

        if (confirm != null) binding.confirmButton.setText(confirm);

        if (showCheckBox) {
            binding.checkBox.setVisibility(View.VISIBLE);
            binding.checkBox.setText(this.checkBox);
        }
        if (inputType != -1) binding.textEdit.setInputType(inputType);

        if (this.confirmListener != null) {
            binding.confirmButton.setOnClickListener(v -> {
                if (required) {
                    Editable text = binding.textEdit.getText();
                    boolean empty = text == null || StringUtilsKt.isEmptyOrBlank(text.toString());
                    if (empty) {
                        binding.textEdit.setError(context.getString(R.string.generic_error_field_empty));
                        return;
                    }
                }
                boolean dismissDialog = confirmListener.onConfirm(binding.textEdit, binding.checkBox.isChecked());
                if (dismissDialog) this.dismiss();
            });
        }
        View.OnClickListener cancelListener = this.cancelListener != null ? this.cancelListener : view -> this.dismiss();
        binding.cancelButton.setOnClickListener(cancelListener);
    }

    @Override
    public Window onInit() {
        return getWindow();
    }

    public interface ConfirmListener {
        boolean onConfirm(EditText editText, boolean checked);
    }

    public static class Builder {
        private final Context context;
        private String title, message, editText, hintText, checkBox, confirm;
        private boolean showCheckBox = false;
        private int inputType = -1;
        private View.OnClickListener cancelListener;
        private ConfirmListener confirmListener;
        private boolean required = false;

        public Builder(Context context) {
            this.context = context;
        }

        @CheckResult
        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        @CheckResult
        public Builder setTitle(int title) {
            return setTitle(context.getString(title));
        }

        @CheckResult
        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        @CheckResult
        public Builder setMessage(int message) {
            return setMessage(context.getString(message));
        }

        @CheckResult
        public Builder setEditText(String editText) {
            this.editText = editText;
            return this;
        }

        @CheckResult
        public Builder setHintText(int hintText) {
            return setHintText(context.getString(hintText));
        }

        @CheckResult
        public Builder setHintText(String hintText) {
            this.hintText = hintText;
            return this;
        }

        @CheckResult
        public Builder setCancel(View.OnClickListener cancel) {
            this.cancelListener = cancel;
            return this;
        }

        @CheckResult
        public Builder setConfirmText(int text) {
            return setConfirmText(context.getString(text));
        }

        @CheckResult
        public Builder setConfirmText(String text) {
            this.confirm = text;
            return this;
        }

        @CheckResult
        public Builder setShowCheckBox(boolean show) {
            this.showCheckBox = show;
            return this;
        }

        @CheckResult
        public Builder setCheckBoxText(int text) {
            return setCheckBoxText(context.getString(text));
        }

        @CheckResult
        public Builder setCheckBoxText(String text) {
            this.checkBox = text;
            return this;
        }

        @CheckResult
        public Builder setInputType(int inputType) {
            this.inputType = inputType;
            return this;
        }

        @CheckResult
        public Builder setConfirmListener(ConfirmListener confirmListener) {
            this.confirmListener = confirmListener;
            return this;
        }

        /**
         * 设置为必填，当用户点击确认时，将检查输入框的内容是否为空（包括空格检查）
         * 如果是，那么拦截点击事件并告知用户
         */
        @CheckResult
        public Builder setAsRequired() {
            this.required = true;
            return this;
        }

        public void buildDialog() {
            new EditTextDialog(this.context, this.title, this.message, this.editText, this.hintText, this.checkBox, this.confirm,
                    showCheckBox, inputType,
                    this.cancelListener, this.confirmListener,
                    this.required).show();
        }
    }
}
