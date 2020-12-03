package com.gproductions.card_against_humanity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * This activity class is used by user to choose nickname and create user with that nickname,
 */
open class ChooseNicknameActivity : AppCompatActivity(), View.OnClickListener {
    // Bundle
    private var bundle: Bundle? = null

    // Variables
    private var auth: FirebaseAuth? = null
    private var user: User? = null
    private var usedNickname = true
    private var comm: ChooseNicknameDbCommunicator? = null

    /**
     * This function is called after class initialization,
     * - Set activity view,
     * - Define class used to communicate with db,
     * - Get user authentication, if not present go to SignInActivity activity,
     * - Initialize bundle,
     * - Set button listeners,
     * - Add listener for nickname,
     * - Get user data if user already in db,
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(resources.getString(R.string.DEBUG_NICKNAME), "Activity created.")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_nickname)

        // Initialize communicator
        comm = ChooseNicknameDbCommunicator(this)
        // Get authentication
        auth = comm!!.getAuthentication()

        // If user not authenticated or authentication error
        if (auth == null) {
            Log.d(resources.getString(R.string.DEBUG_NICKNAME), "User not authenticated or error.")
            showError(resources.getString(R.string.user_not_logged))
            goSignInActivity()
        }

        // Get saved instance state
        bundle = savedInstanceState
        if (bundle == null) {
            // If no instance state present
            bundle = Bundle()
        }

        // Getting user from bundle
        user = bundle!!.getSerializable("b_user") as User?

        // Sync variables with graphical objects
        val etNickname: TextView = findViewById(R.id.et_nickname)
        val btEnter: Button = findViewById(R.id.bt_enter)
        val btChangeUser: Button = findViewById(R.id.bt_change_user)

        // Add button click listener
        btEnter.setOnClickListener(this)
        btChangeUser.setOnClickListener(this)

        // Check for username field changing event
        addNicknameListener(etNickname)

        // Check for user data in db
        comm!!.getUserInDB(auth!!.uid as String)
    }

    /**
     * This function is used by buttons callbacks,
     * @param v view calling this callback
     */
    override fun onClick(v: View?) {
        Log.d(resources.getString(R.string.DEBUG_NICKNAME), "Button pressed.")
        // Check which button has been pressed
        if (v != null) {
            when (v.id) {
                R.id.bt_enter -> updateNickname()
                R.id.bt_change_user -> goSignInActivity()
            }
        }
    }

    /**
     * This function is used to save app state,
     * @param outState bundle with data to keep
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(resources.getString(R.string.DEBUG_NICKNAME), "Saving state.")
        outState.putSerializable("b_user", user)
    }

    /**
     * This function set nickname used/unused,
     * @param used true if nickname is already used by another user,
     */
    fun setNicknameUsed(used: Boolean) {
        Log.d(resources.getString(R.string.DEBUG_NICKNAME), "Set nickname usage $used.")
        // Set usage
        usedNickname = used
    }

