package com.gproductions.card_against_humanity

import java.io.Serializable

/**
 * This is data class for white cards,
 */
data class WhiteCard(
    var Text: String? = null,
    var Usage: Int = 0
) : Serializable