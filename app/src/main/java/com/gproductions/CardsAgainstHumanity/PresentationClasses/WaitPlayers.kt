package com.gproductions.CardsAgainstHumanity.PresentationClasses

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gproductions.CardsAgainstHumanity.LogicClasses.WaitPlayersDbCommunicator
import com.gproductions.CardsAgainstHumanity.R

/**
 * This activity class is used by players to wait other players before starting the match,
 */
class WaitPlayers : AppCompatActivity(), View.OnClickListener {
    // Bundle
    private var bundle: Bundle? = null

    // Variables
    private var user: User? = null
    private var match: Match? = null

    // Db communicator
    private var comm: WaitPlayersDbCommunicator? = null

    /**
     * This function is called after class initialization,
     * - Set activity view,
     * - Define class used to communicate with db,
     * - Initialize bundle,
     * - Get user, if not present go to ChooseNickname activity,
     * - Get match, if not present go to ChooseNickname activity,
     * - Set button listeners,
     * - Add a listener on match players,
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wait_players)
        Log.d(resources.getString(R.string.DEBUG_WAIT), "Activity created.")

        // Initialize communicator
        comm = WaitPlayersDbCommunicator(this)

        // Bind view to layout
        val btStartMatch: Button = findViewById(R.id.bt_start)
        val btCancelMatch: Button = findViewById(R.id.bt_cancel)
        btStartMatch.setOnClickListener(this)
        btCancelMatch.setOnClickListener(this)

        // Get saved state
        bundle = savedInstanceState
        if (bundle == null) {
            // Get bundle
            bundle = intent.extras!!
        }

        // Getting user and match
        user = bundle!!.getSerializable("b_user") as User?

        // Checking for user in bundle
        if (user == null) {
            Log.d(resources.getString(R.string.DEBUG_WAIT), "There is no user in bundle.")

            // Show error to user
            showError(getString(R.string.user_not_logged))

            // Going to nickname activity
            goNicknameActivity()
        } else
            Log.d(resources.getString(R.string.DEBUG_WAIT), "User find.")

        // Checking for match in bundle
        if (user!!.matchName == "nil") {
            // If no match in bundle
            Log.d(resources.getString(R.string.DEBUG_WAIT), "There is no match in bundle.")
            // Show error to user
            showError(resources.getString(R.string.error_no_matches))

            // Going to nickname activity
            goNicknameActivity()
        } else {
            // Get match
            comm!!.getMatchInDB(user!!.matchName)
        }
    }

    /**
     * This function is used by buttons callbacks,
     * @param v view calling this callback
     */
    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.bt_start -> startMatch()
                R.id.bt_cancel -> removePlayerInMatch()
            }
        }
    }

    /**
     * This function is used to save app state,
     * @param outState bundle with data to keep
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(resources.getString(R.string.DEBUG_WAIT), "Saving state.")
        outState.putSerializable("b_user", user)
    }

    /**
     * This function is called by listener once new players join or leave the match,
     * @param dbMatch updated db match
     * - Update local match with passed one,
     */
    fun resultMatchListener(dbMatch: Match) {
        // Update match
        match = dbMatch

        // Get players count
        val count: Int = match!!.players.size
        // Update players with players count
        updatePlayers(count)

        if (match!!.active!! && match!!.dealer != user!!.uid) {
            // If match is active, start it
            goGameActivity()
        }
    }

    /**
     * This function is used to update players count in text view,
     * @param count updated player count
     */
    private fun updatePlayers(count: Int) {
        Log.d(resources.getString(R.string.DEBUG_WAIT), "Updating players count on view.")
        // Preparing text
        val text: String = getText(R.string.players_count).toString() + count
        // Putting text in view
        findViewById<TextView>(R.id.tv_players_count).text = text
    }

    /**
     * This function is used to remove user from players in match,
     * - Remove user from match players in db,
     * - Return to ChooseMatch activity,
     */
    private fun removePlayerInMatch() {
        Log.d(resources.getString(R.string.DEBUG_WAIT), "Deleting player from match.")

        comm!!.removePlayerInDB(user as User, match as Match)

        // Clear user and go to matches activity
        resetUser()
    }

    /**
     * This function is called after getting match in db,
     * @param dbMatch updated db match
     */
    fun updateLocalMatch(dbMatch: Match) {
        Log.d(resources.getString(R.string.DEBUG_WAIT), "Match find.")
        match = dbMatch

        // If match is active go next activity
        if (match!!.active as Boolean)
        // Go to game activity
            goGameActivity()
        else
        // Add listener
            comm!!.addMatchListenerInDB(match!!.name as String)

        // Set button start visible if user is creator
        if (match!!.dealer == user!!.uid) {
            showStartButton()
        } else
            hideStartButton()
    }

    /**
     * This function is used to start match,
     * - Set match active flag as true,
     * - Update match in db,
     */
    private fun startMatch() {
        Log.d(resources.getString(R.string.DEBUG_WAIT), "Starting match.")

        // If enough players in match start it
        if (match!!.players.size < resources.getInteger(R.integer.MIN_PLAYERS)) {
            // otherwise show message to dealer
            showError(resources.getString(R.string.not_enough_players))
            return
        }
        // Set match active
        match!!.active = true

        // Initialize points
        for (player in match!!.players) {
            match!!.playersPoints[player] = 0
            match!!.playersCards[player] = ArrayList()
        }

        // Update match on db
        comm!!.updateMatchInDB(
            match!!.name as String,
            hashMapOf(
                "active" to true,
                "playersPoints" to match!!.playersPoints,
                "playersCards" to match!!.playersCards
            )
        )
    }

    /**
     * This function is used to show start button,
     */
    private fun showStartButton() {
        findViewById<Button>(R.id.bt_start).visibility = View.VISIBLE
    }

    /**
     * This function is used to hide start button,
     */
    private fun hideStartButton() {
        findViewById<Button>(R.id.bt_start).visibility = View.INVISIBLE
    }

    /**
     * This function show a popup error message to user,
     * @param str error string to show
     */
    fun showError(str: String) {
        Toast.makeText(
            this@WaitPlayers,
            str,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * This function is used to reset user before going to the previous activity,
     * - Reset user match and update user in db,
     */
    private fun resetUser() {
        // Delete match
        match = null

        // Update user bundle
        user!!.matchName = "nil"

        // Update user
        comm!!.updateUserInDB(
            user!!.uid as String,
            hashMapOf(
                "matchName" to "nil"
            )
        )
    }

    /**
     * This function is used to go to ChooseMatch activity,
     */
    fun goChooseMatchActivity() {
        Log.d(resources.getString(R.string.DEBUG_WAIT), "Starting ChooseMatchActivity activity.")
        // Define intent
        val intent = Intent(this, ChooseMatchActivity::class.java)

        // Remove players listener
        comm!!.removeMatchListener()

        // Put data in bundle
        bundle!!.putSerializable("b_user", user)

        // Add bundle stored data in previous activity
        intent.putExtras(bundle as Bundle)
        // Start previous activity
        startActivity(intent)
        // End activity
        finish()
    }

    /**
     * This function is used to go to ChooseNickname activity,
     */
    fun goNicknameActivity() {
        Log.d(
            resources.getString(R.string.DEBUG_WAIT),
            "Starting ChooseNickname activity."
        )
        // Define intent
        val intent = Intent(this, ChooseNicknameActivity::class.java)

        // Remove players listener
        comm!!.removeMatchListener()

        // Put data in bundle
        bundle!!.putSerializable("b_user", user)

        // Add bundle stored data
        intent.putExtras(bundle as Bundle)
        // Start previous activity
        startActivity(intent)
        // End activity
        finish()
    }

    /**
     * This function is used to go to Game activity,
     */
    fun goGameActivity() {
        Log.d(resources.getString(R.string.DEBUG_WAIT), "Starting GameActivity activity.")
        // Define intent
        val intent = Intent(this, GameActivity::class.java)

        // Remove players listener
        comm!!.removeMatchListener()

        // Put data in bundle
        bundle!!.putSerializable("b_user", user)

        // Add bundle stored data in next activity
        intent.putExtras(bundle as Bundle)
        // Start next activity
        startActivity(intent)
        // End activity
        finish()
    }
}
