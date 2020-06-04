package com.example.iwsgmisampleapp

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.GsonBuilder
import com.iwsinc.ims.api.IMS
import com.iwsinc.ims.api.IMSPerson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.StringBuilder

//TODO: Delete me: EKS-DEV
const val GMI_SERVER_URL = "https://eks-gmiserver-dev.iwsinc.com"
const val GMI_USER_MANAGER_URL = "https://eks-usermanager-dev.iwsinc.com"
const val GMI_CLIENT_ID = "GoVerifyID"
const val GMI_CLIENT_SECRET = "Un9#He!#TshDmv/Z"
const val GMI_TENANT_CODE = "ChrisTestAll"
const val GMI_APPLICATION_CODE = "GoVerifyID"
const val EMAIL_ADDRESS_USER_ID = "cpaiano+alltest@iwsinc.com"

//If the below constants are filled out with valid information, it will appear when the app launches:
//const val GMI_SERVER_URL = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
//const val GMI_USER_MANAGER_URL = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
//const val GMI_CLIENT_ID = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
//const val GMI_CLIENT_SECRET = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
//const val GMI_TENANT_CODE = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
//const val GMI_APPLICATION_CODE = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"
//const val EMAIL_ADDRESS_USER_ID = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION"

class MainActivity : AppCompatActivity() {

    private var personInGMI: IMSPerson? = null
    private var currentCoroutineJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //----------------------------
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
        //BUTTON: INITIALIZE GMI SDK
        button_init_gmi_sdk.setOnClickListener {
            Log.d("INIT_SDK", "button_init_gmi_sdk clicked, Initializing SDK with provided parameters in background coroutine...")
            currentCoroutineJob = lifecycleScope.launch(Dispatchers.IO) {
                try {
                    IMS.startIMS(
                        this@MainActivity,
                        edit_text_gmi_server_url.extractText(),
                        edit_text_gmi_tenant_code.extractText(),
                        edit_text_gmi_application_code.extractText()
                    )
                    IMS.setUserManagerUrl(edit_text_gmi_user_manager_url.extractText())
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
            }
        }





        //----------------------------
        //BUTTON: CHECK AND SET EMAIL
        button_check_and_set_email.setOnClickListener {
            Log.d("CHECK_EMAIL", "button_check_and_set_email clicked, checking provided email / user ID in background coroutine...")
            currentCoroutineJob = lifecycleScope.launch(Dispatchers.IO) {
                try {

                    personInGMI = IMS.getPersonWithUserId(
                        edit_text_email.extractText(),
                        IMS.getServer())

                    if (personInGMI == null) {
                        showGmiDialog(
                            "Provided user ID / email address is not available on the current GMI server!",
                            "CHECK_EMAIL"
                        )
                    } else {
                        showGmiDialog(
                            "Provided user ID / email address is valid, exists on the current GMI server, person ID is ${personInGMI?.id}",
                            "CHECK_EMAIL"
                        )
                    }
                } catch (e: Exception) {
                    showGmiDialog(
                        "Must perform a previous step first!  Check of provided email on current GMI server failed, exception was ${e.localizedMessage}",
                        "CHECK_EMAIL",
                        e
                    )
                }
            }
        }






        //----------------------------
        //BUTTON: REGISTER DEVICE TO USER
        button_register_device_to_user.setOnClickListener {
            Log.d("REGISTER", "button_register_device_to_user clicked, registering this device to provided email / user ID in background coroutine...")
            currentCoroutineJob = lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val registrationResult = IMS.registerDeviceWithUserId(personInGMI!!.userId)
                    showGmiDialog(
                        "Registration request submitted!  Registration result is $registrationResult.  Check your email for a 2FA link to validate your registration.  Once you have clicked this link, you may continue.",
                        "REGISTER"
                    )
                } catch (e: Exception) {
                    showGmiDialog(
                        "Must perform a previous step first!  Registration on current GMI server failed, exception was ${e.localizedMessage}",
                        "REGISTER",
                        e
                    )
                }
            }
        }







        //----------------------------
        //BUTTON: GET PERSON TENANT DATA
        button_get_person_tenant_data.setOnClickListener {
            Log.d("TENANT_DATA", "button_get_person_tenant_data clicked, retrieving Person Tenant Data in background coroutine...")
            currentCoroutineJob = lifecycleScope.launch(Dispatchers.IO) {
                try {
                    personInGMI = IMS.getPersonTenantData(personInGMI!!.id, IMS.getServer())
                    showGmiDialog(
                        "PersonTenantData retrieved: ${personInGMI!!.objectToString()}",
                        "TENANT_DATA"
                    )
                } catch (e: Exception) {
                    showGmiDialog(
                        "Must perform a previous step first!  Retrieval of Person Tenant Data on current GMI server failed, exception was ${e.localizedMessage}",
                        "TENANT_DATA",
                        e
                    )
                }
            }
        }








        //----------------------------
        //BUTTON: COUNT PENDING ENROLLMENTS
        button_count_pending_enrolls.setOnClickListener {
            Log.d("COUNT_ENROLL", "button_count_pending_enrolls clicked, counting pending enrolls for current user in background coroutine...")
            currentCoroutineJob = lifecycleScope.launch(Dispatchers.IO) {
                try {
                    var pendingEnrollments = IMS.getPendingEnrollmentInfos(personInGMI)
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
            }
        }







        //----------------------------
        //BUTTON: PERFORM PENDING ENROLLMENTS
        button_perform_pending_enrolls.setOnClickListener {
            Log.d("ENROLL", "button_perform_pending_enrolls clicked, performing pending enrolls for current user...")
            currentCoroutineJob = lifecycleScope.launch(Dispatchers.IO) {
                try {
                    var pendingEnrollments = IMS.getPendingEnrollments(personInGMI)
                    if (pendingEnrollments == null || pendingEnrollments.isEmpty()) {
                        showGmiDialog(
                            "No pending enrollments for the current user!",
                            "ENROLL"
                        )
                    } else {
                        var enrollResultStringBuilder = StringBuilder()
                        for (enrollment in pendingEnrollments) {
                            val response: IMS.EnrollResponse? = IMS.nativeEnroll(
                                this@MainActivity,
                                enrollment
                            )
                            enrollResultStringBuilder.append("${enrollment.enrollInfo.captureType} success: ${response?.success()}\n")
                            Log.i("ENROLL", "enrollment result for ${enrollment.enrollInfo.captureType}: success: ${response?.success()} - ${response?.objectToString()}")
                        }

                        showGmiDialog(
                            "Processed ${pendingEnrollments.size} enrollments.\n$enrollResultStringBuilder\nFor more details, see the Android logcat output.",
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
            }
        }







        //----------------------------
        //BUTTON: COUNT PENDING ALERTS
        button_count_pending_alerts.setOnClickListener {
            Log.d("COUNT_ALERTS", "button_count_pending_alerts clicked, counting pending alerts for current user in background coroutine...")
            currentCoroutineJob = lifecycleScope.launch(Dispatchers.IO) {
                try {
                    var pendingAlerts = IMS.getMessagesForPerson(personInGMI)
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
            }
        }





    }





    //------------------------------------------------------------------------------------
    //Utility functions to keep the above code clean and readable

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

            dialog?.setButton(Dialog.BUTTON_POSITIVE, "OK", DialogInterface.OnClickListener { dialog, _ -> dialog.dismiss() })
            dialog?.show()
        }
    }

    private fun TextInputEditText.extractText() = text.toString()

    private fun Any.objectToString(): String? = GsonBuilder().create().toJson(this)
}
