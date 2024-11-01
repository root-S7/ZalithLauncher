package com.movtery.zalithlauncher.ui.fragment

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.movtery.anim.AnimPlayer
import com.movtery.anim.animations.Animations
import com.movtery.zalithlauncher.event.value.InstallLocalModpackEvent
import com.movtery.zalithlauncher.feature.download.enums.Classify
import com.movtery.zalithlauncher.feature.mod.modpack.install.InstallExtra
import com.movtery.zalithlauncher.task.Task
import com.movtery.zalithlauncher.utils.PathAndUrlManager
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.anim.ViewAnimUtils.Companion.setViewAnim
import com.movtery.zalithlauncher.utils.file.FileTools.Companion.copyFileInBackground
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import net.kdt.pojavlaunch.databinding.FragmentSelectModpackBinding
import org.greenrobot.eventbus.EventBus
import java.io.File

class SelectModPackFragment : FragmentWithAnim(R.layout.fragment_select_modpack) {
    companion object {
        const val TAG: String = "SelectModPackFragment"
    }

    private lateinit var binding: FragmentSelectModpackBinding
    private var openDocumentLauncher: ActivityResultLauncher<Any?>? = null
    private var modPackFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(OpenDocumentWithExtension(null)) { result: Uri? ->
            result?.let{
                if (!isTaskRunning()) {
                    val dialog = AlertDialog.Builder(requireContext())
                        .setView(R.layout.view_task_running)
                        .setCancelable(false)
                        .show()
                    Task.runTask {
                        modPackFile = copyFileInBackground(requireContext(), result, PathAndUrlManager.DIR_CACHE.absolutePath)
                        EventBus.getDefault().post(InstallLocalModpackEvent(InstallExtra(true, modPackFile!!.absolutePath, dialog)))
                    }.execute()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectModpackBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.returnButton.setOnClickListener { ZHTools.onBackPressed(requireActivity()) }
        binding.searchButton.setOnClickListener {
            if (!isTaskRunning()) {
                val bundle = Bundle()
                bundle.putInt(DownloadFragment.BUNDLE_CLASSIFY_TYPE, Classify.MODPACK.type)
                ZHTools.swapFragmentWithAnim(this, DownloadFragment::class.java, DownloadFragment.TAG, bundle)
            } else {
                setViewAnim(binding.searchButton, Animations.Shake)
                Toast.makeText(requireActivity(), getString(R.string.tasks_ongoing), Toast.LENGTH_SHORT).show()
            }
        }
        binding.localButton.setOnClickListener {
            if (!isTaskRunning()) {
                Toast.makeText(requireActivity(), getString(R.string.select_modpack_local_tip), Toast.LENGTH_SHORT).show()
                openDocumentLauncher?.launch(null)
            } else {
                setViewAnim(binding.localButton, Animations.Shake)
                Toast.makeText(requireActivity(), getString(R.string.tasks_ongoing), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.root, Animations.BounceInDown))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.root, Animations.FadeOutUp))
    }
}
