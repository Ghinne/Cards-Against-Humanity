package com.gproductions.CardsAgainstHumanity.PresentationClasses

import java.io.Serializable

/**
 * This is data class for black cards,
 */
data class BlackCard (
    var Text: String? = null,
    var Usage: Int = 0
) : Serializable