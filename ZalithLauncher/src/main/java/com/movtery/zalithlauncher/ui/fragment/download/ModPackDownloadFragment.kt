package com.movtery.zalithlauncher.ui.fragment.download

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.movtery.anim.animations.Animations
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.event.value.InstallLocalModpackEvent
import com.movtery.zalithlauncher.feature.download.InfoAdapter
import com.movtery.zalithlauncher.feature.download.enums.Classify
import com.movtery.zalithlauncher.feature.download.utils.CategoryUtils
import com.movtery.zalithlauncher.feature.mod.modpack.install.InstallExtra
import com.movtery.zalithlauncher.task.Task
import com.movtery.zalithlauncher.utils.PathAndUrlManager
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.anim.ViewAnimUtils.Companion.setViewAnim
import com.movtery.zalithlauncher.utils.file.FileTools.Companion.copyFileInBackground
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import org.greenrobot.eventbus.EventBus

class ModPackDownloadFragment() : AbstractResourceDownloadFragment(
    Classify.MODPACK,
    CategoryUtils.getModPackCategory(),
    true
) {
    private var openDocumentLauncher: ActivityResultLauncher<Any>? = null

    constructor(parentFragment: Fragment): this() {
        this.mInfoAdapter = InfoAdapter(parentFragment, this, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(OpenDocumentWithExtension(null)) { uris: List<Uri>? ->
            uris?.let { uriList ->
                uriList[0].let { result ->
                    if (!isTaskRunning()) {
                        val dialog = ZHTools.showTaskRunningDialog(requireContext())
                        Task.runTask {
                            val modPackFile = copyFileInBackground(requireContext(), result, PathAndUrlManager.DIR_CACHE.absolutePath)
                            EventBus.getDefault().post(InstallLocalModpackEvent(InstallExtra(true, modPackFile.absolutePath, dialog)))
                        }.execute()
                    }
                }
            }
        }
    }

    override fun initInstallButton(installButton: Button) {
        installButton.setOnClickListener {
            if (!isTaskRunning()) {
                Toast.makeText(requireActivity(), getString(R.string.select_modpack_local_tip), Toast.LENGTH_SHORT).show()
                openDocumentLauncher?.launch(null)
            } else {
                setViewAnim(installButton, Animations.Shake)
                Toast.makeText(requireActivity(), getString(R.string.tasks_ongoing), Toast.LENGTH_SHORT).show()
            }
        }
    }
}