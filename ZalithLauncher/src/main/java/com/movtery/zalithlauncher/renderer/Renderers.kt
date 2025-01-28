package com.movtery.zalithlauncher.renderer

import android.content.Context
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.renderer.renderers.FreedrenoRenderer
import com.movtery.zalithlauncher.renderer.renderers.GL4ESRenderer
import com.movtery.zalithlauncher.renderer.renderers.PanfrostRenderer
import com.movtery.zalithlauncher.renderer.renderers.VirGLRenderer
import com.movtery.zalithlauncher.renderer.renderers.VulkanZinkRenderer
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.Tools

/**
 * 启动器所有渲染器总管理者，启动器内置的渲染器与渲染器插件加载的渲染器，都会加载到这里
 */
object Renderers {
    private val renderers: MutableList<AbstractRenderer> = mutableListOf()
    private var compatibleRenderers: Pair<RenderersList, MutableList<AbstractRenderer>>? = null
    private var currentRenderer: AbstractRenderer? = null
    private var isInitialized: Boolean = false

    fun init() {
        if (isInitialized) return
        isInitialized = true

        renderers.apply {
            add(GL4ESRenderer())
            add(VulkanZinkRenderer())
            add(VirGLRenderer())
            add(FreedrenoRenderer())
            add(PanfrostRenderer())
        }
    }

    /**
     * 获取兼容当前设备的所有渲染器
     */
    fun getCompatibleRenderers(context: Context): Pair<RenderersList, List<AbstractRenderer>> = compatibleRenderers ?: run {
        val deviceHasVulkan = Tools.checkVulkanSupport(context.packageManager)
        // Currently, only 32-bit x86 does not have the Zink binary
        val deviceHasZinkBinary = !(Architecture.is32BitsDevice() && Architecture.isx86Device())

        val compatibleRenderers1: MutableList<AbstractRenderer> = mutableListOf()
        renderers.forEach { renderer ->
            if (renderer.getRendererId().contains("vulkan") && !deviceHasVulkan) return@forEach
            if (renderer.getRendererId().contains("zink") && !deviceHasZinkBinary) return@forEach
            compatibleRenderers1.add(renderer)
        }

        val rendererIdentifiers: MutableList<String> = mutableListOf()
        val rendererNames: MutableList<String> = mutableListOf()
        compatibleRenderers1.forEach { renderer ->
            rendererIdentifiers.add(renderer.getUniqueIdentifier())
            rendererNames.add(renderer.getRendererName())
        }

        val rendererPair = Pair(RenderersList(rendererIdentifiers, rendererNames), compatibleRenderers1)
        compatibleRenderers = rendererPair
        rendererPair
    }

    fun addRenderer(renderer: AbstractRenderer) {
        this.renderers.add(renderer)
    }

    /**
     * 设置当前的渲染器
     */
    fun setCurrentRenderer(context: Context, uniqueIdentifier: String) {
        if (!isInitialized) throw IllegalStateException("Uninitialized renderer!")
        val compatibleRenderers = getCompatibleRenderers(context).second
        currentRenderer = compatibleRenderers.find { it.getUniqueIdentifier() == uniqueIdentifier } ?: run {
            val renderer = compatibleRenderers[0]
            Logging.w("runGame", "Incompatible renderer $uniqueIdentifier will be replaced with ${renderer.getUniqueIdentifier()} (${renderer.getRendererName()})")
            renderer
        }
    }

    /**
     * 获取当前的渲染器
     */
    fun getCurrentRenderer(): AbstractRenderer {
        if (!isInitialized) throw IllegalStateException("Uninitialized renderer!")
        return currentRenderer ?: throw IllegalStateException("Current renderer not set")
    }

    /**
     * 当前是否设置了渲染器
     */
    fun isCurrentRendererValid(): Boolean = isInitialized && this.currentRenderer != null
}