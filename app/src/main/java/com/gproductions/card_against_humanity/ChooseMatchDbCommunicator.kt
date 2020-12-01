package com.gproductions.card_against_humanity

import android.content.res.Resources
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration

/**
 * This class inherit from DbCommunicator and it's used by activity to communicate with firebase db,
 * @param activity caller activity class,
 */
open class ChooseMatchDbCommunicator(activity: ChooseMatchActivity) : DbCommunicator() {
    // Debug TAG
    private val tag = "DEBUG_MATCHES_COMM"

    // Variables
    private var activity: ChooseMatchActivity? = null
    private var resources: Resources? = null
    private var matchListener: ListenerRegistration? = null

    init {
        this.activity = activity
        resources = activity.resources
    }

    /**
     * This function is used to check if match name is not already used,
     * @param matchName match name to search
     * - Query db match searching for match with matchName passed as parameter,
     * - If not used enable create button, otherwise show error and disable create button,
     */
    fun checkIfNameIsUsedInDB(matchName: String) {
        Log.d(tag, "Checking match name text.")

        // Searching for match with same name in db
        val dRef = db.collection("matches")
            .document(matchName)
            .get()

        // Getting results
        dRef.addOnSuccessListener { doc ->
            if (doc != null && doc.exists()) {
                // Match name already used
                Log.d(tag, "Name already used.")

                // Disable match create button
                (activity as ChooseMatchActivity).disableCreate()
                // Show error to user
                (activity as ChooseMatchActivity).showError(resources?.getString(R.string.already_used_name).toString())
            } else {
                // Match name not used
                Log.d(tag, "Match name feasible.")
                // Enable match create button
                (activity as ChooseMatchActivity).enableCreate()
            }
        }
            .addOnFailureListener { e ->
                Log.d(tag, "Error checking match name.$e")
            }
    }

    /**
     * This function is used to delete user from match players in db,
     * @param user to remove
     * @param by code to get calling function
     * - Query db searching for user match,
     * - If present
     * -  If there aren't enough players to continue, after user removal, delete it,
     * -  Otherwise remove player an if player was dealer one set new dealer as first player,
     */
    fun removePlayerInDB(user: User, by: String) {
        Log.d(tag, "Deleting user from match in db.")

        // Getting match db reference
        val dRef = db.collection("matches")
            .document(user.matchName)
            .get()

        // Getting results
        dRef.addOnSuccessListener { doc ->
            if (doc != null && doc.exists()) {
                val dbMatch: Match = doc.toObject(Match::class.java) as Match

                // Remove user
                dbMatch.players.remove(user.uid)

                // If match is running and player count is less than minimum required delete the match,
                if ((dbMatch.active == true &&
                            dbMatch.players.size < resources?.getInteger(R.integer.MIN_PLAYERS) as Int) ||
                    (dbMatch.active == false &&
                            dbMatch.dealer == user.uid)
                ) {
                    // Delete match from db
                    db.collection("matches")
                        .document(dbMatch.name as String)
                        .delete()
                        .addOnSuccessListener {
                            Log.d(tag, "Match deleted from db.")
                            onRemovePlayerSuccess(by)
                        }
                        .addOnFailureListener { e ->
                            Log.d(tag, "Error deleting match. $e")
                            // Show error to user
                            (activity as ChooseMatchActivity).showError(resources?.getString(R.string.error_update) + "Full match.")
                        }
                } else {
                    // Otherwise delete only user from players list
                    // If player actual user is dealer, choose another one (first available)
                    if (dbMatch.dealer == user.uid) {
                        // Update dealer
                        db.collection("matches")
                            .document(user.matchName)
                            .update("dealer", dbMatch.players[0])
                            .addOnSuccessListener {
                                Log.d(tag, "Dealer updated")
                            }
                            .addOnFailureListener { e ->
                                Log.d(tag, "Error updating dealer. $e")
                                // Show error to user
                                (activity as ChooseMatchActivity).showError(resources?.getString(R.string.error_update) + "Setting new dealer.")
                            }
                    }

                    // Deleting user from players
                    db.collection("matches")
                        .document(dbMatch.name as String)
                        .update("players", FieldValue.arrayRemove(user.uid as String))
                        .addOnSuccessListener {
                            Log.d(tag, "User deleted from match in db")
                            onRemovePlayerSuccess(by)
                        }
                        .addOnFailureListener { e ->
                            Log.e(tag, "Error deleting user from match in db. $e")
                            // Show error to user
                            (activity as ChooseMatchActivity).showError(resources?.getString(R.string.error_update) + "Player.")
                        }
                }
            } else {
                Log.d(tag, "Error getting match.")
            }
        }
    }

