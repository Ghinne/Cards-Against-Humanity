package com.gproductions.card_against_humanity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import kotlin.collections.ArrayList

/**
 * This activity is used to shuffle cards sets in db,
 */
class ShufflingActivity : AppCompatActivity() {
    // Bundle
    private var bundle: Bundle? = null

    // Variables
    private var user: User? = null
    private var match: Match? = null
    private var whiteShuffled = false
    private var blackShuffled = false

    // Db communicator
    private var comm: ShufflingDbCommunicator? = null

    /**
     * This function is called after class initialization,
     * - Set activity view,
     * - Define class used to communicate with db,
     * - Initialize bundle,
     * - Get user, if not present go to ChooseNickname activity,
     * - Get match, if not present go to ChooseNickname activity,
     * - If user is dealer shuffle cards,
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        // Set loading motif
        findViewById<TextView>(R.id.tv_loading_motif).text =
            resources.getString(R.string.shuffling_cards)

        // Initialize communicator
        comm = ShufflingDbCommunicator(this)

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
            // If no user in bundle
            Log.d(resources.getString(R.string.DEBUG_SHUFFLING), "There is no user in bundle.")
            // Show error to user
            showError(getString(R.string.user_not_logged))
            // Going to match activity
            goNicknameActivity()
        }

        // Checking for match in bundle
        if (user!!.matchName == "nil") {
            // If no match in bundle
            Log.d(resources.getString(R.string.DEBUG_SHUFFLING), "There is no match in bundle.")
            // Show error to user
            showError(resources.getString(R.string.error_no_matches))
            // Going to match activity
            goNicknameActivity()
        }
    }

    /**
     * This function is called after activity is created,
     */
    override fun onStart() {
        super.onStart()
        // Get match in db
        comm!!.getMatchInDB(user!!.matchName)
    }

    /**
     * This function is used to save app state,
     * @param outState bundle with data to keep
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(resources.getString(R.string.DEBUG_SHUFFLING), "Saving state.")
        outState.putSerializable("b_user", user)
    }

    /**
     * This function is called after getting match in db,
     * @param dbMatch updated db match
     */
    fun updateLocalMatch(dbMatch: Match) {
        // Update local match
        match = dbMatch
        // If player is dealer
        if (match!!.dealer == user!!.uid)
        // Shuffle cards (DEALER)
            shuffleCards()
    }

    /**
     * This function is called by callback of function that get black shuffled cards from db,
     * @param cards shuffled black cards ids set
     * - Set shuffled black cards in match and set flag,
     * - If white shuffled too update match,
     */
    fun setBlackShuffled(cards: ArrayList<Int>) {
        /**
         * This function set black cards flag,
         */
        // Update white cards in match
        match!!.blackCards = cards

        // Set black shuffled
        blackShuffled = true
        // If white shuffled too
        if (whiteShuffled)
            comm!!.updateMatchInDB(
                match!!.name as String,
                hashMapOf(
                    "whiteCards" to match!!.whiteCards,
                    "blackCards" to match!!.blackCards
                ) as Map<String, Any>
            )
    }

    /**
     * This function is called by callback of function that get white shuffled cards from db,
     * @param cards shuffled white cards ids set
     * - Set shuffled white cards in match and set flag,
     * - If black shuffled too update match,
     */
    fun setWhiteShuffled(cards: ArrayList<Int>) {
        // Update white cards in match
        match!!.whiteCards = cards

        // Set white shuffled
        whiteShuffled = true
        // If black shuffled too
        if (blackShuffled)
        // Update match in db
            comm!!.updateMatchInDB(
                match!!.name as String,
                hashMapOf(
                    "whiteCards" to match!!.whiteCards,
                    "blackCards" to match!!.blackCards
                ) as Map<String, Any>
            )
    }

    /**
     * This function is used by actual dealer to shuffle cards when there is no more usable
     * cards in match sets or when match start,
     * - Check if card sets contain cards if empty shuffle cards,
     */
    private fun shuffleCards() {
        Log.d(
            resources.getString(R.string.DEBUG_SHUFFLING),
            "Shuffling cards."
        )

        // If white cards empty
        if (match!!.whiteCards.isEmpty()) {
            comm!!.getShuffledWhiteCards(match!!.language as String)
        } else
            setWhiteShuffled(match!!.whiteCards)

        // If black cards empty
        if (match!!.blackCards.isEmpty()) {
            comm!!.getShuffledBlackCards(match!!.language as String)
        } else
            setBlackShuffled(match!!.blackCards)
    }

    /**
     * This function show a popup error message to user,
     * @param str error string to show
     */
    fun showError(str: String) {
        Toast.makeText(
            this@ShufflingActivity,
            str,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * This function is used to go to Nickname activity,
     */
    fun goNicknameActivity() {
        Log.d(resources.getString(R.string.DEBUG_SHUFFLING), "Starting Nickname activity.")
        // Define intent
        val intent = Intent(this, ChooseNicknameActivity::class.java)
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
     * This function is used to go to the Distributing activity,
     */
    fun goDistributingActivity() {
        Log.d(resources.getString(R.string.DEBUG_SHUFFLING), "Back to distributing activity.")
        // Define intent
        val intent = Intent()

        // Put data in bundle
        bundle!!.putSerializable("b_user", user)

        // Add bundle stored data in next activity
        intent.putExtras(bundle as Bundle)
        // Return to distributing
        setResult(RESULT_OK, intent)
        // End activity
        finish()
    }
}