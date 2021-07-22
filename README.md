IWA SDK Sample Application

# IMPORTANT:
Once you clone this repo or download the ZIP file:
please do a find-in-files for `PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION` in the source code and replace with configuration provided to you separately by your ImageWare contact (MainActivity.kt, lines 30-35)

The sample app was built on Android Studio v `4.2.1` - which was the current release at the time.





# WALKTHROUGH OF SAMPLE CODE USAGE:
1)  In `MainActivity.kt` - Ensure the `PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION` values have all been replaced by actual configuration values.
    When these values have been entered into the source code, the app will pre-fill the text boxes with this information for much faster testing.

2)  Compile and run the `iwasdksampleapp` module on an Android device

3)  The configuration values you entered should be displayed at the top of the screen on your Android device.  If needed, you may edit them here.  Your changes in the app will not persist.

4)  Tap the `INITIALIZE IWA SDK` button, wait for the dialog to confirm success.   This button event is handled in `MainActivity.kt` line 63.
    First, create an AccountServiceManager object.  The constructor requires an Android Context to be passed in.
    Next, create a MessagesServiceManager object.  The constructor requires an Android Context to be passed in.
    Finally, on your MessagesServiceManager object, call the .register() method.  This method requires a FragmentActivity (AppCompatActivity is fine) and an InteractionManagerListener.
        In this example MainActivity implements the InteractionManagerListener, which has the event methods implemented starting on line 454.

5)  Tap the `REGISTER AND SET EMAIL / USERID` button to attempt to register / check with the server if this user is valid.
    Wait for the dialog to confirm success, then you should have an email waiting for you with at least one validation PIN code.
    You will receive one PIN code for each tenant your email is associated with on the current server.
    This button event is handled in `MainActivity.kt` line 76, and the registration result is handled on line 377 in the `handleRegistrationIwaResult` method.

6)  Check your email for the PIN codes; choose one, each is tied to a specific tenant.  Enter the PIN code in the "PIN to validate" field.  Tap the "Validate PIN" button.  Wait for the dialog to confirm success.
    and to see which tenant you now have set up.
    This button event is handled in `MainActivity.kt` line 102, and the validation result is handled on line 347 in the `handleValidationIwaResult` method.

7)  (Optional) If you'd like to send the server a Firebase push token, the server will send push notifications to that Firebase device.  This sample app does not contain Firebase.
    In order to test this out, enter the push token into the "Push token to register with IWA server" text field and tap the `UPDATE PUSH TOKEN ON IWA SERVER` button.  Wait for the dialog to confirm success.
    This button event is handled in `MainActivity.kt` line 116, and the update result is handled on line 333 in the `handleUpdatePushIwaResult` method.

8)  Tap the `SYNCHRONIZE WITH SERVER` button to perform a sync operation, which will
    pull pending enrollments / completed enrollments / pending alerts into the SDK database and prepare for the buttons below this one to function properly.  Wait for the dialog to confirm success.
    This button event is handled in `MainActivity.kt` line 183.  This method will return once complete and display the result.

9)  Tap the `COUNT PENDING ENROLLMENTS` button to count pending enrollments.  Wait for the dialog to confirm success and display count.
    This button event is handled in `MainActivity.kt` line 131, and this should display a result instantly as it is local data after the synchronize.

10) Tap the `COUNT PENDING ALERTS` button to count pending alerts.  Wait for the dialog to confirm success and display count.
    This button event is handled in `MainActivity.kt` line 157, and this should display a result instantly as it is local data after the synchronize.

11) Tap the `RENDER NEXT WORK ITEM` button to render the next work item (repeat as necessary).  If there are any enrolls pending, those will be shown first.  Then, alerts will be shown next.
    Wait for the dialog at the end of the enroll or alert to confirm success.  This button event is handled in `MainActivity.kt` line 214, and the results will be passed back to the InteractionManagerListener
        methods starting on line 454.

12) Tap the `SET WORK ITEMS UNREAD` button to set work items to "unread" status (if you skipped an enroll or alert, this will allow the RENDER button to show those items again).
    Wait for the dialog at the end to confirm success.
    This button event is handled in `MainActivity.kt` line 233, and this should display a result instantly as it is local data manipulation.

13) Tap the `UNHIDE ALL ENROLLS` button to unhide all enrolls (if you hid an enroll, this will allow the RENDER button to show it again).
    Wait for the dialog at the end to confirm success.
    This button event is handled in `MainActivity.kt` line 256, and this should display a result instantly as it is local data manipulation.

14) Tap the `UNREGISTER FIRST ACCOUNT` button to unregister the first account in the database.
    Wait for the dialog at the end to confirm success.
    This button event is handled in `MainActivity.kt` line 277.


# TROUBLESHOOTING:
In the walkthrough, if at any point you get an unexpected error in the response, it is very possible that you either:
A)  Missed a previous step (one must initialize the SDK before anything else will function, for example -- and one can not check enrolls
    or alerts without first successfully validating 2nd factor authentication / 2FA as well as synchronizing)
B)  Entered some information incorrectly











# LINKS:
This information is available in further details on Confluence here:  https://imageware.atlassian.net/wiki/spaces/MA/pages/755204122/How+To+Integrate+Sample+App+Details

Please also see the quick-reference integration.txt file located here, which goes over how to add the SDK to your project:  https://imageware.atlassian.net/wiki/spaces/MA/pages/704938246

The sample app Github repository is located here:  https://github.com/ImageWare/jumpstart_sample_android

Info about Kotlin, the current Android standard programming language which this sample app is written in:  https://developer.android.com/kotlin

Info about Kotlin Lifecycle-Scope Coroutines, which is what allows this app to require such little actual code:  https://developer.android.com/topic/libraries/architecture/coroutines

Info about Kotlin Synthetic Properties, which is what allows us to casually reference the View components in code by their XML `id` without any `findViewById()` calls but is perhaps not the most code space efficient way to go but it is nice and simple:  https://medium.com/@iateyourmic/synthetic-accessors-in-kotlin-a60184afd94e  

Info about how to view the Android `logcat` log output in Android Studio:  https://developer.android.com/studio/debug/am-logcat  

Info about how to view the Android `logcat` log output with a command-line tool:  https://developer.android.com/studio/command-line/logcat
