package com.iwsinc.gmi.iwasdksampleapp

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
import com.iwsinc.iwa.iwa_sdk.api.AccountServiceManager
import com.iwsinc.iwa.iwa_sdk.api.MessagesServiceManager
import com.iwsinc.iwa.iwa_sdk.api.RegistrationStatus
import com.iwsinc.iwa.iwa_sdk.api.listeners.InteractionManagerListener
import com.iwsinc.iwa.iwa_sdk.api.model.Configuration
import com.iwsinc.iwa.iwa_sdk.api.model.IwaError
import com.iwsinc.iwa.iwa_sdk.api.model.IwaResult
import com.iwsinc.iwa.iwa_sdk.api.model.Profile
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*


//If the below constants are filled out with valid information, it will appear when the app launches:
const val IWA_SERVER_URL = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val IWA_USER_MANAGER_URL = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val IWA_CLIENT_ID = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val IWA_CLIENT_SECRET = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val IWA_APPLICATION_CODE = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val EMAIL_ADDRESS_USER_ID = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"

class MainActivity : AppCompatActivity(), InteractionManagerListener {

    private lateinit var accountServiceManager: AccountServiceManager
    private lateinit var messagesServiceManager: MessagesServiceManager

    private lateinit var profile: Profile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //----------------------------
        //Populate the text fields with the GMI const vals above if they have been changed:
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != IWA_SERVER_URL) edit_text_iwa_server_url.setText(IWA_SERVER_URL)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != IWA_USER_MANAGER_URL) edit_text_iwa_user_manager_url.setText(IWA_USER_MANAGER_URL)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != IWA_CLIENT_ID) edit_text_iwa_client_id.setText(IWA_CLIENT_ID)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != IWA_CLIENT_SECRET) edit_text_iwa_client_secret.setText(IWA_CLIENT_SECRET)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != IWA_APPLICATION_CODE) edit_text_iwa_application_code.setText(IWA_APPLICATION_CODE)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != EMAIL_ADDRESS_USER_ID) edit_text_email.setText(EMAIL_ADDRESS_USER_ID)

        //----------------------------------------------------------------------------------------------------------
        //Set up button onClick event handlers


        //----------------------------
        //BUTTON: INITIALIZE IWA SDK
        button_init_sdk.setOnClickListener {
            Log.d("INIT_SDK", "button_init_iwa_sdk clicked, Initializing SDK...")
            accountServiceManager = AccountServiceManager(this)
            messagesServiceManager = MessagesServiceManager(this)
            messagesServiceManager.register(this, this)
            showDialog(
                "IWA SDK Initialization completed",
                "INITIALIZE"
            )
        }

        //----------------------------
        //BUTTON: REGISTER AND SET EMAIL
        button_register_and_set_email.setOnClickListener {
            Log.d("REGISTER", "button_check_and_set_email clicked, checking provided email / user ID in background coroutine...")
            showBusySpinner(true)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    profile = Profile(
                        edit_text_email.extractText(),
                        Configuration(
                            "MyIwaServer",
                            edit_text_iwa_server_url.extractText(),
                            edit_text_iwa_user_manager_url.extractText(),
                            edit_text_iwa_client_id.extractText(),
                            edit_text_iwa_client_secret.extractText(),
                            edit_text_iwa_application_code.extractText()
                        )
                    )
                    accountServiceManager.register(profile, edit_text_iwa_application_code.extractText(), BuildConfig.VERSION_NAME).collect { handleRegistrationIwaResult(it) }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }


        //----------------------------
        //BUTTON: VALIDATE PIN
        button_validate_pin.setOnClickListener {
            Log.d("VALIDATE_PIN", "button_validate_pin clicked, attempting to validate provided PIN in background coroutine...")
            showBusySpinner()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    accountServiceManager.validate(profile, edit_text_email.extractText()).collect(::handleValidationIwaResult)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        //----------------------------
        //BUTTON: UPDATE PUSH TOKEN
        button_update_push.setOnClickListener {
            Log.d("UPDATE_PUSH", "button_update_push clicked, attempting update push token on server...")
            showBusySpinner()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    accountServiceManager.updatePush(profile, edit_text_push_token.extractText(), BuildConfig.VERSION_NAME).collect(::handleUpdatePushIwaResult)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }


        //----------------------------
        //BUTTON: COUNT PENDING ENROLLMENTS AND ALERTS
        button_count_pending_enrolls.setOnClickListener {
            Log.d("COUNT_ENROLL", "button_count_pending_enrolls clicked, counting pending enrolls for current user in background coroutine...")
            showBusySpinner()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    showDialog(
                        "Profile ${profile.email} on ${profile.configuration?.gmiServerUrl} currently has ${messagesServiceManager.getActiveEnrollmentsCountCo()} pending enrolls and ${messagesServiceManager.getActiveAlertsCountCo()} unread alerts.",
                        "COUNT_ENROLL"
                    )
                } catch (e: Exception) {
                    showDialog(
                        "Must perform a previous step first!  Count of pending enrollments for current user failed, exception was ${e.localizedMessage}",
                        "COUNT_ENROLL",
                        e
                    )
                }
                showBusySpinner(false)
            }
        }





        //----------------------------
        //BUTTON: COUNT PENDING ALERTS
        button_count_pending_alerts.setOnClickListener {
            Log.d("COUNT_ALERTS", "button_count_pending_alerts clicked, counting pending alerts for current user in background coroutine...")
            showBusySpinner()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    showDialog(
                        "Profile ${profile.email} on ${profile.configuration?.gmiServerUrl} currently has ${messagesServiceManager.getActiveAlertsCountCo()} unread alerts.",
                        "COUNT_ALERTS"
                    )
                } catch (e: Exception) {
                    showDialog(
                        "Must perform a previous step first!  Count of pending enrollments for current user failed, exception was ${e.localizedMessage}",
                        "COUNT_ALERTS",
                        e
                    )
                }
                showBusySpinner(false)
            }
        }





        //----------------------------
        //BUTTON: button_perform_sync_and_render_next_work_item
        button_sync.setOnClickListener {
            Log.d("SYNC", "button_sync, synchronizing with server (downloading messages)...")
            showBusySpinner()
            lifecycleScope.launch(Dispatchers.IO) {
                try {

                    if (accountServiceManager.profiles.isEmpty()) {
                        showDialog(
                            "Synchronization failed, no profiles detected!  Please re-register.",
                            "SYNC"
                        )
                    } else {
                        messagesServiceManager.synchronizeWorkItems()
                        showDialog(
                            "Synchronization completed.",
                            "SYNC"
                        )
                    }
                } catch (e: Exception) {
                    showDialog(
                        "Must perform a previous step first!  Sync failed, exception was ${e.localizedMessage}",
                        "SYNC", e
                    )
                }
                showBusySpinner(false)
            }
        }


        //----------------------------
        //BUTTON: button_render_next_work_item
        button_render_next_work_item.setOnClickListener {
            Log.d("RENDER", "button_render_next_work_item, Rendering next work item if not hidden / not skipped...")
            showBusySpinner()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    messagesServiceManager.renderNextWorkItemIfNeeded()
                } catch (e: Exception) {
                    showDialog(
                        "Must perform a previous step first!  Render failed, exception was ${e.localizedMessage}",
                        "RENDER", e
                    )
                }
                showBusySpinner(false)
            }
        }


        //----------------------------
        //BUTTON: button_set_work_items_unread
        button_set_work_items_unread.setOnClickListener {
            Log.d("SET_UNREAD", "button_set_work_items_unread clicked, setting work items unread...")
            showBusySpinner()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    messagesServiceManager.setWorkItemsUnread()
                    showDialog(
                        "Work items (alerts) set as UNREAD (unskipped).",
                        "SET_UNREAD"
                    )
                } catch (e: Exception) {
                    showDialog(
                        "Must perform a previous step first!  Work items not set as UNREAD, exception was ${e.localizedMessage}",
                        "SET_UNREAD", e
                    )
                }
                showBusySpinner(false)
            }
        }


        //----------------------------
        //BUTTON: button_unhide_all_enrolls
        button_unhide_all_enrolls.setOnClickListener {
            Log.d("UNHIDE", "button_unhide_all_enrolls clicked, unhiding all enrolls...")
            showBusySpinner()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    messagesServiceManager.setWorkItemsUnread()
                    showDialog(
                        "All enrollments unhidden and unskipped.",
                        "UNHIDE"
                    )
                } catch (e: Exception) {
                    showDialog(
                        "Must perform a previous step first!  Enrollments not unhidden, exception was ${e.localizedMessage}",
                        "UNHIDE", e
                    )
                }
                showBusySpinner(false)
            }
        }

    }




    //------------------------------------------------------------------------------------
    //Utility functions to keep the above code clean and readable

