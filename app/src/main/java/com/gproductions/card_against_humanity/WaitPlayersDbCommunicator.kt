package com.gproductions.card_against_humanity

import android.content.res.Resources
import android.util.Log
import com.google.firebase.firestore.FieldValue

/**
 * This class inherit from DbCommunicator and it's used by activity to communicate with firebase db,
 * @param activity caller activity class,
 */
open class WaitPlayersDbCommunicator(activity: WaitPlayers) : DbCommunicator() {
    // Debug TAG
    private val tag = "DEBUG_WAIT_PLAYERS_COMM"

    // Variables
    private var activity: WaitPlayers? = null
    private var resources: Resources? = null

    init {
        this.activity = activity
        resources = activity.resources
    }

    /**
     * This function is used to delete user from match players in db,
     * @param user to remove
     * @param match match to search in db
     * - Query db searching for user match,
     * - If player is dealer delete match,
     * - Otherwise remove player an if player was dealer one set new dealer as first player,
     */
    fun removePlayerInDB(user: User, match: Match) {
        Log.d(tag, "Deleting player from match in db.")

        // Getting match reference
        val dRef = db.collection("matches")
            .document(match.name as String)

        if (match.dealer == user.uid) {
            // Delete match from db
            dRef.delete()
                .addOnSuccessListener {
                    Log.d(tag, "Match deleted from db.")
                }
                .addOnFailureListener { e ->
                    Log.d(tag, "Error deleting match. $e")
                    // Show error to user
                    (activity as WaitPlayers).showError(resources?.getString(R.string.error_removing_match).toString())
                    // Going to Choose nickname activity
                    (activity as WaitPlayers).goNicknameActivity()
                }

        } else {
            dRef.get()
                .addOnSuccessListener { doc ->
                    // Otherwise delete only user from players list
                    if (doc != null && doc.exists()) {
                        db.collection("matches")
                            .document(match.name as String)
                            .update("players", FieldValue.arrayRemove(user.uid as String))
                            .addOnSuccessListener {
                                Log.d(tag, "User deleted from match in db")
                            }
                            .addOnFailureListener { e ->
                                Log.d(tag, "Error deleting user from match in db. $e")
                                // Show error to user
                                (activity as WaitPlayers).showError(resources?.getString(R.string.error_removing_match).toString())
                                // Going to Choose nickname activity
                                (activity as WaitPlayers).goNicknameActivity()
                            }
                    } else {
                        Log.d(tag, "User not found.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(tag, "Error getting match from which delete user. $e")
                    // Show error to user
                    (activity as WaitPlayers).showError(resources?.getString(R.string.error_removing_match).toString())
                    // Going to Choose nickname activity
                    (activity as WaitPlayers).goNicknameActivity()
                }
        }
    }

    override fun onCheckUsedNicknameSuccess(used: Boolean) {}
    override fun onCheckUsedNicknameFailure() {}
    override fun onSetUserSuccess() {}
    override fun onSetUserFailure() {}
    override fun onGetUserSuccess(user: User) {}
    override fun onGetUserFailure() {}

    /**
     * This callback is called when user is updated in db,
     * - Go to ChooseMatch activity,
     */
    override fun onUpdateUserSuccess() {
        // Go to the previous activity
        (activity as WaitPlayers).goChooseMatchActivity()
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onUpdateUserFailure() {
        // Show error to user
        (activity as WaitPlayers).showError(resources?.getString(R.string.error_update).toString())
        // Go Nickname activity
        (activity as WaitPlayers).goNicknameActivity()
    }

    override fun onSetMatchSuccess() {}
    override fun onSetMatchFailure() {}
    override fun onGetMatchSuccess(match: Match) {}
    override fun onGetMatchFailure() {}

    /**
     * This callback is called when match is changed in db
     * @param match updated match
     * @param by code to get calling function
     * - Update local match,
     */
    override fun onMatchListenerEvent(match: Match, by: String) {
        // Update in activity
        (activity as WaitPlayers).resultMatchListener(match)
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onMatchListenerFailure() {
        // Show error to user
        (activity as WaitPlayers).showError(resources?.getString(R.string.error_match_cancelled).toString())
        // Go in Nickname activity
        (activity as WaitPlayers).goNicknameActivity()
    }

    override fun onDeleteMatchSuccess() {}
    override fun onDeleteMatchFailure() {}

    /**
     * This callback is called when match is updated in db
     * @param by code to get calling function
     * - Go to Game activity,
     */
    override fun onUpdateMatchSuccess(by: String) {
        // Going to GameActivity activity
        (activity as WaitPlayers).goGameActivity()
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onUpdateMatchFailure() {
        // Show error to user
        (activity as WaitPlayers).showError(resources?.getString(R.string.error_activating_match).toString())
    }
}
