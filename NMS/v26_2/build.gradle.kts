plugins {
    id("ktplus.nms.mojmap")
}

ktJava {
    toolchain.set(25)
    release.set(25)
    testRelease.set(25)
}

ktPaperweight {
    launcherVersion.set(25)
}
