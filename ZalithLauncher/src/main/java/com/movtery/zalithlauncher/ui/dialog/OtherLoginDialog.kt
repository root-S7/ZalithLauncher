package com.movtery.zalithlauncher.ui.dialog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.Window
import android.widget.Toast
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.login.AuthResult
import com.movtery.zalithlauncher.feature.login.OtherLoginApi
import com.movtery.zalithlauncher.feature.login.Servers.Server
import com.movtery.zalithlauncher.task.Task
import com.movtery.zalithlauncher.task.TaskExecutors
import com.movtery.zalithlauncher.ui.dialog.DraggableDialog.DialogInitializationListener
import com.movtery.zalithlauncher.utils.ZHTools
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.databinding.DialogOtherLoginBinding
import net.kdt.pojavlaunch.value.MinecraftAccount
import java.util.Objects

class OtherLoginDialog(
    context: Context,
    private val server: Server,
    private val listener: OnLoginListener
) : FullScreenDialog(context), View.OnClickListener, DialogInitializationListener {
    private val binding = DialogOtherLoginBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
        binding.apply {
            serverName.text = server.serverName
            if (server.register.isEmpty()) {
                registryText.visibility = View.GONE
            } else {
                registryText.setOnClickListener(this@OtherLoginDialog)
            }

            cancelButton.setOnClickListener(this@OtherLoginDialog)
            loginButton.setOnClickListener(this@OtherLoginDialog)
        }

        DraggableDialog.initDialog(this)
    }

    private fun checkAccountInformation(email: String?, password: String?): Boolean {
        val emailEmpty = email.isNullOrEmpty()
        val passwordEmpty = password.isNullOrEmpty()

        return if (emailEmpty || passwordEmpty) {
            val errorString = context.getString(R.string.generic_error_field_empty)
            if (emailEmpty) binding.emailEdit.error = errorString
            if (passwordEmpty) binding.passwordEdit.error = errorString
            false
        } else true
    }

    private fun refresh(account: MinecraftAccount) {
        Task.runTask {
            OtherLoginApi.setBaseUrl(server.baseUrl)
            OtherLoginApi.refresh(context, account, true, object : OtherLoginApi.Listener {
                override fun onSuccess(authResult: AuthResult) {
                    account.accessToken = authResult.accessToken
                    Tools.runOnUiThread {
                        listener.unLoading()
                        listener.onSuccess(account)
                    }
                }

                override fun onFailed(error: String) {
                    Tools.runOnUiThread {
                        listener.unLoading()
                        listener.onFailed(error)
                    }
                }
            })
        }.beforeStart(TaskExecutors.getAndroidUI()) {
            listener.onLoading()
        }.onThrowable { e ->
            Logging.e("Other Login", Tools.printToString(e))
        }.execute()
    }

    override fun onInit(): Window? = window

    override fun onClick(v: View) {
        binding.apply {
            when (v) {
                cancelButton -> dismiss()
                registryText -> {
                    server.register.takeIf { it.isNotEmpty() }?.let {
                        val intent = Intent()
                        intent.setAction("android.intent.action.VIEW")
                        val uri = Uri.parse(it)
                        intent.setData(uri)
                        context.startActivity(intent)
                        dismiss()
                    }
                }
                loginButton -> {
                    val email = emailEdit.text.toString()
                    val password = passwordEdit.text.toString()
                    //登录前需检查邮箱、密码、基础链接
                    if (!checkAccountInformation(email, password)) return
                    if (server.baseUrl.isNullOrEmpty()) {
                        Toast.makeText(context, context.getString(R.string.other_login_server_not_empty), Toast.LENGTH_SHORT).show()
                        return
                    }

                    Task.runTask {
                        OtherLoginApi.setBaseUrl(server.baseUrl)
                        OtherLoginApi.login(context, email, password,
                            object : OtherLoginApi.Listener {
                                override fun onSuccess(authResult: AuthResult) {
                                    MinecraftAccount().apply {
                                        accessToken = authResult.accessToken
                                        clientToken = authResult.clientToken
                                        baseUrl = server.baseUrl
                                        account = email
                                        expiresAt = ZHTools.getCurrentTimeMillis() + 30 * 60 * 1000
                                        accountType = server.serverName
                                        if (!Objects.isNull(authResult.selectedProfile)) {
                                            username = authResult.selectedProfile.name
                                            profileId = authResult.selectedProfile.id
                                            Tools.runOnUiThread {
                                                listener.unLoading()
                                                listener.onSuccess(this)
                                            }
                                        } else {
                                            Tools.runOnUiThread {
                                                val selectRoleDialog = SelectRoleDialog(
                                                    context,
                                                    authResult.availableProfiles
                                                )
                                                selectRoleDialog.setOnSelectedListener { selectedProfile ->
                                                    username = selectedProfile.name
                                                    profileId = selectedProfile.id
                                                    refresh(this)
                                                }
                                                listener.unLoading()
                                                selectRoleDialog.show()
                                            }
                                        }
                                    }
                                }

                                override fun onFailed(error: String) {
                                    Tools.runOnUiThread {
                                        listener.unLoading()
                                        listener.onFailed(error)
                                    }
                                }
                            })
                    }.beforeStart(TaskExecutors.getAndroidUI()) {
                        listener.onLoading()
                    }.onThrowable { e ->
                        Logging.e("Other Login", Tools.printToString(e))
                    }.execute()

                    dismiss()
                }
            }
        }
    }

    interface OnLoginListener {
        fun onLoading()
        fun unLoading()
        fun onSuccess(account: MinecraftAccount)
        fun onFailed(error: String)
    }
}