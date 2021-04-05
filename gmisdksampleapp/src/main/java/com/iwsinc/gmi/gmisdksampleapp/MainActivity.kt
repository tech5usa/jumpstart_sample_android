package com.iwsinc.gmi.gmisdksampleapp

import android.app.Dialog
import android.content.Context
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
import com.iwsinc.ims.response.IMSResponse
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*


//If the below constants are filled out with valid information, it will appear when the app launches:
const val GMI_SERVER_URL = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val GMI_USER_MANAGER_URL = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val GMI_CLIENT_ID = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val GMI_CLIENT_SECRET = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val GMI_APPLICATION_CODE = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
const val EMAIL_ADDRESS_USER_ID = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"

class MainActivity : AppCompatActivity() {

    private var validationResponse: ValidationResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //----------------------------
        //Populate the text fields with the GMI const vals above if they have been changed:
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != GMI_SERVER_URL) edit_text_gmi_server_url.setText(GMI_SERVER_URL)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != GMI_USER_MANAGER_URL) edit_text_gmi_user_manager_url.setText(GMI_USER_MANAGER_URL)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != GMI_CLIENT_ID) edit_text_gmi_client_id.setText(GMI_CLIENT_ID)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != GMI_CLIENT_SECRET) edit_text_gmi_client_secret.setText(GMI_CLIENT_SECRET)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != GMI_APPLICATION_CODE) edit_text_gmi_application_code.setText(GMI_APPLICATION_CODE)
        if ("PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION" != EMAIL_ADDRESS_USER_ID) edit_text_email.setText(EMAIL_ADDRESS_USER_ID)

        //----------------------------------------------------------------------------------------------------------
        //Set up button onClick event handlers


        //----------------------------
        //BUTTON: INITIALIZE GMI SDK
        button_init_gmi_sdk.setOnClickListener {
            Log.d("INIT_SDK", "button_init_gmi_sdk clicked, Initializing SDK with provided parameters in background coroutine...")
            showBusySpinner(true)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    IMS.startIMS(
                        this@MainActivity,
                        edit_text_gmi_server_url.extractText(),
                        "ImageWare",    //tenantcode during initialization is irrelevant, default is ImageWare
                        edit_text_gmi_application_code.extractText()
                    )
                    IMS.setUserManagerUrl(edit_text_gmi_user_manager_url.extractText())

                    //It is recommended to set a unique "Device ID" for each user, otherwise the actual Android device ID will be used:
                    IMS.getThisDevice().setDeviceId(UUID.randomUUID().toString())

                    IMS.acquireOAuthCredentials(
                        edit_text_gmi_client_id.extractText(),
                        edit_text_gmi_client_secret.extractText()
                    )
                    showGmiDialog(
                        "SDK successfully initialized with provided parameters!",
                        "INIT_SDK"
                    )
                } catch (e: Exception) {
                    showGmiDialog(
                        "Initialization of GMI SDK failed, exception was ${e.localizedMessage}",
                        "INIT_SDK",
                        e
                    )
                }
                showBusySpinner(false)
            }
        }

        //----------------------------
        //BUTTON: REGISTER AND SET EMAIL
        button_register_and_set_email.setOnClickListener {
            Log.d("REGISTER", "button_check_and_set_email clicked, checking provided email / user ID in background coroutine...")
            showBusySpinner(true)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    IMS.registerDeviceWithUserId(edit_text_email.extractText(), edit_text_gmi_application_code.extractText(), BuildConfig.VERSION_NAME)
                    showGmiDialog(
                            "Provided user ID / email address is valid, exists on the current GMI server, registration request received; check email for validation codes.",
                            "REGISTER"
                    )
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: IMSServerException) {
                    showGmiDialog(
                            "Provided user ID / email address is not available on the current GMI server, initialization not completed, or registration / validation already completed!",
                            "REGISTER"
                    )
                }
                showBusySpinner(false)
            }
        }

        //----------------------------
        //BUTTON: VALIDATE PIN
        button_validate_pin.setOnClickListener {
            Log.d("VALIDATE_PIN", "button_validate_pin clicked, attempting to validate provided PIN in background coroutine...")
            showBusySpinner()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    validationResponse = IMS.validatePin(edit_text_email.extractText(), edit_text_pin_validate.extractText())
                    showGmiDialog(
                            "ValidationResponse retrieved: ${validationResponse?.objectToString()}",
                            "PIN_VALIDATE"
                    )
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: IMSServerException) {
                    showGmiDialog(
                            "Exception validating PIN: ${e.localizedMessage}",
                            "PIN_VALIDATE"
                    )
                }
                showBusySpinner(false)
            }
        }

        //----------------------------
        //BUTTON: COUNT PENDING ENROLLMENTS
        button_count_pending_enrolls.setOnClickListener {
            Log.d("COUNT_ENROLL", "button_count_pending_enrolls clicked, counting pending enrolls for current user in background coroutine...")
            showBusySpinner()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    var pendingEnrollments = IMS.getPendingEnrollmentInfos(generateImsPerson(), validationResponse!!.tenantCode, edit_text_gmi_application_code.extractText())
                    if (pendingEnrollments == null || pendingEnrollments.isEmpty()) {
                        showGmiDialog(
                            "No pending enrollments for the current user!",
                            "COUNT_ENROLL"
                        )
                    } else {
                        showGmiDialog(
                            "Counted ${pendingEnrollments.size} pending enrollments for the current user!",
                            "COUNT_ENROLL"
                        )
                    }
                } catch (e: Exception) {
                    showGmiDialog(
                        "Must perform a previous step first!  Count of pending enrollments for current user failed, exception was ${e.localizedMessage}",
                        "COUNT_ENROLL",
                        e
                    )
                }
                showBusySpinner(false)
            }
        }



        //----------------------------
        //BUTTON: PERFORM PENDING ENROLLMENTS
        button_perform_pending_enrolls.setOnClickListener {
            Log.d("ENROLL", "button_perform_pending_enrolls clicked, performing pending enrolls for current user...")
            showBusySpinner()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    var pendingEnrollments = IMS.getPendingEnrollments(generateImsPerson(), validationResponse!!.tenantCode, edit_text_gmi_application_code.extractText())
                    if (pendingEnrollments == null || pendingEnrollments.isEmpty()) {
                        showGmiDialog(
                            "No pending enrollments for the current user!",
                            "ENROLL"
                        )
                    } else {
                        var resultStringBuilder = StringBuilder()
                        for (enrollment in pendingEnrollments) {
                            val response: IMS.EnrollResponse? = IMS.nativeEnroll(
                                this@MainActivity,
                                enrollment
                            )
                            resultStringBuilder.append("${enrollment.enrollInfo.captureType} success: ${response?.success()}\n")
                            Log.i("ENROLL", "enrollment result for ${enrollment.enrollInfo.captureType}: success: ${response?.success()} - ${response?.objectToString()}")
                        }

                        showGmiDialog(
                            "Processed ${pendingEnrollments.size} enrollments.\n$resultStringBuilder\nFor more details, see the Android logcat output.",
                            "ENROLL"
                        )
                    }
                } catch (e: Exception) {
                    showGmiDialog(
                        "Must perform a previous step first!  Perform pending enrollments for current user failed, exception was ${e.localizedMessage}",
                        "ENROLL",
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
                    var pendingAlerts = IMS.getMessagesForPerson(generateImsPerson(), validationResponse!!.tenantCode, edit_text_gmi_application_code.extractText())
                    if (pendingAlerts == null || pendingAlerts.isEmpty()) {
                        showGmiDialog("No pending alerts for the current user!", "COUNT_ALERTS")
                    } else {
                        showGmiDialog(
                            "Counted ${pendingAlerts.size} pending alerts for the current user!",
                            "COUNT_ALERTS"
                        )
                    }
                } catch (e: Exception) {
                    showGmiDialog(
                        "Must perform a previous step first!  Count of pending enrollments for current user failed, exception was ${e.localizedMessage}",
                        "COUNT_ALERTS",
                        e
                    )
                }
                showBusySpinner(false)
            }
        }





        //----------------------------
        //BUTTON: PERFORM PENDING ALERTS
        button_perform_pending_alerts.setOnClickListener {
            Log.d("ALERTS", "button_perform_pending_alerts clicked, performing pending alerts for current user...")
            showBusySpinner()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    var pendingAlerts = IMS.getMessagesForPerson(generateImsPerson(), validationResponse!!.tenantCode, edit_text_gmi_application_code.extractText())
                    if (pendingAlerts == null || pendingAlerts.isEmpty()) {
                        showGmiDialog("No pending alerts for the current user!", "ALERTS")
                    } else {
                        var resultStringBuilder = StringBuilder()
                        for (alert in pendingAlerts) {
                            val response = launchAndWaitForNativeMessage(
                                this@MainActivity,
                                alert
                            )

                            if (response?.successfulVerificationEvent == true) {    //If successful, we must mark the alert as "read" on the server or it will remain pending
                                IMS.markMessageAsRead(alert, validationResponse!!.tenantCode, validationResponse!!.personUuid, edit_text_gmi_application_code.extractText())
                            }

                            resultStringBuilder.append("${alert.template.substringAfterLast('/')} success: ${response?.successfulVerificationEvent}\n")
                            Log.i("ALERTS", "${alert.enrollInfo.captureType} success: ${response?.successfulVerificationEvent} - ${response?.objectToString()}")
                        }

                        showGmiDialog(
                            "Processed ${pendingAlerts.size} alerts.\n$resultStringBuilder\nFor more details, see the Android logcat output.",
                            "ALERTS"
                        )
                    }
                } catch (e: Exception) {
                    showGmiDialog(
                        "Must perform a previous step first!  Perform pending alerts for current user failed, exception was ${e.localizedMessage}",
                        "ALERTS",
                        e
                    )
                }
                showBusySpinner(false)
            }
        }

    }





    //------------------------------------------------------------------------------------
    //Utility functions to keep the above code clean and readable

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


    private fun showBusySpinner(showSpinner: Boolean = true) {
        lifecycleScope.launch(Dispatchers.Main) {
            touch_stealing_busy_overlay.visibility = if (showSpinner) View.VISIBLE else View.GONE
        }
    }

    private fun showGmiDialog(
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

    //------------------------------------------------------------------------
    //Kotin extensions functions

    fun IMSMessageInfo.pullMessage(imsPerson: IMSPerson): IMSMessage? =
        IMS.pullMessage(this, imsPerson, validationResponse!!.tenantCode, edit_text_gmi_application_code.extractText())

    private fun TextInputEditText.extractText() = text.toString()

    private fun Any.objectToString(): String? = GsonBuilder().create().toJson(this)
}
