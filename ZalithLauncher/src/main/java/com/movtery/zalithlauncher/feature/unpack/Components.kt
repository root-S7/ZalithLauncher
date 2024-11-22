package com.movtery.zalithlauncher.feature.unpack

import com.movtery.zalithlauncher.R

enum class Components(val component: String, val displayName: String, val summary: Int?, val privateDirectory: Boolean) {
    OTHER_LOGIN("other_login", "authlib-injector", R.string.splash_screen_authlib_injector, false),
    CACIOCAVALLO("caciocavallo", "caciocavallo", R.string.splash_screen_cacio, false),
    CACIOCAVALLO17("caciocavallo17", "caciocavallo 17", R.string.splash_screen_cacio, false),
    LWJGL3("lwjgl3", "LWJGL 3", R.string.splash_screen_lwjgl, false),
    SECURITY("security", "Pro Grade", R.string.splash_screen_pro_grade, true),
    LOADER_INSTALLERS("installer", "Loader Installers", R.string.splash_screen_loader_installers, true),
}