    /**
     * This function add a event listener on username EditText view,
     * @param etU text view to listen
     * - Once nickname is changed check if feasible and available,
     */
    private fun addNicknameListener(etU: TextView) {
        etU.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                Log.d(
                    resources.getString(R.string.DEBUG_NICKNAME),
                    "ChooseNicknameActivity text changed."
                )
                // If username is changed check if it's feasible
                enableSubmitIfReady(etU.text.toString().trim { it <= ' ' })
            }
        })
    }

    /**
     * This function is used to check if nickname is longer than minimum required char and is not
     * already used enable login button.
     * @param nickname nickname to check
     */
    private fun enableSubmitIfReady(nickname: String) {
        Log.d(resources.getString(R.string.DEBUG_NICKNAME), "Checking login text field.")
        // Check username length
        if (nickname.length < resources.getInteger(R.integer.MIN_NICKNAME_CHARS))
            return

        // Check if other users use same nickname
        comm!!.checkUsedNicknameInDB(nickname)
    }

    /**
     * This function is used to update local user data with db one,
     * @param dbUser user data from db
     */
    fun updateUser(dbUser: User) {
        Log.w(
            resources.getString(R.string.DEBUG_NICKNAME),
            "Updating user in game."
        )
        // Get user and set him in bundle
        user = dbUser

        // Set username as default
        setNickname(dbUser.nickname as String)
        // Print points
        printPoints(dbUser.points)
    }

    /**
     * This function is used to print a string in nickname field,
     * @param nickname nickname to set in EditText field
     */
    private fun setNickname(nickname: String) {
        Log.d(resources.getString(R.string.DEBUG_NICKNAME), "Setting a default nickname.")
        findViewById<EditText>(R.id.et_nickname).setText(nickname)
    }

    /**
     * This function is used to print points in points field,
     * @param points points to print in TextView
     */
    private fun printPoints(points: Double) {
        Log.d(resources.getString(R.string.DEBUG_NICKNAME), "Printing points.")
        findViewById<TextView>(R.id.tv_show_points).text =
            (resources.getString(R.string.points) + points)
    }

    /**
     * This function is used to enable Progress Bar,
     */
    private fun showProgressBar() {
        Log.d(resources.getString(R.string.DEBUG_NICKNAME), "Enabling Progress bar.")
        findViewById<ProgressBar>(R.id.pb_nickname).visibility = View.VISIBLE
    }

    /**
     * This function is used to disable Progress Bar,
     */
    fun hideProgressBar() {
        Log.d(resources.getString(R.string.DEBUG_NICKNAME), "Disabling Progress bar.")
        findViewById<ProgressBar>(R.id.pb_nickname).visibility = View.INVISIBLE
    }

    /**
     * This function is called when 'enter the game' button is pressed,
     * - Check if nickname from EditText view is long enough and is not used,
     * - If user is already in db update it's nickname, otherwise create a new user in db,
     */
    private fun updateNickname() {
        Log.d(
            resources.getString(R.string.DEBUG_NICKNAME),
            "ChooseNicknameActivity button pressed."
        )
        // Get nickname
        val etNickname: TextView = findViewById(R.id.et_nickname)

        // Get username text
        val nickname = etNickname.text.toString().trim { it <= ' ' }

        // Check username length
        if (nickname.length < resources.getInteger(R.integer.MIN_NICKNAME_CHARS)) {
            // Show error
            showError(resources.getString(R.string.short_nickname))
            return
        }

        // Check if username is used
        if (usedNickname) {
            // Show error to user
            showError(resources.getString(R.string.error_used_nickname))
            return
        }

        if (user != null) {
            if ((user as User).nickname != nickname)
            // Change only the nickname
                user!!.nickname = nickname
        } else
        // If user is new create a new one
            user = User(nickname, auth!!.uid)

        // Enabling progress bar
        showProgressBar()

        // Update bundle
        bundle!!.putSerializable("b_user", user)

        // Update user in db
        comm!!.setUserInDB(user!!)
    }

    /**
     * This function show a popup error message to user,
     * @param str error string to show
     */
    fun showError(str: String) {
        Toast.makeText(
            this@ChooseNicknameActivity,
            str,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * This function is used to logout and go back to SignInActivity activity,
     */
    private fun goSignInActivity() {
        Log.d(resources.getString(R.string.DEBUG_NICKNAME), "Logging out.")
        // Logout
        auth!!.signOut()
        // Go to SignInActivity activity
        startActivity(Intent(applicationContext, SignInActivity::class.java))
        // End actual activity
        finish()
    }

    /**
     * This function is used to go to ChooseMatchActivity activity,
     */
    fun goChooseMatchesActivity() {
        Log.d(resources.getString(R.string.DEBUG_NICKNAME), "Going to ChooseMatchActivity.")
        // Create an intent
        val intent = Intent(applicationContext, ChooseMatchActivity::class.java)
        // Update bundle
        bundle!!.putSerializable("b_user", user)

        // Add bundle stored data
        intent.putExtras(bundle as Bundle)
        // Start next activity
        startActivity(intent)
        // End actual activity
        finish()
    }
}