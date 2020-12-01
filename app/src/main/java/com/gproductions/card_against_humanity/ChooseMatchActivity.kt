package com.gproductions.card_against_humanity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import java.util.*

/**
 * This activity class is used by user to create a match, resume an active one or join a ready one,
 */
class ChooseMatchActivity : AppCompatActivity(), View.OnClickListener {
    // Bundle
    private var bundle: Bundle? = null

    // Variables
    private var user: User? = null
    private var match: Match? = null
    private var rounds: Int? = null
    private var language: String? = null

    // Db communicator
    private var comm: ChooseMatchDbCommunicator? = null

    /**
     * This function is called after class initialization,
     * - Set activity view,
     * - Define class used to communicate with db,
     * - Get user, if not present go to ChooseNickname activity,
     * - Initialize bundle,
     * - Set button listeners,
     * - Check for active match in db,
     * - Get user language and add a listener on ready matches,
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(resources.getString(R.string.DEBUG_MATCHES), "Activity created.")
        setContentView(R.layout.activity_matches)

        // Initialize communicator
        comm = ChooseMatchDbCommunicator(this)

        // Get saved state
        bundle = savedInstanceState
        if (bundle == null) {
            // Get bundle
            bundle = intent.extras!!
        }

        // Getting user and match
        user = bundle!!.getSerializable("b_user") as User?

        // If user null show an error
        if (user == null) {
            Log.d(resources.getString(R.string.DEBUG_MATCHES), "There is no user in bundle.")

            // Show error to user
            showError(getString(R.string.user_not_logged))

            // Going to nickname activity
            goNicknameActivity()
        }

        // Sync variables with graphical objects
        val btReturnToMatch: Button = findViewById(R.id.bt_return_to_match)
        val etMatchName: EditText = findViewById(R.id.et_match_name)
        val btMinus: Button = findViewById(R.id.bt_remove_round)
        val btPlus: Button = findViewById(R.id.bt_add_round)
        val btMatchCreate: Button = findViewById(R.id.bt_create_match)
        val btBack: Button = findViewById(R.id.bt_back)

        // Add buttons click listener
        btMinus.setOnClickListener(this)
        btPlus.setOnClickListener(this)
        btMatchCreate.setOnClickListener(this)
        btReturnToMatch.setOnClickListener(this)
        btBack.setOnClickListener(this)

        // Initialize rounds
        rounds = resources.getInteger(R.integer.MIN_ROUNDS_COUNT)
        printRounds()
        
        // Check for active matches
        comm!!.getMatchInDB(user!!.matchName, "check")

        // Check for match name field changing event
        addMatchNameListener(etMatchName)

        // Set language
        val languages: Array<out String> = resources.getStringArray(R.array.SUPPORTED_LANGUAGES)
        language = Locale.getDefault().displayLanguage.toString()

        if (!languages.contains(language))
            language = "english"

        // Add listener for ready matches
        comm!!.addReadyMatchesListenerInDB(user!!.uid as String, language as String)
    }

    /**
     * This function is used by buttons callbacks,
     * @param v view calling this callback
     */
    override fun onClick(v: View?) {
        Log.d(resources.getString(R.string.DEBUG_MATCHES), "Button clicked.")
        if (v != null) {
            // Check which button has been pressed
            when (v.id) {
                R.id.bt_return_to_match -> comm!!.getMatchInDB(user!!.matchName, "resume")
                R.id.bt_create_match -> createMatch()
                R.id.bt_add_round -> {
                    if (rounds!! < resources.getInteger(R.integer.MAX_ROUNDS_COUNT))
                        rounds = rounds!! + 1
                    printRounds()
                }
                R.id.bt_remove_round -> {
                    if (rounds!! > resources.getInteger(R.integer.MIN_ROUNDS_COUNT))
                        rounds = rounds!! - 1
                    printRounds()
                }
                R.id.bt_back -> goNicknameActivity()
            }
        }
    }

    /**
     * This function is used to save app state,
     * @param outState bundle with data to keep
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(resources.getString(R.string.DEBUG_MATCHES), "Saving state.")
        outState.putSerializable("b_user", user)
        outState.putSerializable("b_match", match)
    }

    /**
     * This function is used to print round count in it's view,
     */
    private fun printRounds() {
        // Updating view
        findViewById<TextView>(R.id.tv_rounds).text =
            (resources.getString(R.string.rounds) + rounds)
    }

