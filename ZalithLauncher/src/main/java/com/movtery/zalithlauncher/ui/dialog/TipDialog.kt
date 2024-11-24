package com.movtery.zalithlauncher.ui.dialog

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.Window
import androidx.annotation.CheckResult
import com.movtery.zalithlauncher.databinding.DialogTipBinding
import com.movtery.zalithlauncher.ui.dialog.DraggableDialog.DialogInitializationListener

class TipDialog private constructor(
    context: Context,
    title: String?,
    message: String?,
    confirm: String?,
    cancel: String?,
    checkBoxText: String?,
    showCheckBox: Boolean,
    showCancel: Boolean,
    showConfirm: Boolean,
    centerMessage: Boolean,
    private val cancelListener: OnCancelClickListener?,
    private val confirmListener: OnConfirmClickListener?,
    private val dismissListener: OnDialogDismissListener?
) : FullScreenDialog(context), DialogInitializationListener {
    private val binding = DialogTipBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
        DraggableDialog.initDialog(this)

        title?.apply { binding.titleView.text = this }
        message?.apply { binding.messageView.text = this }
        cancel?.apply { binding.cancelButton.text = this }
        confirm?.apply { binding.confirmButton.text = this }
        if (centerMessage) binding.messageView.gravity = Gravity.CENTER_HORIZONTAL
        if (showCheckBox) {
            binding.checkBox.visibility = View.VISIBLE
            checkBoxText?.let { binding.checkBox.text = it }
        }

        binding.cancelButton.setOnClickListener {
            cancelListener?.onCancelClick()
            this.dismiss()
        }
        binding.confirmButton.setOnClickListener {
            confirmListener?.onConfirmClick(binding.checkBox.isChecked)
            this.dismiss()
        }

        binding.cancelButton.visibility = if (showCancel) View.VISIBLE else View.GONE
        binding.confirmButton.visibility = if (showConfirm) View.VISIBLE else View.GONE
    }

    override fun dismiss() {
        if (dismissListener?.onDismiss() == false) return
        super.dismiss()
    }

    override fun onInit(): Window? = window

    fun interface OnCancelClickListener {
        fun onCancelClick()
    }

    fun interface OnConfirmClickListener {
        fun onConfirmClick(checked: Boolean)
    }

    fun interface OnDialogDismissListener {
        fun onDismiss(): Boolean
    }

    open class Builder(private val context: Context) {
        private var title: String? = null
        private var message: String? = null
        private var cancel: String? = null
        private var confirm: String? = null
        private var checkBox: String? = null
        private var cancelClickListener: OnCancelClickListener? = null
        private var confirmClickListener: OnConfirmClickListener? = null
        private var dialogDismissListener: OnDialogDismissListener? = null
        private var cancelable = true
        private var showCheckBox = false
        private var showCancel = true
        private var showConfirm = true
        private var centerMessage = true

        fun buildDialog(): TipDialog {
            val tipDialog = TipDialog(
                this.context,
                title, message, confirm, cancel, checkBox,
                showCheckBox,
                showCancel, showConfirm, centerMessage,
                cancelClickListener, confirmClickListener, dialogDismissListener
            )
            tipDialog.setCancelable(cancelable)
            tipDialog.show()
            return tipDialog
        }

        @CheckResult
        fun setTitle(title: String?): Builder {
            this.title = title
            return this
        }

        @CheckResult
        fun setTitle(title: Int): Builder {
            return setTitle(context.getString(title))
        }

        @CheckResult
        fun setMessage(message: String?): Builder {
            this.message = message
            return this
        }

        @CheckResult
        fun setMessage(message: Int): Builder {
            return setMessage(context.getString(message))
        }

        @CheckResult
        fun setCancel(cancel: String?): Builder {
            this.cancel = cancel
            return this
        }

        @CheckResult
        fun setCancel(cancel: Int): Builder {
            return setCancel(context.getString(cancel))
        }

        @CheckResult
        fun setCheckBox(checkBoxText: String?): Builder {
            this.checkBox = checkBoxText
            return this
        }

        @CheckResult
        fun setCheckBox(checkBoxText: Int): Builder {
            return setCheckBox(context.getString(checkBoxText))
        }

        @CheckResult
        fun setConfirm(confirm: String?): Builder {
            this.confirm = confirm
            return this
        }

        @CheckResult
        fun setConfirm(confirm: Int): Builder {
            return setConfirm(context.getString(confirm))
        }

        @CheckResult
        fun setShowCheckBox(show: Boolean): Builder {
            showCheckBox = show
            return this
        }

        @CheckResult
        fun setCancelClickListener(cancelClickListener: OnCancelClickListener?): Builder {
            this.cancelClickListener = cancelClickListener
            return this
        }

        @CheckResult
        fun setConfirmClickListener(confirmClickListener: OnConfirmClickListener?): Builder {
            this.confirmClickListener = confirmClickListener
            return this
        }

        @CheckResult
        fun setDialogDismissListener(dialogDismissListener: OnDialogDismissListener?): Builder {
            this.dialogDismissListener = dialogDismissListener
            return this
        }

        @CheckResult
        fun setCancelable(cancelable: Boolean): Builder {
            this.cancelable = cancelable
            return this
        }

        @CheckResult
        fun setShowCancel(showCancel: Boolean): Builder {
            this.showCancel = showCancel
            return this
        }

        @CheckResult
        fun setShowConfirm(showConfirm: Boolean): Builder {
            this.showConfirm = showConfirm
            return this
        }

        @CheckResult
        fun setCenterMessage(center: Boolean): Builder {
            this.centerMessage = center
            return this
        }
    }
}
