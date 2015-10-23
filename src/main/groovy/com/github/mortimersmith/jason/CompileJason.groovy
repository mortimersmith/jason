package com.github.mortimersmith.jason

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class CompileJason extends DefaultTask {

    @InputDirectory
    def File input = project.file(project.extensions.jason.input)

    @OutputDirectory
    def File outputDir = project.file(project.extensions.jason.output)

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        com.github.mortimersmith.jason.Compiler.compile(input.toPath(), outputDir.toPath())
    }
}
