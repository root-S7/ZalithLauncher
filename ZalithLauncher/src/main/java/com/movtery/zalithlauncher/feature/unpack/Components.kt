package com.movtery.zalithlauncher.feature.unpack

import com.movtery.zalithlauncher.R

enum class Components(val component: String, val displayName: String, val summary: Int?, val privateDirectory: Boolean) {
    OTHER_LOGIN("other_login", "authlib-injector", R.string.splash_screen_authlib_injector, false),
    CACIOCAVALLO("caciocavallo", "caciocavallo", R.string.splash_screen_cacio, false),
    CACIOCAVALLO17("caciocavallo17", "caciocavallo 17", R.string.splash_screen_cacio, false),
    LWJGL3("lwjgl3", "LWJGL 3", R.string.splash_screen_lwjgl, false),
    SECURITY("security", "Pro Grade", R.string.splash_screen_pro_grade, true),
    OPTIFINE_INSTALLER("forge_installer", "OptiFine Installer", R.string.splash_screen_optifine_installer, true),
    FORGE_INSTALL_BOOTSTRAPPER("forge_install_bootstrapper", "Forge Install Bootstrapper", R.string.splash_screen_forge_install_bootstrapper, true),
}