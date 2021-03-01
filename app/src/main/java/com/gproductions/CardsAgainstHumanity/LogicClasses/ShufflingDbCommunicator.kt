package com.gproductions.CardsAgainstHumanity.LogicClasses

import android.content.res.Resources
import android.util.Log
import com.gproductions.CardsAgainstHumanity.PresentationClasses.Match
import com.gproductions.CardsAgainstHumanity.R
import com.gproductions.CardsAgainstHumanity.PresentationClasses.ShufflingActivity
import com.gproductions.CardsAgainstHumanity.PresentationClasses.User
import java.util.*
import kotlin.collections.ArrayList

/**
 * This class inherit from DbCommunicator and it's used by activity to communicate with firebase db,
 * @param activity caller activity class,
 */
open class ShufflingDbCommunicator(activity: ShufflingActivity) : DbCommunicator() {
    // Debug TAG
    private val tag = "DEBUG_SHUFFLING_COMM"

    // Variables
    private var activity: ShufflingActivity? = null
    private var resources: Resources? = null
    private val random = Random(System.currentTimeMillis())

    init {
        this.activity = activity
        resources = activity.resources
    }

    /**
     * This function is used to retrieve all black cards from db and shuffle them,
     * @param language of needed black cards
     * - Get black cards indexes,
     * - Shuffle them,
     */
    fun getShuffledBlackCards(language: String) {
        /**
         * This function retrieve all black cards shuffled from db,
         */
        val cards: ArrayList<Int> = ArrayList()
        // Get db black cards count
        db.collection("$language-black")
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    // Getting cards list
                    for (i in 0 until documents.count()) {
                        cards.add(i)
                    }
                    // Shuffle it
                    cards.shuffle(random)
                    Log.w(tag, "Black card shuffled.")

                    // Set shuffled in activity
                    activity!!.setBlackShuffled(cards)
                } else {
                    Log.w(tag, "Black cards set not found.")
                    // Show error to user
                    activity!!.showError(resources!!.getString(R.string.error_cards) + "Black.")

                    // Go to nickname activity
                    activity!!.goNicknameActivity()
                }
            }
            .addOnFailureListener { e ->
                Log.d(tag, "Error getting black cards.$e")
                // Show error to user
                activity!!.showError(resources!!.getString(R.string.error_cards) + "Black.")
                // Go to nickname activity
                activity!!.goNicknameActivity()
            }
    }

    /**
     * This function is used to retrieve all white cards from db and shuffle them,
     * @param language of needed white cards
     * - Get white cards indexes,
     * - Shuffle them,
     */
    fun getShuffledWhiteCards(language: String) {
        val cards: ArrayList<Int> = ArrayList()
        // Get db white cards count
        db.collection("$language-white")
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    // Getting cards list
                    for (i in 0 until documents.count()) {
                        cards.add(i)
                    }
                    // Shuffle them
                    cards.shuffle(random)
                    Log.w(tag, "White card shuffled.")

                    // Set shuffled
                    activity!!.setWhiteShuffled(cards)
                } else {
                    Log.w(tag, "white cards set not found.")
                    // Show error to user
                    activity!!.showError(resources!!.getString(R.string.error_cards) + "White.")

                    // Go to nickname activity
                    activity!!.goNicknameActivity()
                }
            }
            .addOnFailureListener { e ->
                Log.d(tag, "Error getting white cards. $e")
                // Show error to user
                activity!!.showError(resources!!.getString(R.string.error_cards) + "White.")
                // Go to nickname activity
                activity!!.goNicknameActivity()
            }
    }

    override fun onCheckUsedNicknameSuccess(used: Boolean) {}
    override fun onCheckUsedNicknameFailure() {}
    override fun onSetUserSuccess() {}
    override fun onSetUserFailure() {}
    override fun onGetUserSuccess(user: User) {}
    override fun onGetUserFailure() {}
    override fun onUpdateUserSuccess() {}
    override fun onUpdateUserFailure() {}
    override fun onSetMatchSuccess() {}
    override fun onSetMatchFailure() {}

    /**
     * This callback is called when match is retrieved from db,
     * @param match updated match
     * @param by code to get calling function
     * - Update local match,
     */
    override fun onGetMatchSuccess(match: Match, by: String) {
        activity!!.updateLocalMatch(match)
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onGetMatchFailure(by: String) {
        // Show error to user
        activity!!.showError(resources!!.getString(R.string.error_match_cancelled))
        // Go in Nickname activity
        activity!!.goNicknameActivity()
    }

    /**
     * This callback is called when match is updated in db,
     * @param by code to get calling function
     */
    override fun onUpdateMatchSuccess(by: String) {
        // Update bundle and return to distributing activity
        activity!!.goDistributingActivity()
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onUpdateMatchFailure() {
        // Show error to user
        activity!!.showError(resources!!.getString(R.string.error_cards))
    }

    override fun onMatchListenerEvent(match: Match, by: String) {}
    override fun onMatchListenerFailure() {}
    override fun onMatchDeleted() {}
    override fun onDeleteMatchSuccess() {}
    override fun onDeleteMatchFailure() {}
}