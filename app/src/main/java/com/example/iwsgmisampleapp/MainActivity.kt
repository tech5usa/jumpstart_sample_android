package com.example.iwsgmisampleapp

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.GsonBuilder
import com.iwsinc.ims.api.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

const val GMI_SERVER_TITLE = "My GMI Server"
const val GMI_USER_NAME= "Test GMI User"

//If the below constants are filled out with valid information, it will appear when the app launches:
const val GMI_SERVER_URL = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val GMI_USER_MANAGER_URL = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val GMI_CLIENT_ID = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val GMI_CLIENT_SECRET = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val GMI_TENANT_CODE = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val GMI_APPLICATION_CODE = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val EMAIL_ADDRESS_USER_ID = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"

class MainActivity : AppCompatActivity(), InteractionManagerListener {

    lateinit var accountServiceManager: AccountServiceManager
    lateinit var messagesServiceManager: MessagesServiceManager
    var profileDetailsManager: ProfileDetailsManager? = null
    var configuration: Configuration? = null
    var profile: Profile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //----------------------------------------------------------------------------------------------------------
        //SDK Initialization
        accountServiceManager = AccountServiceManager(this)
        messagesServiceManager = MessagesServiceManager()
        messagesServiceManager.register(this, this)


        //----------------------------------------------------------------------------------------------------------
        //Populate the text fields with the GMI const vals above if they have been changed:
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != GMI_SERVER_URL) edit_text_gmi_server_url.setText(GMI_SERVER_URL)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != GMI_USER_MANAGER_URL) edit_text_gmi_user_manager_url.setText(GMI_USER_MANAGER_URL)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != GMI_CLIENT_ID) edit_text_gmi_client_id.setText(GMI_CLIENT_ID)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != GMI_CLIENT_SECRET) edit_text_gmi_client_secret.setText(GMI_CLIENT_SECRET)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != GMI_TENANT_CODE) edit_text_gmi_tenant_code.setText(GMI_TENANT_CODE)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != GMI_APPLICATION_CODE) edit_text_gmi_application_code.setText(GMI_APPLICATION_CODE)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != EMAIL_ADDRESS_USER_ID) edit_text_email.setText(EMAIL_ADDRESS_USER_ID)


        //----------------------------------------------------------------------------------------------------------
        //Set up button onClick event handlers


        //----------------------------
        //BUTTON: REGISTER USER
        button_register_user.setOnClickListener {
            Log.d("REGISTER", "button_register_user clicked, Registering user with provided parameters in background coroutine...")
            showBusySpinner(true)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    configuration = Configuration(
                        GMI_SERVER_TITLE,
                        edit_text_gmi_server_url.extractText(),
                        edit_text_gmi_user_manager_url.extractText(),
                        edit_text_gmi_client_id.extractText(),
                        edit_text_gmi_client_secret.extractText(),
                        edit_text_gmi_tenant_code.extractText(),
                        edit_text_gmi_application_code.extractText()
                    )
                    profile = Profile(
                        edit_text_email.extractText(),
                        configuration!!,
                        null,
                        GMI_USER_NAME
                    )

                    lifecycleScope.launch(Dispatchers.IO) {
                        accountServiceManager.register(profile!!, true)
                            .collect {
                                when(it) {
                                    is Result.Success -> {
                                        when (it.data) {
                                            RegistrationStatus.pendingVerification -> {
                                                showSimpleDialog(title = "Check your email to confirm registration")
                                            }
                                            RegistrationStatus.userVerified -> {
                                                showSimpleDialog(title = "Account registration successfully completed")
                                            }
                                        }
                                    }
                                    is Result.Error -> {
                                        when (it.error) {
                                            GMIError.userNotFound -> {
                                                showSimpleDialog("User not found on this server")
                                            }
                                            else -> {
                                                showSimpleDialog(title = "Server error")
                                            }
                                        }
                                    }
                                }
                            }
                    }
                } catch (e: Exception) {
                    showSimpleDialog(
                        "Registration of GMI SDK failed, exception was ${e.localizedMessage}",
                        "REGISTER EXCEPTION",
                        e
                    )
                }
                showBusySpinner(false)
            }
        }


        //----------------------------
        //BUTTON: SYNC AND RUN FIRST ITEM
        button_sync_and_run_first_item.setOnClickListener {
            Log.d("SYNC_AND_RUN1", "button_sync_and_run_first_item clicked, synchronizing and running first expected item in background coroutine...")
            showBusySpinner(true)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    messagesServiceManager.renderNextWorkItemIfNeeded()
                } catch (e: Exception) {
                    showSimpleDialog(
                        "Must perform a previous step first, perhaps?  Sync/Run1st failed, exception was ${e.localizedMessage}",
                        "SYNC_AND_RUN1 EXCEPTION",
                        e
                    )
                }
                showBusySpinner(false)
            }
        }


        //----------------------------
        //BUTTON: COUNT PENDING ENROLLS AND ALERTS
        button_count_pending_enrolls_and_alerts.setOnClickListener {
            Log.d("COUNT_MSGS", "button_count_pending_enrolls_and_alerts clicked, counting pending enrolls and alerts in background coroutine...")
            showBusySpinner()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val enrollCount = messagesServiceManager.activeEnrollmentsCount
                    val alertCount = messagesServiceManager.activeAlertsCount
                    showSimpleDialog("There are $enrollCount pending enrolls and $alertCount alerts", "Message Count")
                 } catch (e: Exception) {
                    showSimpleDialog(
                        "Must perform a previous step first, perhaps?  Counting enrolls and alerts failed, exception was ${e.localizedMessage}",
                        "COUNT_MSGS EXCEPTION",
                        e
                    )
                }
                showBusySpinner(false)
            }
        }


        //----------------------------
        //BUTTON: SHOW PROFILE DETAILS
        button_show_profile_details.setOnClickListener {
            Log.d("PROFILED", "button_show_profile_details clicked, constructing profile details in background coroutine...")
            showBusySpinner()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    profileDetailsManager = ProfileDetailsManager(profile!!, messagesServiceManager)

                    val profileDetailStringBuilder = StringBuilder()
                    for (tenant in profileDetailsManager!!.sectionHeaders) {
                        with (profileDetailStringBuilder) {
                            append(tenant).append(":\n")
                            for (enrollment in profileDetailsManager!!.enrollsTenantMap[tenant]!!) {
                                append("-->").append(enrollment.captureType).append(":\n")
                                append("---->").append("Hidden: ").append(enrollment.hidden).append("\n")
                                append("---->").append("Completed: ").append(enrollment.completed).append("\n")
                                append("---->").append("AlgCode: ").append(enrollment.algCode).append("\n")
                            }
                            append("\n")
                        }
                        showSimpleDialog(profileDetailStringBuilder.toString(), "PROFILE DETAILS")
                    }

                } catch (e: Exception) {
                    showSimpleDialog(
                        "Must perform a previous step first, perhaps?  Profile details construction failed, exception was ${e.localizedMessage}",
                        "PROFILED EXCEPTION",
                        e
                    )
                }
                showBusySpinner(false)
            }
        }
    }


    //------------------------------------------------------------------------------------
    //InteractionManagerListener overrides

    override fun onAlertAccepted() {
        showSimpleDialog(title = "Alert accepted")
    }

    override fun onAlertCompleted() {
        showSimpleDialog(title = "Alert completed")
    }

    override fun onAlertRejected() {
        showSimpleDialog(title = "Alert rejected")
    }

    override fun onEnrollmentCompleted() {
        showSimpleDialog(title = "Enroll completed")
    }

    override fun onEnrollmentHidden() {
        showSimpleDialog(title = "Enroll hidden")
    }

    //------------------------------------------------------------------------------------
    //Utility functions to keep the above code clean and readable

    private fun showBusySpinner(showSpinner: Boolean = true) {
        lifecycleScope.launch(Dispatchers.Main) {
            touch_stealing_busy_overlay.visibility = if (showSpinner) View.VISIBLE else View.GONE
        }
    }

    private fun showSimpleDialog(
        message: String? = null,
        title: String? = null,
        exception: Exception? = null
    ) {
        //The following needs to be executed on the Main thread to affect the UI, so ensure this method properly handles being launched from an IO thread / coroutine
        lifecycleScope.launch(Dispatchers.Main) {
            // 1. Instantiate an <code><a href="/reference/android/app/AlertDialog.Builder.html">AlertDialog.Builder</a></code> with its constructor
            val builder: AlertDialog.Builder? = this@MainActivity.let {
                AlertDialog.Builder(it)
            }

            // 2. Chain together various setter methods to set the dialog characteristics
            builder?.setMessage(message)?.setTitle(title)

            // 3. Get the <code><a href="/reference/android/app/AlertDialog.html">AlertDialog</a></code> from <code><a href="/reference/android/app/AlertDialog.Builder.html#create()">create()</a></code>
            val dialog: AlertDialog? = builder?.create()

            if (exception == null) {
                Log.i("DIALOG", "showDialog($title, $message)")
            } else {
                Log.e("DIALOG_ERR", "showDialog($title, $message)", exception)
            }

            dialog?.setButton(Dialog.BUTTON_POSITIVE, "OK", DialogInterface.OnClickListener { dialog1, _ -> dialog1.dismiss() })
            dialog?.show()
        }
    }

    //------------------------------------------------------------------------
    //Kotin extension functions
    private fun TextInputEditText.extractText() = text.toString()

    private fun Any.objectToString(): String? = GsonBuilder().create().toJson(this)

}
