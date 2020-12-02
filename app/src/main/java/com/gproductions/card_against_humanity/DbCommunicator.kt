package com.gproductions.card_against_humanity

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * This is the abstract class used to communicate with Firebase and Firestore db,
 */
abstract class DbCommunicator {
    // Debug TAG
    private val tag = "DEBUG_COMMUNICATOR"

    // Firebase db reference
    protected val auth = Firebase.auth
    protected val db = Firebase.firestore

    // Listeners
    private var matchListener: ListenerRegistration? = null

    /**
     * This function return Firebase Authentication,
     */
    fun getAuthentication(): FirebaseAuth {
        return auth
    }

    /**
     * This function is used to check if nickname is already used in db,
     * @param nickname user nickname to query
     * - Run a query on db searching for user with nickname passed as parameter,
     * - Count how many users has that nickname,
     * - Call appropriate callbacks based on count,
     */
    fun checkUsedNicknameInDB(nickname: String) {
        Log.d(tag, "Checking nickname in db.")

        // Reset counter
        var count = 0
        // Check username usage in db for user different than actual one
        db.collection("users")
            .whereEqualTo("nickname", nickname)
            .get()
            .addOnSuccessListener { documents ->
                // If query found some users
                if (documents != null && documents.documents.isNotEmpty()) {
                    // For each user found
                    for (doc in documents.documents) {
                        // Increment counter if user has a different uid from actual one
                        if (doc.toObject(User::class.java)?.uid != auth.uid) {
                            count++
                        }
                    }
                }
                // If no users use this nickname other than actual one enable submit
                if (count == 0) {
                    onCheckUsedNicknameSuccess(false)
                } else {
                    onCheckUsedNicknameSuccess(true)
                }
            }
            .addOnFailureListener { e ->
                Log.d(tag, "Error getting users.$e")
                onCheckUsedNicknameFailure()
            }
    }

    abstract fun onCheckUsedNicknameSuccess(used: Boolean)
    abstract fun onCheckUsedNicknameFailure()

    /**
     * This function is used to set user data in db,
     * @param user user data to set in db
     * - Get user document and set user data passed as parameter in it,
     * - Call appropriate callback based on results,
     */
    fun setUserInDB(user: User) {
        Log.d(tag, "Updating/creating user in db.")
        // Get db user reference
        val dRef = db.collection("users").document(user.uid as String)
        // Update user in db
        dRef.set(user)
            .addOnSuccessListener {
                // If update is successful start match choosing activity else show error message
                Log.d(tag, "User created in db.")
                onSetUserSuccess()
            }
            .addOnFailureListener { e ->
                Log.d(tag, "Error in user creation.$e")
                onSetUserFailure()
            }
    }

    abstract fun onSetUserSuccess()
    abstract fun onSetUserFailure()

