package com.gproductions.card_against_humanity

import java.io.Serializable

data class Match(
    var name: String? = null,
    var language: String? = null,
    var passkey: String? = null,
    var active: Boolean? = null,
    var distributing: ArrayList<String> = ArrayList(),
    var dealer: String?= null,
    var round: Int = 0,
    var rounds: Int? = null,
    var winner: String?= null,
    var actualBlackCard: BlackCard = BlackCard(""),
    var players: ArrayList<String> = ArrayList(),
    var playersPoints: HashMap<String, Int> = HashMap(),
    var playersCards: HashMap<String, ArrayList<WhiteCard>> = HashMap(),
    var playersChoices: HashMap<String, ArrayList<WhiteCard>> = HashMap(),
    var blackCards: ArrayList<Int> = ArrayList(),
    var whiteCards: ArrayList<Int> = ArrayList()
) : Serializable