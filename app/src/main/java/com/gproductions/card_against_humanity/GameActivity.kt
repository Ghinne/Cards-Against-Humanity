package com.gproductions.card_against_humanity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import org.apache.commons.lang3.StringUtils

/**
 * This activity class is main game activity class,
 */
class GameActivity : AppCompatActivity(), View.OnClickListener {
    // Bundle
    private var bundle: Bundle? = null

    // Variables
    private var user: User? = null
    private var match: Match? = null
    private var blackGaps: Int? = null
    private val chosenCards: ArrayList<WhiteCard> = ArrayList()
    private val layoutsIndex: HashMap<String, View> = HashMap()
    private var bestChoice: String = ""
    private var bestChoiceLayout: View? = null

    // Db communicator
    private var comm: GameDbCommunicator? = null

    /**
     * This function is called after class initialization,
     * - Set activity view,
     * - Define class used to communicate with db,
     * - Initialize bundle,
     * - Get user, if not present go to ChooseNickname activity,
     * - Get match, if not present go to ChooseNickname activity,
     * - Set button listeners,
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        Log.d(resources.getString(R.string.DEBUG_GAME), "Activity created.")

        // Initialize communicator
        comm = GameDbCommunicator(this)

        // Get saved state
        bundle = savedInstanceState
        if (bundle == null) {
            // Get bundle
            bundle = intent.extras!!
        }

        // Getting user and match
        user = bundle?.getSerializable("b_user") as User?
        match = bundle?.getSerializable("b_match") as Match?

        // Checking for user in bundle
        if (user == null) {
            Log.d(resources.getString(R.string.DEBUG_GAME), "There is no user in bundle.")

            // Show error to user
            showError(getString(R.string.user_not_logged))

            // Going to match activity
            goNicknameActivity()
        }

        // Checking for match in bundle
        if (match == null) {
            // If no match in bundle
            Log.d(resources.getString(R.string.DEBUG_GAME), "There is no match in bundle.")
            // Show error to user
            showError(resources.getString(R.string.error_no_matches))
            // Going to match activity
            goNicknameActivity()
        }

        // Bind view to layout
        val btDone = findViewById<Button>(R.id.bt_done)
        val btExit = findViewById<Button>(R.id.bt_exit_game)
        // Set a listener for buttons
        btDone.setOnClickListener(this)
        btExit.setOnClickListener(this)
    }

    /**
     * This function is called each time this activity start,
     * - Check if match is finished,
     * - If distributing go to distributing activity,
     * - Otherwise clean chosen cards lists,
     * - Count gaps in black card and plot black card,
     * - Player status parameters,
     * - If player is dealer add a listener on players chosen cards,
     * - Otherwise plot white cards,
     */
    override fun onStart() {
        super.onStart()
        Log.d(resources.getString(R.string.DEBUG_GAME), "Activity started.")

        // If match is finished return
        if (bundle?.getBoolean("end") != null && bundle?.getBoolean("end") as Boolean)
            return

        // Check if game in distributing mode
        if (match!!.distributing.isNotEmpty())
            goDistributingActivity()
        else {
            // Clear lists
            chosenCards.clear()
            layoutsIndex.clear()

            // Clear local winner
            bestChoice = ""

            // Get black card gaps
            blackGaps = StringUtils.countMatches(
                match!!.actualBlackCard.Text,
                "__"
            )

            // Plot black card in layout
            plotBlackCard()

            // Plot player points, placement, round
            plotPointPlacementRound(
                match!!.playersPoints[user!!.uid as String] as Int,
                getPlayerPlacement(),
                match!!.round,
                match!!.rounds as Int
            )

            // If player is dealer
            if (match!!.dealer == user!!.uid) {
                // Add choice listener
                comm?.addMatchListenerInDB(match!!.name as String)
            } else {
                // Plot white cards in layout
                plotWhiteCards()
            }
        }
    }

    /**
     * This function is used by buttons callbacks,
     * @param v view calling this callback
     */
    override fun onClick(v: View?) {
        Log.d(resources.getString(R.string.DEBUG_GAME), "Button pressed.")
        if (v != null) {
            // Getting which button has been pressed
            when (v.id) {
                R.id.bt_done -> choiceDone()
                R.id.bt_exit_game -> goNicknameActivity()
            }
        }
    }