    /**
     * This function add a listener on EditText passed as parameter,
     * @param etMatchName match name edit text
     * - On text change check if name is long enough and if not already used,
     */
    private fun addMatchNameListener(etMatchName: EditText) {
        // Listen match name edit text for changes
        etMatchName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                Log.d(resources.getString(R.string.DEBUG_MATCHES), "Match text changed.")
                // Getting match name
                val matchName = s.toString().trim { it <= ' ' }

                // Check username length
                if (matchName.length < resources.getInteger(R.integer.MIN_MATCH_NAME_CHARS)) {
                    Log.d(resources.getString(R.string.DEBUG_MATCHES), "Match text too short.")
                    disableCreate()
                } else {
                    // Check if username is long enough and if it's not already used
                    comm!!.checkIfNameIsUsedInDB(matchName)
                }
            }
        })
    }

    /**
     * This function is used to create a new match,
     * - Create a match object,
     * - If user is playing a match, remove him from match,
     */
    private fun createMatch() {
        Log.d(resources.getString(R.string.DEBUG_MATCHES), "Create match button pressed.")
        // Get views in layout
        val etMatchName: EditText = findViewById(R.id.et_match_name)
        val etMatchPasskey: EditText = findViewById(R.id.et_match_passkey)

        // Get new match name and passkey
        val mName = etMatchName.text.toString().trim { it <= ' ' }
        val mPasskey = etMatchPasskey.text.toString().trim { it <= ' ' }

        // Create new ActiveMatch
        val newMatch = Match(
            name = mName,
            language = language,
            passkey = mPasskey,
            active = false,
            dealer = user!!.uid as String,
            rounds = rounds
        )
        newMatch.players.add(user!!.uid as String)
        newMatch.distributing.add(user!!.uid as String)

        // Update match
        match = newMatch

        // Delete actual match if present
        Log.d(resources.getString(R.string.DEBUG_MATCHES), "OLD MATCH ${user!!.matchName}")
        if (user!!.matchName != "nil") {
            comm!!.removePlayerInDB(user as User, "create")
        } else {
            // Update or create match in db
            comm!!.setMatchInDB(match as Match)
        }
    }

    /**
     * This function is called by player remove callback,
     * @param by code to get calling function
     * - If caller function is create set new match in db,
     * - Il caller function is join update match in db adding user in players,
     */
    fun afterPlayerRemove(by: String) {
        when(by) {
            "create" -> {
                // Update or create match in db
                comm!!.setMatchInDB(match as Match)}
            "join" -> {
                // Add player to db
                comm!!.updateMatchInDB(
                    match!!.name as String,
                    hashMapOf("players" to FieldValue.arrayUnion(user!!.uid))
                )
            }
        }
    }

    /**
     * This function is called by update match in db function that add user in match players,
     * - Update user in db,
     */
    fun playerAddedInMatch() {
        // Update user in db
        comm!!.updateUserInDB(user!!.uid as String,
            hashMapOf(
                "matchName" to match!!.name as String
            )
        )
    }

    /**
     * This function is called by update match in db function that create match in db,
     * - Update user in db,
     */
    fun prepareDealer() {
        /**
         * This function prepare user to be match dealer once he create a match,
         */
        // Update user match
        user!!.matchName = match!!.name as String

        // Update user
        comm!!.updateUserInDB(
            user!!.uid as String,
            hashMapOf(
                "matchName" to match!!.name as String
            )
        )
    }

    /**
     * This function is used to update local user before with db one,
     * - Update user and go to WaitPlayers activity,
     */
    fun updateMatchToResume(dbMatch: Match) {
        // update match
        match = dbMatch
        // Go to waiting activity
        goWaitPlayersActivity()
    }

    /**
     * This function is used to add a ready match view to ready matches scroll view,
     * @param joinableMatch match to add in view
     * - Inflate a match view with match passed as parameter and add it to ready matches scroll view,
     */
    @SuppressLint("InflateParams")
    fun addReadyMatchView(joinableMatch: Match) {
        Log.d(
            resources.getString(R.string.DEBUG_MATCHES),
            "Adding match to ready matches view."
        )

        // Get the LayoutInflater from Context
        val li: LayoutInflater = LayoutInflater.from(this)
        this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)

        val matchView =
            li.inflate(R.layout.match, findViewById(R.id.ll_ready_matches), false)

        // Set match name text
        val mvName = matchView.findViewById<TextView>(R.id.m_match_name)
        mvName.text = joinableMatch.name

        // If password non present set password field invisible
        val mvPasskey = matchView.findViewById<EditText>(R.id.m_match_passkey)
        if (joinableMatch.passkey == null) mvPasskey.visibility = View.GONE

        // Set on click join match
        val mvJoin = matchView.findViewById<Button>(R.id.m_match_join)
        mvJoin.setOnClickListener {
            Log.d(
                resources.getString(R.string.DEBUG_MATCHES),
                "Join button of match ${joinableMatch.name} clicked."
            )

            // Read passkey from view
            val pk = mvPasskey.text.toString()
            if (pk == joinableMatch.passkey) {

                // Update user bundle
                user!!.matchName = joinableMatch.name as String

                // Add player to match players
                joinableMatch.players.add(user!!.uid as String)

                // Update match
                match = joinableMatch

                if (user!!.matchName != "nil") {
                    Log.d(
                        resources.getString(R.string.DEBUG_MATCHES),
                        "Removing player from old match."
                    )
                    // Clear user matches
                    comm!!.removePlayerInDB(user as User, "join")
                } else {
                    // Add player to db
                    comm!!.updateMatchInDB(
                        match!!.name as String,
                        hashMapOf("players" to FieldValue.arrayUnion(user!!.uid))
                    )
                }
            } else {
                Log.d(
                    resources.getString(R.string.DEBUG_MATCHES),
                    "Wrong passkey."
                )
                // Show error to user
                showError(getString(R.string.wrong_passkey))
            }
        }

        // Add view to scroll view
        findViewById<LinearLayout>(R.id.ll_ready_matches).addView(matchView)
    }

    /**
     * This function is used to enable 'return to match' button,
     */
    fun enableReturn() {
        Log.d(resources.getString(R.string.DEBUG_MATCHES), "Enabling return button.")
        (findViewById<Button>(R.id.bt_return_to_match)).visibility = View.VISIBLE
    }

    /**
     * This function is used to enable create button,
     */
    fun enableCreate() {
        Log.d(resources.getString(R.string.DEBUG_MATCHES), "Enabling create button.")
        (findViewById<Button>(R.id.bt_create_match)).isEnabled = true
    }

    /**
     * This function is used to disable create button,
     */
    fun disableCreate() {
        Log.d(resources.getString(R.string.DEBUG_MATCHES), "Disabling create button.")
        (findViewById<Button>(R.id.bt_create_match)).isEnabled = false
    }

    /**
     * This function is used empty ready matches linear layout in ready matches scroll view,
     */
    fun clearReadyMatches() {
        Log.d(resources.getString(R.string.DEBUG_MATCHES), "Ready matches cleared.")
        findViewById<LinearLayout>(R.id.ll_ready_matches).removeAllViews()
    }

    /**
     * This function show a popup error message to user,
     * @param str error string to show
     */
    fun showError(str: String) {
        Toast.makeText(
            this@ChooseMatchActivity,
            str,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * This function is used to go to ChooseNicknameActivity activity,
     */
    fun goNicknameActivity() {
        Log.d(
            resources.getString(R.string.DEBUG_MATCHES),
            "Starting Nickname activity."
        )
        comm!!.removeMatchListener()
        // Define intent
        val intent = Intent(this, ChooseNicknameActivity::class.java)
        // Put data in bundle
        bundle!!.putSerializable("b_user", user)
        bundle!!.putSerializable("b_match", match)

        // Add bundle stored data
        intent.putExtras(bundle as Bundle)
        // Start previous activity
        startActivity(intent)
        // End activity
        finish()
    }

    /**
     * This function is used to go to Wait players activity,
     */
    fun goWaitPlayersActivity() {
        Log.d(resources.getString(R.string.DEBUG_MATCHES), "Starting Waiting activity.")
        // Remove match listener
        comm!!.removeMatchListener()
        // Define intent
        val intent = Intent(this, WaitPlayers::class.java)

        // Put data in bundle
        bundle?.putSerializable("b_user", user)
        bundle?.putSerializable("b_match", match)

        // Add bundle stored data
        intent.putExtras(bundle as Bundle)

        // Start next activity
        startActivity(intent)

        // End activity
        finish()
    }
}