    /**
     * This function is used to get user data in db,
     * @param uid user uid to search in db
     * - Query db for user with uid passed as parameter data,
     * - Call appropriate callback based on results,
     */
    fun getUserInDB(uid: String) {
        Log.w(tag, "Getting user data.")
        // Getting user data in db
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                // If user document found
                if (doc != null && doc.exists()) {
                    Log.w(tag, "User retrieved.")
                    onGetUserSuccess(doc.toObject(User::class.java) as User)
                } else
                    Log.w(tag, "User NOT found.")
            }
            .addOnFailureListener { e ->
                Log.d(tag, "Error getting user.$e")
                onGetUserFailure()
            }
    }

    abstract fun onGetUserSuccess(user: User)
    abstract fun onGetUserFailure()

    /**
     * This function is used to update user in db,
     * @param uid user uid to search in db
     * @param map map of user data to update
     * - Query db for user with uid passed as parameter data,
     * - Update his data,
     * - Call appropriate callback based on results,
     */
    fun updateUserInDB(uid: String, map: HashMap<String, Any>) {
        Log.d(tag, "Updating id user in db.")

        // Get db user reference
        val dRef = db.collection("users")
            .document(uid)

        // Update it
        dRef.update(map)
            .addOnSuccessListener {
                Log.d(tag, "Id updated")
                onUpdateUserSuccess()
            }
            .addOnFailureListener { e ->
                // Error in updating db
                Log.d(tag, "Error updating user. $e")
                onUpdateUserFailure()
            }
    }

    abstract fun onUpdateUserSuccess()
    abstract fun onUpdateUserFailure()

    /**
     * This function is used to set match data in db,
     * @param match match data to set in db
     * - Get match document and set match data passed as parameter in it,
     * - Call appropriate callback based on results,
     */
    fun setMatchInDB(match: Match) {
        Log.d(tag, "Updating db match.")

        // Create db match reference
        val dRef = db.collection("matches")
            .document(match.name as String)
        // Update db
        dRef.set(match)
            .addOnSuccessListener {
                Log.d(tag, "Match sat")
                onSetMatchSuccess()
            }
            .addOnFailureListener { e ->
                // Error in updating db
                Log.d(tag, "Error in match setting.$e")
                onSetMatchFailure()
            }
    }

    abstract fun onSetMatchSuccess()
    abstract fun onSetMatchFailure()

    /**
     * This function is used to get match data in db,
     * @param matchName match name to search in db
     * - Query db for match with matchName passed as parameter data,
     * - Call appropriate callback based on results,
     */
    fun getMatchInDB(matchName: String, by: String="") {
        Log.d(tag, "Getting match in db.")
        // Getting user cards
        db.collection("matches")
            .document(matchName)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    Log.d(tag, "Match retrieved.")
                    // Return match
                    onGetMatchSuccess(doc.toObject(Match::class.java) as Match, by)
                } else {
                    Log.d(tag, "Error match not found.")
                    onGetMatchFailure(by)
                }
            }
            .addOnFailureListener { e ->
                Log.d(tag, "Error getting match. $e")
                onGetMatchFailure(by)
            }
    }

    abstract fun onGetMatchSuccess(match: Match, by: String="")
    abstract fun onGetMatchFailure(by: String="")

    /**
     * This function is used to update match in db,
     * @param matchName match name to search in db
     * @param map map of match data to update
     * - Query db for match with matchName passed as parameter data,
     * - Update his data,
     * - Call appropriate callback based on results,
     */
    fun updateMatchInDB(matchName: String, map: Map<String, Any>, by: String = "") {
        Log.d(tag, "Updating db match.")

        // Get db table
        val dRef = db.collection("matches")
            .document(matchName)

        // Update db
        dRef.update(map)
            .addOnSuccessListener {
                Log.d(tag, "Match updated.")
                onUpdateMatchSuccess(by)
            }
            .addOnFailureListener { e ->
                Log.d(tag, "Error updating match. $e")
                onUpdateMatchFailure()
            }
    }

    abstract fun onUpdateMatchSuccess(by: String = "")
    abstract fun onUpdateMatchFailure()

    /**
     * This function is used to add a listener for match updates in db,
     * @param matchName match name to search in db
     * @param by code to get calling function
     * - Add a listener on match with matchName passed as parameter,
     * - On event call appropriate callback,
     */
    fun addMatchListenerInDB(matchName: String, by: String = "") {
        Log.d(tag, "Adding listener to players count in match.")
        // Get db reference
        val dRef = db.collection("matches").document(matchName)

        // Add listener
        matchListener = dRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.d(tag, "Listening for ready players error. $e")
                onMatchListenerFailure()
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                Log.d(tag, "Players changing event occurred.")
                onMatchListenerEvent(snapshot.toObject(Match::class.java) as Match, by)
            } else {
                Log.w(tag, "Match has been cancelled.")
                onMatchDeleted()
            }
        }
    }

    abstract fun onMatchListenerEvent(match: Match, by: String = "")
    abstract fun onMatchListenerFailure()
    abstract fun onMatchDeleted()

    /**
     * This function is used to remove match listener,
     */
    fun removeMatchListener() {
        if (matchListener != null)
            matchListener?.remove()
    }

    /**
     * This function is used to remove a match from db,
     * @param matchName match name of match to remove
     * - Search match with passed matchName as parameter and delete it,
     */
    fun deleteMatchInDB(matchName: String) {
        // Removing match
        db.collection("matches")
            .document(matchName)
            .delete()
            .addOnSuccessListener {
                Log.d(tag, "Match deleted from DB.")
                onDeleteMatchSuccess()
            }
            .addOnFailureListener {e ->
                Log.d(tag, "Error deleting match from DB. $e")
                onDeleteMatchFailure()
            }
    }

    abstract fun onDeleteMatchSuccess()
    abstract fun onDeleteMatchFailure()
}
