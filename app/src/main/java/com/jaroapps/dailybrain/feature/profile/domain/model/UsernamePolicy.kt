package com.jaroapps.dailybrain.feature.profile.domain.model

import java.util.Locale

/** Public handles are case-insensitive and contain 3-20 ASCII Latin letters, digits or underscores. */
object UsernamePolicy {
    private val pattern = Regex("^[a-z0-9_]{3,20}$")

    fun normalize(input: String): String =
        input.trim().removePrefix("@").lowercase(Locale.ROOT)

    fun isValid(input: String): Boolean = pattern.matches(normalize(input))

    fun sanitizeInput(input: String): String =
        input.removePrefix("@")
            .filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' }
            .take(20)
}
