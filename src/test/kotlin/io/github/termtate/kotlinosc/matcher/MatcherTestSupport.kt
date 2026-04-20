package io.github.termtate.kotlinosc.matcher

import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.pattern.OscAddressMatcher

internal val strictMatcher = OscAddressMatcher(OscConfig.AddressPattern.default)
internal val looseMatcher = OscAddressMatcher(OscConfig.AddressPattern(strictAddressPattern = false))
