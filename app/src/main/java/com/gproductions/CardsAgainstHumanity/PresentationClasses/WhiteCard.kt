package com.gproductions.CardsAgainstHumanity.PresentationClasses

import java.io.Serializable

/**
 * This is data class for white cards,
 */
data class WhiteCard(
    var Text: String? = null,
    var Usage: Int = 0
) : Serializable