    /**
     * This function is used to save app state,
     * @param outState bundle with data to keep
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(resources.getString(R.string.DEBUG_GAME), "Saving state.")
        outState.putSerializable("b_user", user)
        outState.putSerializable("b_match", match)
    }

    /**
     * This function is used to get user placement,
     * @return player placement
     * - Sort players by their points,
     * - Get user position in sorting,
     */
    private fun getPlayerPlacement(): Int {
        Log.d(resources.getString(R.string.DEBUG_GAME), "Getting player placement.")
        // Getting player points
        val playersPoints = match!!.playersPoints
        // Getting player placement
        var placement = 1
        for ((player, points) in playersPoints.entries) {
            if (player != user!!.uid as String && points > playersPoints[user!!.uid as String] as Int)
                placement++
        }
        return placement
    }

    /**
     * This function is used when player want to confirm it's card choice,
     * - If user is dealer, if choice has been made set choice in match,
     * -  Set new dealer and winner player points and update in db,
     * -  If this round is last one, go to Awarding activity,
     * -  Otherwise go to Distributing activity,
     * - If user is player, if choices cover all black card gaps,
     * -  Remove chosen cards from player cards set, update match in db,
     * -  While waiting for other players choices, show other player choices instead of white cards,
     */
    private fun choiceDone() {
        // If dealer clicked done button
        if (match!!.dealer == user!!.uid) {
            Log.d(resources.getString(R.string.DEBUG_GAME), "Done button clicked by DEALER.")
            // If dealer has not selected a player choice
            if (bestChoice == "") {
                Log.d(resources.getString(R.string.DEBUG_GAME), "Choice not present.")
                // Show error to user
                showError(resources.getString(R.string.no_best_choice))
                return
            }
            // If not all players have choice cards
            if (match!!.playersChoices.size < match!!.players.size - 1) {
                Log.d(resources.getString(R.string.DEBUG_GAME), "Not all players voted.")
                // Show error to user
                showError(resources.getString(R.string.not_all_players_voted))
                return
            }

            // Setting winner
            match!!.winner = bestChoice

            // Setting winner points
            match!!.playersPoints[bestChoice] = match!!.playersPoints[bestChoice]!! + 1

            // Setting new dealer
            if (match!!.players.indexOf(match!!.dealer as String) + 1 >= match!!.players.size)
                match!!.dealer = match!!.players[0]
            else
                match!!.dealer = match!!.players[match!!.players.indexOf(match!!.dealer as String) + 1]

            // Add distributing
            match!!.distributing.add(user!!.uid as String)

            // Update match
            comm?.setMatchInDB(match!!)

            // If actual round is not the last one
            if (match!!.round < match!!.rounds as Int) {
                // Go distributing activity
                goDistributingActivity()
            } else {
                // Go awarding activity for final awarding
                goAwardingActivity()
            }

        } else {
            // Player clicked done button
            Log.d(resources.getString(R.string.DEBUG_GAME), "Done button clicked by PLAYER.")

            // If player has select enough cards to fill all gaps
            if (chosenCards.size != blackGaps) {
                Log.d(resources.getString(R.string.DEBUG_GAME), "Not enough cards.")
                // Show error to user
                showError(resources.getString(R.string.not_enough_cards))
            } else {
                // Remove used cards
                val updatedCards: ArrayList<WhiteCard> = match!!.playersCards[user!!.uid] as ArrayList<WhiteCard>
                updatedCards.removeAll(chosenCards)

                // Update choice in db
                comm?.updateMatchInDB(
                    match!!.name as String,
                    hashMapOf(
                        "playersChoices.${user!!.uid}" to chosenCards,
                        "playersCards.${user!!.uid}" to updatedCards
                    ) as Map<String, Any>
                )

                // Show other players choices
                showPlayersChoices()

                // Add a listener for awarding activity
                comm?.enableWinnerListener()
            }
        }
    }

