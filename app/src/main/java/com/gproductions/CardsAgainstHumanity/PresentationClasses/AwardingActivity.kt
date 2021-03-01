package com.gproductions.CardsAgainstHumanity.PresentationClasses

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.gproductions.CardsAgainstHumanity.LogicClasses.AwardingDbCommunicator
import com.gproductions.CardsAgainstHumanity.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This activity class used to give prizes to players and end the game,
 */
class AwardingActivity : AppCompatActivity() {

    // Bundle
    private var bundle: Bundle? = null

    // Constants
    private val waitTime: Long = 3000L

    // Variables
    private var user: User? = null
    private var match: Match? = null
    private var opsDone = 0

    // Db communicator
    private var comm: AwardingDbCommunicator? = null

    /**
     * This function is called after class initialization,
     * - Set activity view,
     * - Define class used to communicate with db,
     * - Initialize bundle,
     * - Get user, if not present go to ChooseNickname activity,
     * - Get match, if not present go to ChooseNickname activity,
     * - Set button listeners,
     * - If game is finished show winner(s), update winner(s) user points,
     * -    If user is dealer add a listener to wait until winner(s) have finished,
     * -    Otherwise user remove himself from match players,
     * - Otherwise show show if player win this set,
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_awarding)
        Log.d(resources.getString(R.string.DEBUG_AWARDING), "Activity created.")

        // Initialize communicator
        comm = AwardingDbCommunicator(this)

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
            Log.d(resources.getString(R.string.DEBUG_AWARDING), "There is no user in bundle.")

            // Show error to user
            showError(getString(R.string.user_not_logged))

            // Going to match activity
            goNicknameActivity()
        }

        // Checking for match in bundle
        if (user!!.matchName == "nil") {
            // If no match in bundle
            Log.d(resources.getString(R.string.DEBUG_AWARDING), "There is no match in bundle.")
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
        Log.d(resources.getString(R.string.DEBUG_GAME), "Saving state.")
        outState.putSerializable("b_user", user)
        outState.putSerializable("b_match", match)
    }

    /**
     * This function is called after getting match in db,
     * @param dbMatch updated db match
     */
    fun updateLocalMatch(dbMatch: Match) {
        // Update local match
        match = dbMatch

        if (bundle?.getBoolean("end") != null && bundle?.getBoolean("end") as Boolean) {
            // Get if user wins
            val points = amIWinner()

            // Update matches
            user!!.matchPlayed += 1
            // Update points
            user!!.points += points
            // Clear match
            user!!.matchName = "nil"

            if (points > 0) {
                // Set win text
                findViewById<TextView>(R.id.tv_awarding).text =
                    resources.getString(R.string.you_win_the_game)
            } else {
                // Set lost text
                findViewById<TextView>(R.id.tv_awarding).text =
                    resources.getString(R.string.you_lost_the_game)
            }

            // Remove match from bundle
            bundle!!.remove("b_match")
            // Update points in DB
            comm!!.updateUserInDB(
                user!!.uid as String,
                hashMapOf(
                    "matchPlayed" to user!!.matchPlayed,
                    "points" to user!!.points,
                    "matchName" to user!!.matchName
                )
            )

            if (match!!.dealer == user!!.uid) {
                // Add listener to delete match in DB
                comm!!.addMatchListenerInDB(match!!.name as String)
            } else {
                // Remove player from match in DB
                comm!!.updateMatchInDB(
                    match!!.name as String,
                    hashMapOf("players" to FieldValue.arrayRemove(user!!.uid as String))
                )
            }
        } else {
            if (match!!.winner == user!!.uid) {
                // Set win text
                findViewById<TextView>(R.id.tv_awarding).text =
                    resources.getString(R.string.you_win)
            } else {
                // Set lost text
                findViewById<TextView>(R.id.tv_awarding).text =
                    resources.getString(R.string.you_lost)
            }
            // Wait
            waitBeforeReturn()
        }
    }

    /**
     * This function create a thread that wait a certain time before return to Game activity,
     */
    fun waitBeforeReturn() {
        GlobalScope.launch {
            // Wait some time before return
            delay(waitTime)

            // Define intent
            val intent = Intent()

            // Put data in bundle
            bundle!!.putSerializable("b_user", user)

            // Add bundle stored data
            intent.putExtras(bundle as Bundle)
            // Return to distributing
            setResult(RESULT_OK, intent)
            // End activity
            finish()
        }
    }

    /**
     * This function check if user is match game winner,
     * @return player match points
     */
    private fun amIWinner(): Double {
        return if (match!!.playersPoints[user!!.uid as String]!! >= match!!.playersPoints.values.maxOrNull()!!)
            1 / (match!!.playersPoints.values.count { it == match!!.playersPoints[user!!.uid as String]!! }).toDouble()
        else
            .0
    }

    /**
     * This function check if all two db operations are finished,
     */
    fun checkUpdatesEnd() {
        opsDone++
        if (opsDone == 2)
            waitBeforeReturn()
    }

    /**
     * This function show a popup error message to user,
     * @param str error string to show
     */
    fun showError(str: String) {
        Toast.makeText(
            this@AwardingActivity,
            str,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * This function is used to go to Nickname activity,
     */
    fun goNicknameActivity() {
        Log.d(resources.getString(R.string.DEBUG_AWARDING), "Starting Nickname activity.")
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
}