package com.gproductions.card_against_humanity

import android.content.res.Resources
import android.util.Log

/**
 * This class inherit from DbCommunicator and it's used by activity to communicate with firebase db,
 * @param activity caller activity class,
 */
open class DistributingDbCommunicator(activity: DistributingActivity) : DbCommunicator() {
    // Debug TAG
    private val tag = "DEBUG_DISTRIBUTING_COMM"

    // Variables
    private var activity: DistributingActivity? = null
    private var resources: Resources? = null

    init {
        this.activity = activity
        resources = activity.resources
    }

    /**
     * This method retrieve white card of passed language with passed id from db,
     * @param uid of user requiring that card
     * @param id of card
     * @param language of card set
     * - Query db searching for required card,
     * - Once find give that card to requiring player,
     */
    fun getWhiteCardInDB(uid: String, id: Int, language: String) {
        Log.d(tag, "Getting $id card from db.")

        // Getting card from db
        db.collection(("$language-white"))
            .document(id.toString())
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    // Add retrieved card to user ones
                    (activity as DistributingActivity).setPlayerCard(uid, doc.toObject(WhiteCard::class.java) as WhiteCard)
                } else {
                    Log.d(tag, "Card $id not found.")
                }
            }
            .addOnFailureListener { e ->
                Log.d(tag, "Error getting $id card. $e")
                (activity as DistributingActivity).showError(resources!!.getString(R.string.error_no_card))
            }
    }

    /**
     * This method retrieve black card of passed language with passed id from db,
     * @param id of card
     * @param language of card set
     * - Query db searching for required card,
     * - Once find, set black card in match,
     */
    fun getBlackCardInDB(id: Int, language: String) {
        Log.d(tag, "Getting $id black card from db.")

        // Getting card from db
        db.collection("$language-black")
            .document(id.toString())
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    Log.d(tag, "Black card retrieved."
                    )
                    // Set black card in match
                    (activity as DistributingActivity).setBlackCard(doc.toObject(BlackCard::class.java) as BlackCard)
                } else {
                    Log.d(tag, "Card $id not found.")
                }
            }
            .addOnFailureListener { e ->
                Log.d(tag, "Error getting $id card. $e")
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

    /**
     * This callback is called when match is set in db,
     * - Go to Game activity,
     */
    override fun onSetMatchSuccess() {
        // Go next activity
        (activity as DistributingActivity).goGameActivity()
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onSetMatchFailure() {
        // Show error to user
        (activity as DistributingActivity).showError(resources!!.getString(R.string.error_update))
        // Go to Nickname activity
        (activity as DistributingActivity).goNicknameActivity()
    }

    /**
     * This callback is called when match is retrieved from db,
     * - Update local match,
     */
    override fun onGetMatchSuccess(match: Match, by: String) {
        // Update activity match and go to next activity
        (activity as DistributingActivity).updateMatch(match)
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onGetMatchFailure(by: String) {
        // Show error to user
        (activity as DistributingActivity).showError(resources!!.getString(R.string.error_no_matches))
        // Go to Nickname activity
        (activity as DistributingActivity).goNicknameActivity()
    }

    /**
     * This callback is called when match is updated in db,
     * @param by code to get calling function
     */
    override fun onUpdateMatchSuccess(by: String) {
        when(by){
            "cards" -> {// Adding new cards to empty cards set
                        (activity as DistributingActivity).goShuffleActivity()}
        }
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onUpdateMatchFailure() {
        // Show error to user
        (activity as DistributingActivity).showError(resources!!.getString(R.string.error_cleaning_cards))
        // Go to Nickname activity
        (activity as DistributingActivity).goNicknameActivity()
    }

    /**
     * This callback is called when match change in db,
     * @param match match listened
     * @param by code to get calling function
     * - If caller is player and distributing is finished get updated match in db,
     * - If caller is dealer and all player in distributing remove listener for players and distribute cards,
     */
    override fun onMatchListenerEvent(match: Match, by: String) {
        when(by) {
            "player" -> {
                if(match.distributing.isEmpty()) {
                    Log.d(tag, "Cards distributed going to next activity."
                    )
                    // Disable listener
                    removeMatchListener()

                    // Update user and match in bundle
                    getMatchInDB(match.name as String)
                }
            }
            "dealer" -> {
                if (match.distributing.containsAll(match.players))                 {
                    Log.d(tag, "All users in distributing, launch distributing.")
                    // Disable listener
                    removeMatchListener()
                    // Distribute cards
                    (activity as DistributingActivity).launchDistribute()
                }
            }
        }
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onMatchListenerFailure() {
        // Show error to user
        (activity as DistributingActivity).showError(resources!!.getString(R.string.error_no_matches))
        // Go to Nickname activity
        (activity as DistributingActivity).goNicknameActivity()
    }

    override fun onMatchDeleted() {}
    override fun onDeleteMatchSuccess() {}
    override fun onDeleteMatchFailure() {}

}