package com.root.direct.install.feature.check.rule

import kotlin.reflect.KClass

/**
 * FileType枚举绑定一个默认的检测规则
**/
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class DefaultCheck(val ruleClass: KClass<out FileCheckRule>)