    /**
     * This function is used to print points, placement and round in activity layout,
     * @param points user points
     * @param placement user placement
     * @param rounds actual match round
     */
    private fun plotPointPlacementRound(points: Int, placement: Int, round: Int, rounds: Int) {
        // Print points
        findViewById<TextView>(R.id.tv_points).text =
            (resources.getString(R.string.points) + points.toString())
        // Print placement
        findViewById<TextView>(R.id.tv_placement).text =
            (resources.getString(R.string.placement) + placement.toString())
        // Print round
        findViewById<TextView>(R.id.tv_round).text =
            (resources.getString(R.string.round) + round + "/" + rounds)
    }

    /**
     * This function is used to plot black card in layout,
     * - Inflate black card view with black card text and plot it,
     */
    @SuppressLint("InflateParams")
    private fun plotBlackCard() {
        Log.d(
            resources.getString(R.string.DEBUG_GAME),
            "Adding black card to game layout."
        )
        // Clear inflated layout
        findViewById<LinearLayout>(R.id.ll_black_card).removeAllViews()

        // Get the LayoutInflater from Context
        val li: LayoutInflater = LayoutInflater.from(this)
        this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)

        // create an inflated view
        val blackCardView =
            li.inflate(R.layout.black_card, findViewById<LinearLayout>(R.id.ll_black_card), false)

        // Set card text
        val bcText = blackCardView.findViewById<TextView>(R.id.tv_black_card)
        bcText.text = match!!.actualBlackCard.Text

