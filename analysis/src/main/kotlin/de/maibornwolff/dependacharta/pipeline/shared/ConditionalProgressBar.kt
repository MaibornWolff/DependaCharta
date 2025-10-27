package de.maibornwolff.dependacharta.pipeline.shared

import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle

class ConditionalProgressBar(
    private val name: String,
    private val steps: Int,
    private val condition: () -> Boolean
) {
    fun <T> use(block: (step: () -> Any) -> T): T =
        if (this.condition()) {
            Logger.w("'$name' consists of $steps steps. This might take a while.")
            ProgressBarBuilder()
                .setTaskName(name)
                .setInitialMax(steps.toLong())
                .setStyle(ProgressBarStyle.ASCII)
                .build()
                .use {
                    block { it.step() }
                }
        } else {
            block {}
        }
}