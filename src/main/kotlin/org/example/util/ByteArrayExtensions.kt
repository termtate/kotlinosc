package org.example.util

import java.util.Locale.getDefault

internal fun ByteArray.asString() =
    joinToString(" ") {
        "%02x".format(it).uppercase(getDefault())
    }
