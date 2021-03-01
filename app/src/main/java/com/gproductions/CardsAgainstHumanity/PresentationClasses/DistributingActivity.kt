package com.gproductions.CardsAgainstHumanity.PresentationClasses

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.FieldValue
import com.gproductions.CardsAgainstHumanity.LogicClasses.DistributingDbCommunicator
import com.gproductions.CardsAgainstHumanity.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * This activity class is the one used to distribute cards to players in match,
 */
class DistributingActivity : AppCompatActivity() {
    // Bundle
    private var bundle: Bundle? = null

    // Variables
    private var user: User? = null
    private var match: Match? = null

    // DEALER
    private var usersCardsNeeded: HashMap<String, Int> = HashMap()
    private var blackChosen = false

    // Db communicator
    private var comm: DistributingDbCommunicator? = null

    /**
     * This function is called after class initialization,
     * - Set activity view,
     * - Define class used to communicate with db,
     * - Initialize bundle,
     * - Get user, if not present go to ChooseNickname activity,
     * - Get match, if not present go to ChooseNickname activity,
     * - Set button listeners,
     * - If user is dealer initialize players needed cards add a listener to wait for players in distributing,
     * - Otherwise increment player in distributing count in match and add a listener to distributing end,
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)
        Log.d(resources.getString(R.string.DEBUG_DISTRIBUTING), "Activity created.")

        // Set loading motif
        findViewById<TextView>(R.id.tv_loading_motif).text =
            resources.getString(R.string.distributing_cards)

        // Initialize communicator
        comm = DistributingDbCommunicator(this)

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
            Log.d(resources.getString(R.string.DEBUG_DISTRIBUTING), "There is no user in bundle.")

            // Show error to user
            showError(getString(R.string.user_not_logged))

            // Going to match activity
            goNicknameActivity()
        }

        // Checking for match in bundle
        if (user!!.matchName == "nil") {
            // If no match in bundle
            Log.d(resources.getString(R.string.DEBUG_DISTRIBUTING), "There is no match in bundle.")
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
        Log.d(resources.getString(R.string.DEBUG_DISTRIBUTING), "Saving state.")
        outState.putSerializable("b_user", user)
    }

    /**
     * This function is called after getting match in db,
     * @param dbMatch updated db match
     */
    fun updateLocalMatch(dbMatch: Match) {
        Log.d(
            resources.getString(R.string.DEBUG_DISTRIBUTING),
            "Updating local match and start distributing."
        )
        // Update local match
        match = dbMatch

        // If actual player is dealer
        if (match!!.dealer == user!!.uid) {
            if (match!!.distributing.containsAll(match!!.players))
                distributeCards()
            else {
                // Player wait for distributing
                comm!!.updateMatchInDB(
                    match!!.name as String,
                    hashMapOf("distributing" to FieldValue.arrayUnion(user!!.uid)),
                    "distributing"
                )

                // Initializing hash map
                for (player in match!!.players)
                    usersCardsNeeded[player] =
                        resources.getInteger(R.integer.WHITE_CARDS_PER_PLAYER)

                // Add listener for distributing players
                comm!!.addMatchListenerInDB(match!!.name as String, "dealer")
            }
        } else {
            // Player wait for distributing
            comm!!.updateMatchInDB(
                match!!.name as String,
                hashMapOf("distributing" to FieldValue.arrayUnion(user!!.uid)),
                "distributing"
            )
            // Listen for distributing mode turn off
            comm!!.addMatchListenerInDB(match!!.name as String, "player")
        }
    }

    /**
     * This function is used to launch distribute operations,
     */
    fun launchDistribute() {
        // If actual player is dealer
        if (match!!.dealer == user!!.uid)
        // Give cards to players
            distributeCards()
    }

    /**
     * This function is used by dealer to distribute cards to players,
     * - If black card is not already chosen
     * -  If there are no more cards in black set, reshuffle the set,
     * -  Get card id from black card set, get card in db,
     * - For each player in match run a thread that check if player need cards,
     * -  If true if there are enough cards in white card set, reshuffle the set,
     * -  Get a white card id from white cards set, get card in db,
     */
    private fun distributeCards() {
        Log.d(resources.getString(R.string.DEBUG_DISTRIBUTING), "Distributing cards.")

        // If black card isn't chosen
        if (!blackChosen) {
            // Updating black card
            if (match!!.blackCards.count() < 1) {
                // If not enough cards in set, clear db cards and reshuffle the set
                comm!!.updateMatchInDB(
                    match!!.name as String,
                    hashMapOf("blackCards" to ArrayList<Int>()),
                    "cards"
                )
                return
            }
            // Set first card in set as new black card
            comm!!.getBlackCardInDB(match!!.blackCards[0], match!!.language as String)
            // Delete first card from set
            match!!.blackCards.removeAt(0)
        }

        // Updating white cards
        // For each player in match check it's white card count and add card until reach the
        // right count,
        Log.d(resources.getString(R.string.DEBUG_DISTRIBUTING), "Distributing white cards.")

        for (player in match!!.players) {
            GlobalScope.launch {
                Log.d(
                    resources.getString(R.string.DEBUG_DISTRIBUTING),
                    "Giving cards to $player. ${Thread.currentThread()}"
                )
                // Getting player cards
                if (match!!.playersCards[player] == null)
                    usersCardsNeeded[player] =
                        resources.getInteger(R.integer.WHITE_CARDS_PER_PLAYER)
                else
                    usersCardsNeeded[player] =
                        resources.getInteger(R.integer.WHITE_CARDS_PER_PLAYER) - match!!.playersCards[player]!!.size

                // If user has not enough cards give him necessary amount
                if (usersCardsNeeded[player]!! > 0) {
                    synchronized(this) {
                        if (match!!.whiteCards.size < usersCardsNeeded[player] as Int) {
                            // If not enough cards in set, clear db cards and reshuffle the set
                            comm!!.updateMatchInDB(
                                match!!.name as String,
                                hashMapOf("whiteCards" to ArrayList<Int>()),
                                "cards"
                            )
                            return@launch
                        }
                    }
                    // Getting cards to add
                    for (i in 1..(usersCardsNeeded[player] as Int)) {
                        var card: Int?
                        // Sync because all players are doing same thing in other threads
                        synchronized(this) {
                            // Get first card
                            card = match!!.whiteCards[0]
                            // Delete first card from set
                            match!!.whiteCards.removeAt(0)
                        }
                        // Add first card of the set to user cards
                        comm!!.getWhiteCardInDB(
                            player,
                            card!!,
                            match!!.language as String
                        )
                    }
                }
            }
        }
    }

