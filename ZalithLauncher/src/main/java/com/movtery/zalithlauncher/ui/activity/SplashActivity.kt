package com.movtery.zalithlauncher.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.ContextExecutor
import com.movtery.zalithlauncher.databinding.ActivitySplashBinding
import com.movtery.zalithlauncher.feature.background.BackgroundManager.setBackgroundImage
import com.movtery.zalithlauncher.feature.background.BackgroundType
import com.movtery.zalithlauncher.feature.unpack.Components
import com.movtery.zalithlauncher.feature.unpack.Jre
import com.movtery.zalithlauncher.feature.unpack.UnpackComponentsTask
import com.movtery.zalithlauncher.feature.unpack.UnpackJreTask
import com.movtery.zalithlauncher.feature.unpack.UnpackSingleFilesTask
import com.movtery.zalithlauncher.task.Task
import net.kdt.pojavlaunch.LauncherActivity
import net.kdt.pojavlaunch.MissingStorageActivity
import net.kdt.pojavlaunch.Tools

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    private var isStarted: Boolean = false
    private lateinit var binding: ActivitySplashBinding
    private lateinit var installableAdapter: InstallableAdapter
    private val items: MutableList<InstallableItem> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initItems()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val splashText = findViewById<TextView>(R.id.splash_text)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SplashActivity)
            adapter = installableAdapter
        }

        binding.startButton.apply {
            setOnClickListener {
                if (isStarted) return@setOnClickListener
                isStarted = true
                splashText.setText(R.string.splash_screen_installing)
                installableAdapter.startAllTasks()
            }
            isClickable = false
        }

        if (!Tools.checkStorageRoot()) {
            startActivity(Intent(this, MissingStorageActivity::class.java))
            finish()
        } else {
            setBackgroundImage(this, BackgroundType.MAIN_MENU, findViewById(R.id.background_view))
            checkEnd()
        }
    }

    override fun onResume() {
        super.onResume()
        ContextExecutor.setActivity(this)
    }

    private fun initItems() {
        Components.entries.forEach {
            val unpackComponentsTask = UnpackComponentsTask(this, it)
            if (!unpackComponentsTask.isCheckFailed()) {
                items.add(
                    InstallableItem(
                        it.displayName,
                        it.summary?.let { it1 -> getString(it1) },
                        unpackComponentsTask
                    )
                )
            }
        }
        Jre.entries.forEach {
            val unpackJreTask = UnpackJreTask(this, it)
            if (!unpackJreTask.isCheckFailed()) {
                items.add(
                    InstallableItem(
                        it.jreName,
                        getString(it.summary),
                        unpackJreTask
                    )
                )
            }
        }
        items.sort()
        installableAdapter = InstallableAdapter(items) {
            toMain()
        }
    }
    
    private fun checkEnd() {
        installableAdapter.checkAllTask()
        Task.runTask {
            UnpackSingleFilesTask(this).run()
        }.execute()

        binding.startButton.isClickable = true
    }

    private fun toMain() {
        startActivity(Intent(this, LauncherActivity::class.java))
        finish()
    }
}