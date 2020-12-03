package com.gproductions.card_against_humanity

import java.io.Serializable

/**
 * This data class is used for users,
 */
data class User(
    var nickname: String? = null,
    var uid: String? = null,
    var points: Double = .0,
    var matchPlayed: Int = 0,
    var matchName: String = "nil"
) : Serializable