    /**
     * This callback is called when player is removed from match in db,
     * @param by code to get calling function
     */
    private fun onRemovePlayerSuccess(by: String) {
        (activity as ChooseMatchActivity).afterPlayerRemove(by)
    }

    /**
     * This function add a listener for matches in db,
     * @param uid uid of user adding this listener
     * @param language language of match searched
     * - Set a listener on ready match in same language as user,
     * - On changing event add new match to ready match scroll view,
     */
    fun addReadyMatchesListenerInDB(uid: String, language: String) {
        Log.d(tag, "Adding listener on match changing event."
        )
        // Get db table
        val dRef = db.collection("matches")
            .whereEqualTo("active", false)
            .whereEqualTo("language", language)

        // Initialize listener value
        matchListener = dRef.addSnapshotListener { snapshot, e ->
            Log.d(tag, "Match changing event occurred.")

            // Check for error
            if (e != null) {
                Log.d(tag, "Listening for ready matches error. $e")
                return@addSnapshotListener
            }
            // Clear ready matches
            (activity as ChooseMatchActivity).clearReadyMatches()

            // Check snapshot
            if (snapshot != null && snapshot.documents.isNotEmpty()) {
                for (snap in snapshot) {
                    // Get a match
                    val match: Match = snap.toObject(Match::class.java)

                    // Check players count
                    if (match.players.size >= resources?.getInteger(R.integer.MAX_PLAYERS) as Int)
                        continue

                    // Check if user is match dealer
                    if (match.dealer == uid)
                        continue

                    // Add ready match to list
                    (activity as ChooseMatchActivity).addReadyMatchView(match)
                }
            } else {
                Log.d(tag, "Ready match list is empty.")
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
     * This callback is called when match is set in db,
     * - Update local user,
     */
    override fun onSetMatchSuccess() {
        // Set user as match dealer
        (activity as ChooseMatchActivity).prepareDealer()
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onSetMatchFailure() {
        // Show error to user
        (activity as ChooseMatchActivity).showError(resources?.getString(R.string.error_update).toString())
        // Go nickname activity
        (activity as ChooseMatchActivity).goNicknameActivity()
    }

    /**
     * This callback is called when match is retrieved from db,
     * - Update local match,
     */
    override fun onGetMatchSuccess(match: Match, by: String) {
        when (by) {
            "resume"-> {
                    // Update active match to resume
                    (activity as ChooseMatchActivity).updateMatchToResume(match)}
            "check" -> {
                    // Enable resume
                    (activity as ChooseMatchActivity).enableReturn()}
        }
    }
    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onGetMatchFailure(by: String) {
        when (by) {
            "resume"-> {
                // Show error to user
                (activity as ChooseMatchActivity).showError(resources?.getString(R.string.error_returning).toString())
                // Go to Nickname activity
                //(activity as ChooseMatchActivity).goNicknameActivity()
                }
        }
    }

    /**
     * This callback is called when user is updated in db,
     * - Go to WaitPlayers activity,
     */
    override fun onUpdateUserSuccess() {
        // Go to the next activity
        (activity as ChooseMatchActivity).goWaitPlayersActivity()
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onUpdateUserFailure() {
        // Show error to user
        (activity as ChooseMatchActivity).showError(resources?.getString(R.string.error_update).toString())
        // Go Nickname activity
        (activity as ChooseMatchActivity).goNicknameActivity()
    }

    /**
     * This callback is called when match is updated in db
     * @param by code to get calling function
     */
    override fun onUpdateMatchSuccess(by: String) {
        // return to activity
        (activity as ChooseMatchActivity).playerAddedInMatch()
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onUpdateMatchFailure() {
        // Show error to user
        (activity as ChooseMatchActivity).showError(resources?.getString(R.string.error_adding_user).toString())
        // Go Nickname activity
        (activity as ChooseMatchActivity).goNicknameActivity()
    }

    override fun onMatchListenerEvent(match: Match, by: String) {}
    override fun onMatchListenerFailure() {}
    override fun onMatchDeleted() {}
    override fun onDeleteMatchSuccess() {}
    override fun onDeleteMatchFailure() {}
}
