package com.gproductions.CardsAgainstHumanity.LogicClasses

import android.content.res.Resources
import com.gproductions.CardsAgainstHumanity.PresentationClasses.AwardingActivity
import com.gproductions.CardsAgainstHumanity.PresentationClasses.Match
import com.gproductions.CardsAgainstHumanity.R
import com.gproductions.CardsAgainstHumanity.PresentationClasses.User

/**
 * This class inherit from DbCommunicator and it's used by activity to communicate with firebase db,
 * @param activity caller activity class,
 */
open class AwardingDbCommunicator(activity: AwardingActivity) : DbCommunicator() {
    // Variables
    private var activity: AwardingActivity? = null
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
     * This callback is called when user is updated in db,
     * - Check for updates
     */
    override fun onUpdateUserSuccess() {
        // Go back to activity
        activity!!.checkUpdatesEnd()
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and check for updates
     */
    override fun onUpdateUserFailure() {
        // Show error to user
        activity!!.showError(resources!!.getString(R.string.error_ending))
        // Go back to activity
        activity!!.checkUpdatesEnd()
    }

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
        // Go in Nickname activity
        activity!!.goNicknameActivity()
    }

    /**
     * This callback is called when match is updated successfully,
     * @param by code to get calling function
     */
    override fun onUpdateMatchSuccess(by: String) {
        // Go back to activity
        activity!!.checkUpdatesEnd()
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and check for updates,
     */
    override fun onUpdateMatchFailure() {
        // Show error to user
        activity!!.showError(resources!!.getString(R.string.error_ending))
        // Go back to activity
        activity!!.checkUpdatesEnd()
    }

    /**
     * This callback is called when match is changed in db
     * @param match updated match
     * @param by code to get calling function
     * - If only dealer in game remove listener and delete match,
     */
    override fun onMatchListenerEvent(match: Match, by: String) {
        // If only dealer in match
        if (match.players.size == 1) {
            // Remove listener
            removeMatchListener()
            // Remove match
            deleteMatchInDB(match.name as String)
        }
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go back,
     */
    override fun onMatchListenerFailure() {
        // Show error to user
        activity!!.showError(resources!!.getString(R.string.error_deleting))
        activity!!.waitBeforeReturn()
    }

    override fun onMatchDeleted() {}

    /**
     * This callback is called when match is deleted successfully,
     * - Go back,
     */
    override fun onDeleteMatchSuccess() {
        // If match deleted return
        activity!!.waitBeforeReturn()
    }

    /**
     * This callback is called when error occurred in db,
     * - Show error and go back,
     */
    override fun onDeleteMatchFailure() {
        // Show error to user
        activity!!.showError(resources!!.getString(R.string.error_deleting))
        activity!!.waitBeforeReturn()
    }
}