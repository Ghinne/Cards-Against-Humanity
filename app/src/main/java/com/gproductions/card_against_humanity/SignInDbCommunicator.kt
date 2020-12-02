package com.gproductions.card_against_humanity

import android.content.res.Resources
import android.util.Log
import com.google.firebase.auth.GoogleAuthProvider

/**
 * This class inherit from DbCommunicator and it's used by activity to communicate with firebase db,
 * @param activity caller activity class,
 */
open class SignInDbCommunicator(activity: SignInActivity) : DbCommunicator() {
    // Debug TAG
    private val tag = "DEBUG_SIGN_IN_COMM"

    // Variables
    private var activity: SignInActivity? = null
    private var resources: Resources? = null

    init {
        this.activity = activity
        resources = activity.resources
    }

    /**
     * This function is used to authenticate a user with Email and Password,
     * @param email of user to login
     * @param password od user to login
     * - Call Firebase function to login with email and password,
     * - If sign-in fail because user isn't registered in db, try to register user in db,
     */
    fun authWithEmailInDB(email: String, password: String) {
        Log.d(tag, "Authenticate (email).")

        // Log in user
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                // Disable progress bar
                activity!!.hideProgressBar()
                Log.d(tag, "User logged in (email).")
                activity!!.goChooseNicknameActivity()
            }
            .addOnFailureListener { e ->
                // Disable progress bar
                activity!!.hideProgressBar()
                // Sign in failed
                Log.d(tag, "User not logged in (email). ${e.message}")
                if (e.message.equals("The password is invalid or the user does not have a password.")) {
                    // Show error to user
                    activity!!.showError(
                        resources?.getString(R.string.wrong_password).toString()
                    )
                } else {
                    // Create a new user
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            // Disable progress bar
                            activity!!.hideProgressBar()
                            // Sign in success
                            Log.d(tag, "User authenticated (email).")
                            activity!!.goChooseNicknameActivity()
                        }
                        .addOnFailureListener { e2 ->
                            // Disable progress bar
                            activity!!.hideProgressBar()
                            // User creation failed
                            Log.d(tag, "User NOT created (email). $e2")
                            if (e.equals("email-already-in-use")) {
                                // Show error to user
                                activity!!.showError(
                                    resources?.getString(R.string.used_email).toString()
                                )
                            } else {
                                // Show error to user
                                activity!!.showError(
                                    resources?.getString(R.string.error_authentication).toString()
                                )
                            }
                        }
                }
            }
    }

    /**
     * This function is used to authenticate a user with idToken,
     * @param idToken idtoken of google user
     * - Get credentials from Google,
     * - Call Firebase function to authenticate user in db with it's token,
     */
    fun authWithIdTokenInDB(idToken: String) {
        Log.d(tag, "Authenticate (idToken).")
        // Getting credentials from idToken
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        // Authenticate user on Firebase db
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                // Sign in success
                Log.d(tag, "User authenticated (idToken).")
                activity!!.goChooseNicknameActivity()
            }
            .addOnFailureListener { e ->
                // Disable progress bar
                activity!!.hideProgressBar()
                // Sign in failed
                Log.d(tag, "User NOT authenticated (idToken). $e")
                // Show error to user
                activity!!.showError(resources?.getString(R.string.error_authentication) + "Auth with token.")
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
    override fun onGetMatchSuccess(match: Match, by: String) {}
    override fun onGetMatchFailure(by: String) {}
    override fun onUpdateMatchSuccess(by: String) {}
    override fun onUpdateMatchFailure() {}
    override fun onMatchListenerEvent(match: Match, by: String) {}
    override fun onMatchListenerFailure() {}
    override fun onMatchDeleted() {}
    override fun onDeleteMatchSuccess() {}
    override fun onDeleteMatchFailure() {}
}