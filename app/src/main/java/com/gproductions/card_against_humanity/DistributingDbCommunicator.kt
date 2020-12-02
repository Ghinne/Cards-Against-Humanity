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
                    activity!!.setPlayerCard(uid, doc.toObject(WhiteCard::class.java) as WhiteCard)
                } else {
                    Log.d(tag, "Card $id not found.")
                }
            }
            .addOnFailureListener { e ->
                Log.d(tag, "Error getting $id card. $e")
                activity!!.showError(resources!!.getString(R.string.error_no_card))
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
                    activity!!.setBlackCard(doc.toObject(BlackCard::class.java) as BlackCard)
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
        activity!!.goGameActivity()
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onSetMatchFailure() {
        // Show error to user
        activity!!.showError(resources!!.getString(R.string.error_update))
        // Go to Nickname activity
        activity!!.goNicknameActivity()
    }

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
        activity!!.showError(resources?.getString(R.string.error_match_cancelled).toString())
        // Go in Nickname activity
        activity!!.goNicknameActivity()
    }

    /**
     * This callback is called when match is updated in db,
     * @param by code to get calling function
     */
    override fun onUpdateMatchSuccess(by: String) {
        when(by){
            "cards" -> {// Adding new cards to empty cards set
                        activity!!.goShuffleActivity()}
        }
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onUpdateMatchFailure() {
        // Show error to user
        activity!!.showError(resources!!.getString(R.string.error_cleaning_cards))
        // Go to Nickname activity
        activity!!.goNicknameActivity()
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

                    // Go back to Game activity
                    activity!!.goGameActivity()
                }
            }
            "dealer" -> {
                if (match.distributing.containsAll(match.players))                 {
                    Log.d(tag, "All users in distributing, launch distributing.")
                    Log.d(tag, "DEALER $match ${auth.uid}")

                    // Disable listener
                    removeMatchListener()
                    // Distribute cards
                    activity!!.launchDistribute()
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
        activity!!.showError(resources!!.getString(R.string.error_no_matches))
        // Go to Nickname activity
        activity!!.goNicknameActivity()
    }

    override fun onMatchDeleted() {}
    override fun onDeleteMatchSuccess() {}
    override fun onDeleteMatchFailure() {}

}