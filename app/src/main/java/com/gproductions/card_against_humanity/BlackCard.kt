package com.gproductions.card_against_humanity

import java.io.Serializable

data class BlackCard (
    var Text: String? = null,
    var Usage: Int = 0
) : Serializable