    /**
     * This function is called by callback function that get black card in db,
     * @param card black card from db
     * - Update local black card,
     * - Check for end,
     */
    fun setBlackCard(card: BlackCard) {
        Log.d(
            resources.getString(R.string.DEBUG_DISTRIBUTING),
            "Setting black card in bundle. $card"
        )
        // Set card in match
        match!!.actualBlackCard = card

        // Set black card flag
        blackChosen = true
        checkForEnd()
    }

    /**
     * This function is called by callback function that get white card in db,
     * @param uid uid of player to serve
     * @param card black card from db
     * - Update local player with uid passed as parameter adding white card passed in his set,
     * - Decrease his cards needs,
     * - Check for end,
     */
    @Synchronized
    fun setPlayerCard(uid: String, card: WhiteCard) {
        Log.d(resources.getString(R.string.DEBUG_DISTRIBUTING), "Setting player $uid cards.")

        // Updating user cards
        match!!.playersCards[uid]!!.add(card)
        // Decrease amount of cards needed by user
        usersCardsNeeded[uid] = (usersCardsNeeded[uid] as Int) - 1

        // Check for distribution end
        checkForEnd()
    }

    /**
     * This function is used to check if all distributing task has been completed,
     * - If all completed update match in db disabling distributing,
     */
    @Synchronized
    private fun checkForEnd() {
        var nonZero = 0
        for (v in usersCardsNeeded.values)
            nonZero += v
        Log.d(
            resources.getString(R.string.DEBUG_DISTRIBUTING),
            "Checking for end $nonZero and $blackChosen."
        )
        if (nonZero == 0 && blackChosen) {
            // Reset winner
            match!!.winner = ""

            // Clear players choice list
            match!!.playersChoices.clear()

            // Increment round count
            match!!.round++

            // Disable and update bundle
            match!!.distributing.clear()

            // Update db
            comm!!.setMatchInDB(match!!)
        }
    }

    /**
     * This function show a popup error message to user,
     * @param str error string to show
     */
    fun showError(str: String) {
        Toast.makeText(
            this@DistributingActivity,
            str,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * This function is used to go to Nickname activity,
     */
    fun goNicknameActivity() {
        Log.d(resources.getString(R.string.DEBUG_DISTRIBUTING), "Starting Nickname activity.")
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
     * This function is used to go to Game activity,
     */
    fun goGameActivity() {
        Log.d(resources.getString(R.string.DEBUG_DISTRIBUTING), "Starting GameActivity activity.")

        // Define intent
        val intent = Intent()

        // Return to distributing
        setResult(RESULT_OK, intent)
        // End activity
        finish()
    }

    /**
     * This function is used to go to shuffling activity when cards ends waiting for results,
     */
    fun goShuffleActivity() {
        Log.d(resources.getString(R.string.DEBUG_DISTRIBUTING), "Starting Shuffling activity.")
        // Define intent
        val intent = Intent(this, ShufflingActivity::class.java)

        // Put data in bundle
        bundle!!.putSerializable("b_user", user)

        // Add bundle stored data in next activity
        intent.putExtras(bundle as Bundle)
        // Start next activity
        startActivityForResult(intent, resources.getInteger(R.integer.SHUFFLING_CODE))
    }

    /**
     * This function is used to get external login activities results,
     * @param requestCode of caller function
     * @param resultCode of called function
     * @param data returned data
     * - Based on result code do different operations,
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            resources.getString(R.string.DEBUG_DISTRIBUTING),
            "Activity results ready. (result code $resultCode)"
        )

        // Get which activity terminated with results
        when (requestCode) {
            resources.getInteger(R.integer.SHUFFLING_CODE) -> {
                Log.d(
                    resources.getString(R.string.DEBUG_DISTRIBUTING),
                    "Shuffling activity returned."
                )
            }
        }
    }

}
