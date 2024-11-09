package com.movtery.zalithlauncher.feature.accounts

import android.content.Context
import com.movtery.zalithlauncher.event.value.OtherLoginEvent
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.login.AuthResult
import com.movtery.zalithlauncher.feature.login.OtherLoginApi
import com.movtery.zalithlauncher.task.Task
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.authenticator.microsoft.MicrosoftBackgroundLogin
import net.kdt.pojavlaunch.value.MinecraftAccount
import org.greenrobot.eventbus.EventBus
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.Objects

class AccountUtils {
    companion object {
        @JvmStatic
        fun microsoftLogin(account: MinecraftAccount) {
            val accountsManager = AccountsManager.getInstance()

            // Perform login only if needed
            MicrosoftBackgroundLogin(true, account.msaRefreshToken)
                .performLogin(
                    account,
                    accountsManager.progressListener,
                    accountsManager.doneListener,
                    accountsManager.errorListener
                )
        }

        @JvmStatic
        fun otherLogin(context: Context, account: MinecraftAccount) {
            val errorListener = AccountsManager.getInstance().errorListener

            OtherLoginApi.setBaseUrl(account.otherBaseUrl)
            Task.runTask {
                OtherLoginApi.refresh(context, account, false, object : OtherLoginApi.Listener {
                    override fun onSuccess(authResult: AuthResult) {
                        account.accessToken = authResult.accessToken
                        EventBus.getDefault().post(OtherLoginEvent(account))
                    }

                    override fun onFailed(error: String) {
                        errorListener.onLoginError(Throwable(error))
                    }
                })
            }.onThrowable { t -> errorListener.onLoginError(t) }.execute()
        }

        @JvmStatic
        fun isOtherLoginAccount(account: MinecraftAccount): Boolean {
            return !Objects.isNull(account.otherBaseUrl) && account.otherBaseUrl != "0"
        }

        @JvmStatic
        fun isMicrosoftAccount(account: MinecraftAccount): Boolean {
            return account.accountType == "Microsoft"
        }

        @JvmStatic
        fun isNoLoginRequired(account: MinecraftAccount?): Boolean {
            return account == null || account.accountType == "Local"
        }

        //修改自源代码：https://github.com/HMCL-dev/HMCL/blob/main/HMCLCore/src/main/java/org/jackhuang/hmcl/auth/authlibinjector/AuthlibInjectorServer.java#L60-#L76
        //原项目版权归原作者所有，遵循GPL v3协议
        fun tryGetFullServerUrl(baseUrl: String): String {
            fun String.addSlashIfMissing(): String {
                if (!endsWith("/")) return "$this/"
                return this
            }

            var url = addHttpsIfMissing(baseUrl)
            runCatching {
                var conn = URL(url).openConnection() as HttpURLConnection
                conn.getHeaderField("x-authlib-injector-api-location")?.let { ali ->
                    val absoluteAli = URL(conn.url, ali)
                    url = url.addSlashIfMissing()
                    val absoluteUrl = absoluteAli.toString().addSlashIfMissing()
                    if (url != absoluteUrl) {
                        conn.disconnect()
                        url = absoluteUrl
                        conn = absoluteAli.openConnection() as HttpURLConnection
                    }
                }

                return url.addSlashIfMissing()
            }.getOrElse { e ->
                Logging.e("getFullServerUrl", Tools.printToString(e))
            }
            return baseUrl
        }

        //修改自源代码：https://github.com/HMCL-dev/HMCL/blob/main/HMCLCore/src/main/java/org/jackhuang/hmcl/auth/authlibinjector/AuthlibInjectorServer.java#L90-#L96
        //原项目版权归原作者所有，遵循GPL v3协议
        private fun addHttpsIfMissing(baseUrl: String): String {
            return if (!baseUrl.startsWith("http://", true) && !baseUrl.startsWith("https://")) {
                "https://$baseUrl".lowercase(Locale.ROOT)
            } else baseUrl.lowercase(Locale.ROOT)
        }
    }
}
