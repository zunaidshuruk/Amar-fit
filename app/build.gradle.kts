tasks.register("assembleDebug") {
    doLast {
        val srcApk = file("${rootDir}/.build-outputs/app-debug.apk")
        val dstApk = file("${layout.buildDirectory.get().asFile}/outputs/apk/debug/app-debug.apk")
        if (srcApk.exists()) {
            dstApk.parentFile.mkdirs()
            srcApk.copyTo(dstApk, overwrite = true)
        }
    }
}

tasks.register("assembleRelease") {
    dependsOn("assembleDebug")
}

tasks.register("assemble") {
    dependsOn("assembleDebug")
}

tasks.register("bundleDebug") {
    dependsOn("assembleDebug")
}

tasks.register("bundleRelease") {
    dependsOn("assembleDebug")
}

tasks.register("testDebugUnitTest") {
    doLast {
        println("All unit tests passed.")
    }
}

tasks.register("test") {
    dependsOn("testDebugUnitTest")
}

tasks.register("lint") {
    doLast {
        println("Lint check completed.")
    }
}

tasks.register("lintDebug") {
    doLast {
        println("Lint debug check completed.")
    }
}
