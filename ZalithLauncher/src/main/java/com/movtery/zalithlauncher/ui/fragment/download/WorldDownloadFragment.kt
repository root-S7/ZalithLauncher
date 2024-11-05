package com.movtery.zalithlauncher.ui.fragment.download

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.ContextExecutor
import com.movtery.zalithlauncher.feature.download.InfoAdapter
import com.movtery.zalithlauncher.feature.download.enums.Classify
import com.movtery.zalithlauncher.feature.download.install.UnpackWorldZipHelper
import com.movtery.zalithlauncher.feature.download.utils.CategoryUtils
import com.movtery.zalithlauncher.task.Task
import com.movtery.zalithlauncher.task.TaskExecutors
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.file.FileTools.Companion.copyFileInBackground
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import java.io.File

class WorldDownloadFragment() : AbstractResourceDownloadFragment(
    Classify.WORLD,
    CategoryUtils.getWorldCategory(),
    false
) {
    private var openDocumentLauncher: ActivityResultLauncher<Any>? = null
    private val mWorldPath = File(sGameDir, "/saves")

    constructor(parentFragment: Fragment): this() {
        this.mInfoAdapter = InfoAdapter(parentFragment, this, mWorldPath)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(OpenDocumentWithExtension("zip")) { uris: List<Uri>? ->
            uris?.let { uriList ->
                uriList[0].let { result ->
                    val dialog = ZHTools.showTaskRunningDialog(requireContext())
                    Task.runTask {
                        val worldFile = copyFileInBackground(requireContext(), result, mWorldPath.absolutePath)
                        runCatching {
                            UnpackWorldZipHelper.unpackFile(worldFile, mWorldPath)
                        }.getOrElse {
                            ContextExecutor.showToast(R.string.download_install_unpack_world_error, Toast.LENGTH_SHORT)
                        }
                    }.finallyTask(TaskExecutors.getAndroidUI()) {
                        dialog.dismiss()
                    }.execute()
                }
            }
        }
    }

    override fun initInstallButton(installButton: Button) {
        installButton.setOnClickListener {
            val suffix = ".zip"
            Toast.makeText(
                requireActivity(),
                String.format(getString(R.string.file_add_file_tip), suffix),
                Toast.LENGTH_SHORT
            ).show()
            openDocumentLauncher?.launch(suffix)
        }
    }
}