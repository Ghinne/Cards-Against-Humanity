package com.gproductions.card_against_humanity

import java.io.Serializable

/**
 * This is data class for black cards,
 */
data class BlackCard (
    var Text: String? = null,
    var Usage: Int = 0
) : Serializable