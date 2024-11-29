package com.movtery.zalithlauncher.utils.stringutils

class StringUtilsKt {
    companion object {
        @JvmStatic
        fun getNonEmptyOrBlank(string: String?): String? {
            return string?.takeIf { it.isNotEmpty() && it.isNotBlank() }
        }

        @JvmStatic
        fun isBlank(string: String?): Boolean = string.isNullOrBlank()

        @JvmStatic
        fun isNotBlank(string: String?): Boolean = string?.isNotBlank() ?: false

        @JvmStatic
        fun isEmptyOrBlank(string: String): Boolean = string.isEmpty() || string.isBlank()

        @JvmStatic
        fun removeSuffix(string: String, suffix: String) = string.removeSuffix(suffix)

        @JvmStatic
        fun removePrefix(string: String, prefix: String) = string.removePrefix(prefix)
    }
}