        // Add view to scroll view
        findViewById<LinearLayout>(R.id.ll_black_card).addView(blackCardView)
    }

    /**
     * This function is used to plot white cards in layout,
     * - Clear white cards layout,
     * - For each white card in user set,
     * -  Inflate white card view with white card text, add a listener to this card,
     * -  Add card to white cards layout in white cards scroll view,
     */
    @SuppressLint("InflateParams")
    private fun plotWhiteCards() {
        /**
         * This function add white cards to activity layout,
         */
        Log.d(
            resources.getString(R.string.DEBUG_GAME),
            "Adding white cards to game layout."
        )
        // Get destination layout
        val dLayout = findViewById<LinearLayout>(R.id.ll_white_cards)

        // Clear it
        dLayout.removeAllViews()

        // Plot all white cards
        for (card in match!!.playersCards[user!!.uid]!!) {
            // Get the LayoutInflater from Context
            val li: LayoutInflater = LayoutInflater.from(this)
            this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)

            // Create an inflated view
            val whiteCardView =
                li.inflate(R.layout.white_card, dLayout, false)

            // Set card text
            val bcText = whiteCardView.findViewById<TextView>(R.id.tv_white_card)
            bcText.text = card.Text

            // Add layout to cards layout
            layoutsIndex[card.Text as String] = whiteCardView

            // Add listener for card selection
            whiteCardView.setOnClickListener {
                // Add card to chosen cards list
                addCardToChosenOnes(card)
            }

            // Add view to scroll view
            dLayout.addView(whiteCardView)
        }
    }

    /**
     * This function is called when a white card is clicked,
     * @param card white clicked card
     * - If card was already selected remove from selected ones,
     * - Add card to selected ones as last one,
     * - Show card background so player know that this card is selected,
     * - If selected cards count is greater than gaps remove first card in selected ones, hide it's background ad gap text,
     * - For each selected card add gap position on top,
     */
    private fun addCardToChosenOnes(card: WhiteCard) {
        Log.d(
            resources.getString(R.string.DEBUG_GAME),
            "Adding white card to chosen ones."
        )
        // Remove card if already present
        chosenCards.remove(card)
        // Add card to chosen ones
        chosenCards.add(card)

        // Set card background color
        layoutsIndex[card.Text]?.findViewById<ImageView>(R.id.iv_background)?.visibility =
            View.VISIBLE

        // If there are already enough cards to fill all gaps remove first
        if (chosenCards.size > blackGaps as Int) {
            // Remove selected background from cards
            layoutsIndex[chosenCards[0].Text as String]?.findViewById<ImageView>(R.id.iv_background)?.visibility =
                View.INVISIBLE
            layoutsIndex[chosenCards[0].Text as String]?.findViewById<TextView>(R.id.tv_gap_id)?.text =
                ""
            // Remove card from chosen cards
            chosenCards.removeAt(0)
        }
        // For each chosen cards set number of it's gap
        chosenCards.forEachIndexed { i, c ->
            // Set card gap
            layoutsIndex[c.Text]?.findViewById<TextView>(R.id.tv_gap_id)?.text = (i + 1).toString()
        }
    }

    /**
     * This function is used to plot players choices in layout,
     * - Clear white cards layout,
     * - For each player choice inflate a player choices layout view with inflated white cards view,
     * - If player is dealer add an on click listener that show inflated players choices background,
     *      so dealer know which choice is selected,
     * - Add inflated choice view to white cards layout in white cards scroll view,
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    fun plotPlayersChoices(dbMatch: Match) {
        /**
         * This function is used to plot players choices,
         */
        Log.d(
            resources.getString(R.string.DEBUG_GAME),
            "Plot player choices."
        )
        // Update match
        match = dbMatch

        // Get destination layout
        val dLayout = findViewById<LinearLayout>(R.id.ll_white_cards)

        // Clear it
        dLayout.removeAllViews()

        // Get the LayoutInflater from Context
        val li: LayoutInflater = LayoutInflater.from(this)
        this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)

        // For each choice
        for ((player, cards) in match!!.playersChoices.entries) {
            // Create layout to inflate
            val outLayout = li.inflate(R.layout.white_card_choices, dLayout, false)

            // Get sub-layout that contain cards
            val inLayout = outLayout.findViewById<LinearLayout>(R.id.ll_white_choices)

            // For each card in player choice
            cards.forEachIndexed { i, card ->
                // Create an inflated view
                val whiteCardView = li.inflate(R.layout.white_card, inLayout, false)

                // Set card text
                val bcText = whiteCardView.findViewById<TextView>(R.id.tv_white_card)
                bcText.text = card.Text

                // Add layout to cards layout
                layoutsIndex[card.Text as String] = whiteCardView

                // Set card gap
                whiteCardView.findViewById<TextView>(R.id.tv_gap_id).text = (i + 1).toString()

                // Add card to layout
                inLayout.addView(whiteCardView)
            }
            // If player is dealer
            if (match!!.dealer == user!!.uid) {
                // Enable Done button
                findViewById<Button>(R.id.bt_done).visibility = View.VISIBLE

                // Add listener for card selection
                inLayout.setOnClickListener {
                    Log.d(
                        resources.getString(R.string.DEBUG_GAME),
                        "Player choice selected."
                    )
                    // Update choice
                    bestChoice = player

                    // Set card background color
                    bestChoiceLayout?.findViewById<ImageView>(R.id.iv_white_choices_back)
                        ?.setImageResource(R.drawable.white_choices_not_selected)

                    // Update layout
                    bestChoiceLayout = outLayout

                    // Set card background color
                    outLayout.findViewById<ImageView>(R.id.iv_white_choices_back)
                        .setImageResource(R.drawable.white_choices_selected)
                }
            }

            // Add view to scroll view
            dLayout.addView(outLayout)
        }
    }

    /**
     * This function is used to show players choices to players that have already made a choice,
     */
    private fun showPlayersChoices() {
        // Hide done button
        findViewById<Button>(R.id.bt_done).visibility = View.INVISIBLE

        // Plot other players choices
        comm?.addMatchListenerInDB(match!!.name as String)
    }

    /**
     * This function is used to update match winner in activity,
     * @param dbMatch updated db match
     * - Update local match with db one,
     * - Go to Awarding activity,
     */
    fun winnerElected(dbMatch: Match) {
        // Updating winner
        match = dbMatch

        // Hide done button
        findViewById<Button>(R.id.bt_done).visibility = View.VISIBLE

        // Going to awarding activity
        goAwardingActivity()
    }

    /**
     * This function is used when match is deleted,
     */
    fun clearUserMatch() {
        match = null
        user!!.matchName = "nil"
        comm!!.updateUserInDB(user!!.uid as String, hashMapOf("matchName" to "nil"))
    }

    /**
     * This function show a popup error message to user,
     * @param str error string to show
     */
    fun showError(str: String) {
        Toast.makeText(
            this@GameActivity,
            str,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * This function is used to go to ChooseNickname activity,
     */
    fun goNicknameActivity() {
        Log.d(resources.getString(R.string.DEBUG_GAME), "Starting Nickname activity.")

        // Remove all listeners
        comm?.disableWinnerListener()
        comm?.removeMatchListener()

        // Define intent
        val intent = Intent(this, ChooseNicknameActivity::class.java)

        // Put data in bundle
        bundle?.putSerializable("b_user", user)
        bundle?.putSerializable("b_match", match)

        // Add bundle stored data in previous activity
        intent.putExtras(bundle as Bundle)
        // Start previous activity
        startActivity(intent)
        // End activity
        finish()
    }

    /**
     * This function is used to go to Distributing activity waiting for results,
     */
    private fun goDistributingActivity() {
        Log.d(resources.getString(R.string.DEBUG_GAME), "Starting Distributing activity.")

        // Remove all listeners
        comm?.disableWinnerListener()
        comm?.removeMatchListener()

        // Define intent
        val intent = Intent(this, DistributingActivity::class.java)

        // Put data in bundle
        bundle?.putSerializable("b_user", user)
        bundle?.putSerializable("b_match", match)

        // Add bundle stored data
        intent.putExtras(bundle as Bundle)
        // Start activity
        startActivityForResult(intent, resources.getInteger(R.integer.DISTRIBUTING_CODE))
    }

    /**
     * This function is used to go to Awarding activity waiting for results,
     */
    private fun goAwardingActivity() {
        Log.d(resources.getString(R.string.DEBUG_GAME), "Starting AwardingActivity activity.")

        // Update bundle match
        var code: Int = resources.getInteger(R.integer.AWARDING_CODE)

        // Remove all listeners
        comm?.disableWinnerListener()
        comm?.removeMatchListener()

        // Check if game is finished
        if (match!!.round >= match!!.rounds as Int) {
            bundle?.putBoolean("end", true)
            code = resources.getInteger(R.integer.END_CODE)
        } else {
            bundle?.putBoolean("end", false)
        }

        // Define intent
        val intent = Intent(this, AwardingActivity::class.java)

        // Put data in bundle
        bundle?.putSerializable("b_user", user)
        bundle?.putSerializable("b_match", match)

        // Add bundle stored data
        intent.putExtras(bundle as Bundle)
        // Start activity
        startActivityForResult(intent, code)
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
            resources.getString(R.string.DEBUG_GAME),
            "Activity results ready. (result code $resultCode)"
        )

        // Get which activity terminated with results
        when (requestCode) {
            resources.getInteger(R.integer.DISTRIBUTING_CODE) -> {
                Log.d(
                    resources.getString(R.string.DEBUG_GAME),
                    "Distributing terminated."
                )

                // Get shuffled card set
                if (data != null) {
                    // Update match
                    match = data.extras?.getSerializable("b_match") as Match
                    Log.d(
                        resources.getString(R.string.DEBUG_GAME),
                        "Updated match obtained."
                    )
                } else {
                    Log.d(
                        resources.getString(R.string.DEBUG_GAME),
                        "NO DATA from Distributing."
                    )
                }
            }
            resources.getInteger(R.integer.AWARDING_CODE) -> {
                Log.d(
                    resources.getString(R.string.DEBUG_GAME),
                    "AwardingActivity terminated."
                )

                // If player is dealer
                if (match!!.dealer == user!!.uid) {
                    // Enable dealer distributing locally
                    match!!.distributing.add(user!!.uid as String)
                    // Enable dealer distributing in db
                    comm?.updateMatchInDB(
                        match!!.name as String,
                        hashMapOf(
                            "distributing" to FieldValue.arrayUnion(user!!.uid)
                        )
                    )
                }
            }

            resources.getInteger(R.integer.END_CODE) -> {
                Log.d(
                    resources.getString(R.string.DEBUG_GAME),
                    "Ending terminated."
                )
                // Return to Nickname activity
                goNicknameActivity()
            }
        }
    }
}
