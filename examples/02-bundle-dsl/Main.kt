package examples.bundledsl

import io.github.termtate.kotlinosc.arg.OscTimetag
import io.github.termtate.kotlinosc.type.oscBundle

fun main() {
    val bundle = oscBundle(OscTimetag.IMMEDIATELY) {
        message("/session/start", "demo")
        message("/synth/freq", 440, 0.8f)
        bundle {
            message("/synth/gate", true)
            message("/fx/reverb/mix", 0.35f)
        }
    }

    println("Bundle is immediate: ${bundle.isImmediately()}")
    println(bundle)
}
