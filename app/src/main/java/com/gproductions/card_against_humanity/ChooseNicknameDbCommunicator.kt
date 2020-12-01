package com.gproductions.card_against_humanity

import android.content.res.Resources

/**
 * This class inherit from DbCommunicator and it's used by activity to communicate with firebase db,
 * @param activity caller activity class,
 */
open class ChooseNicknameDbCommunicator(activity: ChooseNicknameActivity) : DbCommunicator() {
    // Variables
    private var activity: ChooseNicknameActivity? = null
    private var resources: Resources? = null

    init {
        this.activity = activity
        resources = activity.resources
    }

    /**
     * This callback set flag in activity,
     * @param used flag value to set in activity
     */
    override fun onCheckUsedNicknameSuccess(used: Boolean) {
        (activity as ChooseNicknameActivity).setNicknameUsed(used)
        if (used)
        // Show error to user
            (activity as ChooseNicknameActivity).showError(
                resources?.getString(R.string.error_used_nickname).toString()
            )

    }

    override fun onCheckUsedNicknameFailure() {}

    /**
     * This callback is called if user nickname is set in db,
     * - Hide progress bar and go to ChooseMatches activity,
     */
    override fun onSetUserSuccess() {
        // Disabling eventually enabled progress bar
        (activity as ChooseNicknameActivity).hideProgressBar()
        // Going to next activity
        (activity as ChooseNicknameActivity).goChooseMatchesActivity()
    }

    /**
     * This callback is called when an error occur while setting user nickname in db,
     * - Hide progress bar and show error,
     */
    override fun onSetUserFailure() {
        // Disabling eventually enabled progress bar
        (activity as ChooseNicknameActivity).hideProgressBar()
        // Show error to user
        (activity as ChooseNicknameActivity).showError(
            resources?.getString(R.string.error_updating_nickname).toString()
        )
    }

    /**
     * This callback is called when user is retrieved successfully,
     * - Update user in activity,
     */
    override fun onGetUserSuccess(user: User) {
        // Updating user in game
        (activity as ChooseNicknameActivity).updateUser(user)
    }

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