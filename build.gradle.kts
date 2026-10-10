plugins {
    alias(libs.plugins.android.application) apply false
}

layout.buildDirectory.set(layout.projectDirectory.dir(".build_root"))
