package com.gproductions.card_against_humanity

import android.content.res.Resources

/**
 * This class inherit from DbCommunicator and it's used by activity to communicate with firebase db,
 * @param activity caller activity class,
 */
open class GameDbCommunicator(activity: GameActivity) : DbCommunicator() {
    // Variables
    private var activity: GameActivity? = null
    private var winnerListener: Boolean? = false
    private var resources: Resources? = null

    init {
        this.activity = activity
        resources = activity.resources
    }

    override fun onCheckUsedNicknameSuccess(used: Boolean) {}
    override fun onCheckUsedNicknameFailure() {}
    override fun onSetUserSuccess() {}
    override fun onSetUserFailure() {}
    override fun onGetUserSuccess(user: User) {}
    override fun onGetUserFailure() {}

    /**
     * This callback is called when match is deleted and user update his matchName,
     */
    override fun onUpdateUserSuccess() {
        // User match clear returning to Nickname activity
        activity!!.goNicknameActivity()
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onUpdateUserFailure() {
        // Show error to user
        activity!!.showError(resources!!.getString(R.string.error_update))
        // Go in Nickname activity
        activity!!.goNicknameActivity()
    }

    /**
     * This callback is called when match is set in db,
     */
    override fun onSetMatchSuccess() {
        activity!!.matchSet()
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onSetMatchFailure() {
        // Show error to user
        activity!!.showError(resources!!.getString(R.string.error_update))
        // Go in Nickname activity
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
        activity!!.showError(resources!!.getString(R.string.error_match_cancelled))
        // Go in Nickname activity
        activity!!.goNicknameActivity()
    }

    override fun onUpdateMatchSuccess(by: String) {}

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onUpdateMatchFailure() {
        // Show error to user
        activity!!.showError(resources!!.getString(R.string.error_update))
        // Go in Nickname activity
        activity!!.goNicknameActivity()
    }

    /**
     * This callback is called when match is changed in db
     * @param match updated match
     * @param by code to get calling function
     * - Update local match,
     */
    override fun onMatchListenerEvent(match: Match, by: String) {
        // Update choices
        activity!!.plotPlayersChoices(match)
        // Check winner
        if (winnerListener!! && match.winner != "") {
            disableWinnerListener()
            // Update elected winner
            activity!!.winnerElected(match)
        }
    }

    /**
     * This function is used to "enable" winner listener,
     */
    fun enableWinnerListener() {
        winnerListener = true
    }

    /**
     * This function is used to "disable" winner listener,
     */
    fun disableWinnerListener() {
        winnerListener = true
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go to Nickname activity
     */
    override fun onMatchListenerFailure() {
        // Show error to user
        activity!!.showError(resources!!.getString(R.string.error_getting_data))
        // Go in Nickname activity
        activity!!.goNicknameActivity()
    }

    override fun onMatchDeleted() {
        // Show error to user
        activity!!.showError(resources!!.getString(R.string.error_getting_data))
        //Update user
        activity!!.clearUserMatch()
    }

    override fun onDeleteMatchSuccess() {}
    override fun onDeleteMatchFailure() {}
}
