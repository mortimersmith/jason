package com.github.mortimersmith.jason

import org.gradle.api.Project
import org.gradle.api.Plugin

class Plugin implements Plugin<Project> {
    void apply(Project project) {
        project.extensions.create("jason", JasonPluginExtension)
        project.task("compileJason", type: CompileJason)
        project.tasks.compileJava.dependsOn("compileJason")
        project.sourceSets.main.java.srcDirs += "${project.extensions.jason.output}"
    }
}
