package com.gproductions.card_against_humanity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth

/**
 * This class is used for Users login and registration,
 * It's possible to use:
 * - Email/password,
 * - Google accounts,
 */
class SignInActivity : AppCompatActivity(), View.OnClickListener {
    // Variables
    private var auth: FirebaseAuth? = null
    private var signInClient: GoogleSignInClient? = null

    // Constants
    private val rcSignInGoogle = 1

    // Db communicator
    private var comm: SignInDbCommunicator? = null

    /**
     * This function is executed after activity class definition,
     * - Set activity view layout,
     * - Define class for the communications with db,
     * - Define Google sign in apis,
     * - Initialize buttons and their listeners,
     */

    /**
     * This function is called after class initialization,
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        Log.d(resources.getString(R.string.DEBUG_SIGN_IN), "Activity created.")

        // Initialize communicator
        comm = SignInDbCommunicator(this)
        // Get authentication
        auth = comm?.getAuthentication()

        // Define Google sign in options
        val options: GoogleSignInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        // Create a client to interact with google apis
        signInClient = GoogleSignIn.getClient(this, options)

        // Bind button view to layout
        val btGoogle: SignInButton = findViewById(R.id.bt_google_sign_in)
        val btEmail: Button = findViewById(R.id.bt_email_sign_in)
        btEmail.setOnClickListener(this)
        btGoogle.setOnClickListener(this)
    }

    /**
     * This function is used by buttons callbacks,
     * @param v view calling this callback
     */
    override fun onClick(v: View?) {
        Log.d(resources.getString(R.string.DEBUG_SIGN_IN), "Button pressed.")
        if (v != null) {
            when (v.id) {
                R.id.bt_email_sign_in -> signInEmail()
                R.id.bt_google_sign_in -> signInGoogle()
            }
        }
    }

    /**
     * This function is executed on activity start,
     * - Check if user is authenticated, if true go to Choose nickname activity,
     */
    override fun onStart() {
        super.onStart()
        Log.d(resources.getString(R.string.DEBUG_SIGN_IN), "Activity started.")
        // Getting user authentication
        val currentUser = auth?.currentUser
        // Check if user is authenticated.
        if (currentUser != null)
            goChooseNicknameActivity()
    }

    /**
     * This function is used to sign in with email and password,
     * - Get email and password from editable text fields and check for their feasibility,
     * - If feasible enable loading bar and call function to sign in in db,
     */
    private fun signInEmail() {
        Log.d(
            resources.getString(R.string.DEBUG_SIGN_IN),
            "Authentication started (email&password)."
        )
        // Get email and password from edit text views
        val email: String = (findViewById<EditText>(R.id.et_email)).text.toString()
        val password: String = (findViewById<EditText>(R.id.et_password)).text.toString()

        // Checking email validity
        if (!email.isEmailValid()) {
            Log.d(resources.getString(R.string.DEBUG_SIGN_IN), "Invaid email.")
            // Show error to user
            showError(resources.getString(R.string.invalid_email))
            return
        }
        // Checking password length
        if (password.length < resources.getInteger(R.integer.MIN_PASSWORD_CHARS)) {
            Log.d(resources.getString(R.string.DEBUG_SIGN_IN), "Password to short.")
            // Show error to user
            showError(resources.getString(R.string.short_password))
            return
        }
        // Enable progress bar
        showProgressBar()
        // Authenticate user on Firebase db
        comm?.authWithEmailInDB(email, password)
    }

    /**
     * This function check email feasibility,
     */
    private fun String.isEmailValid(): Boolean {
        return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this)
            .matches()
    }

    /**
     * This function is used to sign in with Google,
     * - It will start Google sign in activity in default google apis and wait for it's results,
     */
    private fun signInGoogle() {
        Log.d(resources.getString(R.string.DEBUG_SIGN_IN), "Authentication started (Google).")

        // Create a google sign in Intent
        val intent: Intent? = signInClient?.signInIntent
        // Run google sign in activity waiting for results
        startActivityForResult(intent, rcSignInGoogle)
    }

    /**
     * This function show a popup error message to user,
     * @param str error string to show
     */
    fun showError(str: String) {
        Toast.makeText(
            this@SignInActivity,
            str,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     *  This function enable loading bar,
     */
    private fun showProgressBar() {
        findViewById<ProgressBar>(R.id.pb_sign_in).visibility = View.VISIBLE
    }

    /**
     *  This function disable loading bar,
     */
    fun hideProgressBar() {
        findViewById<ProgressBar>(R.id.pb_sign_in).visibility = View.INVISIBLE
    }

    /**
     * This function start Choose nickname activity,
     */
    fun goChooseNicknameActivity() {
        Log.d(resources.getString(R.string.DEBUG_SIGN_IN), "Going to ChooseNicknameActivity.")
        // Start next activity
        startActivity(Intent(applicationContext, ChooseNicknameActivity::class.java))
        // End actual activity
        finish()
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
            resources.getString(R.string.DEBUG_SIGN_IN),
            "Activity results ready. (result code $resultCode)"
        )

        // Get which activity terminated with results
        when (requestCode) {
            rcSignInGoogle -> {
                if (resultCode == 0)
                // User returned from Google login
                    return
                // Enable progress bar
                showProgressBar()
                // Launch sign in task
                val task: Task<GoogleSignInAccount> =
                    GoogleSignIn.getSignedInAccountFromIntent(data)
                // Once authentication occurred
                task.addOnCompleteListener {
                    try {
                        Log.d(resources.getString(R.string.DEBUG_SIGN_IN), "User created (Google).")
                        // Google Sign In was successful
                        val account = task.result!!
                        // Authenticate on firebase DB
                        comm?.authWithIdTokenInDB(account.idToken!!)
                    } catch (e: ApiException) {
                        // Disable progress bar
                        hideProgressBar()
                        // Google Sign In failed
                        Log.d(
                            resources.getString(R.string.DEBUG_SIGN_IN),
                            "User NOT created (Google). $e"
                        )
                        // Show error to user
                        showError(resources.getString(R.string.error_authentication) + "Auth with Google.")
                    }
                }
            }
        }
    }
}
