package com.gproductions.card_against_humanity

import java.io.Serializable

data class User(
    /**
     * Class Users
     * */
    var nickname: String? = null,
    var uid: String? = null,
    var points: Double = .0,
    var matchPlayed: Int = 0,
    var matchName: String = "nil"
) : Serializable
