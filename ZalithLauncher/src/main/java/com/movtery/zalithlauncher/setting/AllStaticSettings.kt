package com.movtery.zalithlauncher.setting

/**
 * 静态设置项的值，用于一些临时生效的设置项使用
 * 这里的值不会被保存到设置配置中，软件重启就会消失！
 */
class AllStaticSettings {
    companion object {
        /**
         * 缩放因子 Float
         */
        @JvmField var scaleFactor = AllSettings.resolutionRatio / 100f

        /**
         * 禁用双击交换手中物品 Boolean
         */
        @JvmField var disableDoubleTap = AllSettings.disableDoubleTap

        /**
         * 触发长按延迟 Int
         */
        @JvmField var timeLongPressTrigger = AllSettings.timeLongPressTrigger

        /**
         * 启用陀螺仪控制 Boolean
         */
        @JvmField var enableGyro = AllSettings.enableGyro

        /**
         * 陀螺仪控制灵敏度 Int
         */
        @JvmField var gyroSensitivity = AllSettings.gyroSensitivity

        /**
         * 陀螺仪反转X轴 Boolean
         */
        @JvmField var gyroInvertX = AllSettings.gyroInvertX

        /**
         * 陀螺仪反转Y轴 Boolean
         */
        @JvmField var gyroInvertY = AllSettings.gyroInvertY
    }
}