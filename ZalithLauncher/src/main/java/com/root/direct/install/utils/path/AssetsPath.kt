package com.root.direct.install.utils.path

const val PATH = "components/app_config"

const val LANG = "$PATH/lang.xml"
const val DEFAULT_CONTROL = "default.json"
const val CONFIG_VERSION = "$PATH/version"
const val RULES = "$PATH/launcher_rules.json"
const val SETTINGS = "$PATH/settings.properties"
const val AUTH_SERVER = "$PATH/auth_servers.json"
const val GAME_VERSION = "components/.minecraft/version"
const val CUSTOM_RENDERER = "$PATH/custom_renderer.json"
const val LAUNCHER_CONFIG = "$PATH/launcher_settings.json"

fun addPrefix(path: String): String = if(path.startsWith("/")) "assets$path" else "/assets/$path"