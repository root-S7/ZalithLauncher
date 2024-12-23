package com.movtery.zalithlauncher.feature

import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.ui.subassembly.about.SponsorItemBean
import com.movtery.zalithlauncher.ui.subassembly.about.SponsorMeta
import com.movtery.zalithlauncher.utils.http.CallUtils
import com.movtery.zalithlauncher.utils.http.CallUtils.CallbackListener
import com.movtery.zalithlauncher.utils.path.UrlManager
import com.movtery.zalithlauncher.utils.stringutils.StringUtils
import net.kdt.pojavlaunch.Tools
import okhttp3.Call
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Objects

class CheckSponsor {
    companion object {
        private var sponsorData: ArrayList<SponsorItemBean>? = null
        private var isChecking = false

        @JvmStatic
        fun getSponsorData(): List<SponsorItemBean>? {
            return sponsorData
        }

        @JvmStatic
        fun check(listener: CheckListener) {
            if (isChecking) {
                listener.onFailure()
                return
            }
            isChecking = true

            sponsorData?.let {
                listener.onSuccessful(sponsorData)
                isChecking = false
                return
            }

            CallUtils(object : CallbackListener {
                override fun onFailure(call: Call?) {
                    listener.onFailure()
                    isChecking = false
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call?, response: Response?) {
                    if (!response!!.isSuccessful) {
                        Logging.e("CheckSponsor", "Unexpected code ${response.code}")
                    } else {
                        runCatching {
                            Objects.requireNonNull(response.body)
                            val responseBody = response.body!!.string()

                            val originJson = JSONObject(responseBody)
                            val rawBase64 = originJson.getString("content")
                            //base64解码，因为这里读取的是一个经过Base64加密后的文本
                            val rawJson = StringUtils.decodeBase64(rawBase64)

                            val sponsorMeta =
                                Tools.GLOBAL_GSON.fromJson(rawJson, SponsorMeta::class.java)
                            if (sponsorMeta.sponsors.isEmpty()) {
                                listener.onFailure()
                                return
                            }
                            sponsorData = ArrayList()
                            for (sponsor in sponsorMeta.sponsors) {
                                sponsorData?.add(
                                    SponsorItemBean(
                                        sponsor.name,
                                        sponsor.time,
                                        sponsor.amount
                                    )
                                )
                            }
                            listener.onSuccessful(sponsorData)
                        }.getOrElse { e ->
                            Logging.e("Load Sponsor Data", "Failed to resolve sponsor list.", e)
                            listener.onFailure()
                        }
                    }
                    isChecking = false
                }
            }, "${UrlManager.URL_GITHUB_HOME}launcher_sponsor.json", null).enqueue()
        }
    }

    interface CheckListener {
        fun onFailure()

        fun onSuccessful(data: List<SponsorItemBean>?)
    }
}