/*
    private fun generateImsPerson(): IMSPerson {
        val imsPerson = IMSPerson(validationResponse!!.personUuid, IMS.getServer())
        imsPerson.userId = edit_text_email.extractText()
        return imsPerson
    }

    private suspend fun launchAndWaitForNativeMessage(context: Context, imsMessageInfo: IMSMessageInfo): IMSResponse? {
        Log.i("LAUNCH", ".launchAndWaitForNativeMessage() launching IMS.renderMessage...")

        val imsMessage = imsMessageInfo.pullMessage(generateImsPerson())

        val listener = BlockingNativeMessageResponseListener()

        listener.requestAndWaitForPermit()
        IMS.renderMessage(imsMessage, context, listener)

        //Wait here until response, then release resulting sermaphore permit
        listener.requestAndWaitForPermit()
        Log.i("LAUNCH",".launchAndWaitForNativeMessage() execution returned from IMS.renderMessage with response: ${listener.lastResponse}")

        listener.releasePermit()
        Log.i("LAUNCH",".launchAndWaitForNativeMessage() execution returned from IMS.renderMessage, released permit.")
        return listener.lastResponse
    }
*/

    private fun handleUpdatePushIwaResult(it: IwaResult<Unit>) {
        showBusySpinner(false)
        return when (it) {
            is IwaResult.Success -> showDialog(
                "Push updated successfully!",
                "UPDATE_PUSH"
            )
            is IwaResult.Error -> showDialog(
                "Push not updated due to an error.",
                "UPDATE_PUSH"
            )
        }
    }

    private fun handleValidationIwaResult(it: IwaResult<Profile>) {
        when (it) {
            is IwaResult.Success -> {
                val profile = it.data
                showDialog(
                    "PIN Validated for ${profile.email}, tenants are now ${profile.tenants}, person UUID is ${profile.id}",
                    "VALIDATE"
                )
                this.profile = profile
            }
            is IwaResult.Error -> {
                when (it.error) {
                    IwaError.PIN_NOT_VALIDATED -> {
                        showDialog(
                            "PIN not validated",
                            "VALIDATE"
                        )
                    }
                    else -> {
                        showDialog(
                            "Server error",
                            "VALIDATE"
                        )
                    }
                }
            }
        }
        showBusySpinner(false)
    }

    private fun handleRegistrationIwaResult(it: IwaResult<RegistrationStatus>) {
        when (it) {
            is IwaResult.Success -> {
                when (it.data) {
                    RegistrationStatus.PENDING_VERIFICATION -> {
                        showDialog(
                            "Provided user ID / email address is valid, exists on the current GMI server, registration request received; check email for validation codes.",
                            "REGISTER"
                        )
                    }
                    RegistrationStatus.USER_VERIFIED -> {
                        showDialog(
                            "Provided user ID / email address is valid, exists on the current GMI server, registration request invalid; already validated all codes.",
                            "REGISTER"
                        )
                    }
                }
            }
            is IwaResult.Error -> {
                when (it.error) {
                    IwaError.USER_NOT_FOUND -> {
                        showDialog(
                            "Provided user ID / email address not found on provided server.",
                            "REGISTER"
                        )
                    }
                    else -> {
                        showDialog(
                            "Server error.",
                            "REGISTER"
                        )
                    }
                }
            }
        }
        showBusySpinner(false)
    }


    private fun showBusySpinner(showSpinner: Boolean = true) {
        lifecycleScope.launch(Dispatchers.Main) {
            touch_stealing_busy_overlay.visibility = if (showSpinner) View.VISIBLE else View.GONE
        }
    }

    private fun showDialog(
        message: String?,
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

            dialog?.setButton(Dialog.BUTTON_POSITIVE, "OK", DialogInterface.OnClickListener { gmiDialog, _ -> gmiDialog.dismiss() })
            dialog?.show()
        }
    }

    //------------------------------------------------------------------------------------
    //InteractionManagerListener overrides

    override fun onAlertAccepted() {
        showDialog(
            "Alert accepted",
            "RESULT"
        )
    }

    override fun onAlertCompleted() {
        showDialog(
            "Alert completed",
            "RESULT"
        )
    }

    override fun onAlertRejected() {
        showDialog(
            "Alert rejected",
            "RESULT"
        )
    }

    override fun onEnrollmentCompleted() {
        showDialog(
            "Enroll completed",
            "RESULT"
        )
    }

    override fun onEnrollmentHidden() {
        showDialog(
            "Enroll hidden",
            "RESULT"
        )
    }


    //------------------------------------------------------------------------
    //Kotin extensions functions

    private fun TextInputEditText.extractText() = text.toString()

    private fun Any.objectToString(): String? = GsonBuilder().create().toJson(this)
}
