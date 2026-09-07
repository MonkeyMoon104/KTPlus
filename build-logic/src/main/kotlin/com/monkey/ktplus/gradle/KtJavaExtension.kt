package com.monkey.ktplus.gradle

import org.gradle.api.provider.Property

abstract class KtJavaExtension {
    abstract val toolchain: Property<Int>
    abstract val release: Property<Int>
    abstract val testRelease: Property<Int>
    abstract val injectReleaseArg: Property<Boolean>
    abstract val werror: Property<Boolean>
}
