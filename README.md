

The sample app was built on Android Studio v `4.0` - which was the current release at the time.


#IMPORTANT:
Once you clone this repo or download the ZIP file:
please do a find-in-files for `PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION` in the source code and replace with configuration provided to you separately by your ImageWare contact.
The `DEVICE_ID_TO_2FA` value can be retrieved from the Android logcat log output at the appropriate time during the sample app's operation - see below.






#WALKTHROUGH OF SAMPLE CODE USAGE:
1)  In `MainActivity.kt` - Ensure the `PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION` values have all been replaced by actual configuration values.  (Lines 26-32)

2)  Compile and run the `app` module on an Android device, have the `logcat` log output up.  

3)  The configuration values you entered should be displayed at the top of the screen on your Android device.  If needed, you may edit them here.  Your changes will not persist.

4)  Tap the `INITIALIZE GMI SDK` button, wait for the dialog to confirm success.   This button event is handled in `MainActivity.kt` line 59.
First, we must call `IMS.startIMS()`  on line 59 to start the SDK.
Next, we must call `IMS.setUserManagerUrl()`  on line 65 to set the appropriate usermanager URL.
Next, we should call `IMS.getThisDevice().setDeviceId(UUID.randomUUID().toString())` on line 68 to set a unique "device ID" for this particular account association; this ensures unique push notifications from the GMI server for this account.
Next, we must call `IMS.acquireOAuthCredentials()`  on line 70 to actually acquire credentials to access the GMI server

5)  The User ID or email address you entered should be displayed in the `Email / UserID` field.  If needed, you may edit it here.  Your changes will not persist.

6)  Tap the `REGISTER AND SET EMAIL / USERID` button to attempt to register (associate the current device with the current user ID on the GMI server) / check with the server if this user is valid,
and if so set the SDK's current user for future operations.  Wait for the dialog to confirm success, then you should have an email waiting for you.
This button event is handled in `MainActivity.kt` line 91.
If an IMSServerException is thrown, one of the following conditions is true:
    a) Email does not exist on this server
    b) No unverified PINs remain for this account on this device ID
    c) The GMI SDK is not properly initialized

7)  Check your email for the PIN codes; choose one, each is tied to a specific tenant.  Enter the PIN code in the "PIN to validate" field.  Tap the "Validate PIN" button.  Wait for the dialog to confirm success
and to see which tenant you now have set up (only one tenant at a time can be tested with this sample app with the simple sample app logic, it only keeps track of one user / tenant)
This button event is handled in `MainActivity.kt` line 115.

8) Tap the `COUNT PENDING ENROLLMENTS` button to count pending enrollments.  Wait for the dialog to confirm success and display count.
This button event is handled in `MainActivity.kt` line 139.
To count pending enrollments, we call `IMS.getPendingEnrollmentInfos()` to get a quick list of enrollments pending on line 200.  

9) Tap the `PERFORM PENDING ENROLLMENTS` button to perform pending enrollments; the app will retrieve the list of them and then iterate through them one at a time and allow you to enroll.
Wait for the dialog at the end to confirm successes.  This button event is handled in `MainActivity.kt` line 171.
To get a detailed list of pending enrollments suitable to directly pass to the SDK for processing, we call `IMS.getPendingEnrollments()` on line 176.
Then, we iterate through this list and call `IMS.nativeEnroll()` as demonstrated on line 185.

10) Tap the `COUNT PENDING ALERTS` button to count pending alerts.  Wait for the dialog to confirm success and display count.
This button event is handled in `MainActivity.kt` line 217.
To count pending alerts, we call `IMS.getMessagesForPerson()` on line 222.

11) Tap the `PERFORM PENDING ALERTS` button to perform pending alerts; the app will retrieve the list of them and then iterate through them one at a time and allow you to perform verifications.
Wait for the dialog at the end to confirm successes.  This button event is handled in `MainActivity.kt` line 248.
To get a detailed list of pending alerts suitable to directly pass to the SDK for processing, we again call `IMS.getMessagesForPerson()` on line 253.
Then, we iterate through this list and call `IMS.renderMessage()` as demonstrated on line 311 (after an `IMS.pullMessage() call to fill out the rest of the required info, on line 306).
The `launchAndWaitForNativeMessage()` method on line 303 wraps up the required logic necessary to properly resume from a coroutine from a completed alert.






#TROUBLESHOOTING: 
In the walkthrough, if at any point you get an unexpected error in the response, it is very possible that you either:
A)  Missed a previous step (one must initialize the SDK before anything else will function, for example -- and one can not check enrolls or alerts without first successfully validating 2nd factor authentication / 2FA)
B)  Entered some information incorrectly








#VIDEOS: 
(NOTE: Need sanitized videos without actual credentials if we are to share them with external customers)





#LINKS:
This information is available in further details on Confluence here:  https://imageware.atlassian.net/wiki/spaces/MA/pages/755204122/How+To+Integrate+Sample+App+Details

Please also see the quick-reference integration.txt file located here, which goes over how to add the SDK to your project:  https://imageware.atlassian.net/wiki/spaces/MA/pages/704938246

The sample app Github repository is located here:  https://github.com/ImageWare/jumpstart_sample_android

Info about Kotlin, the current Android standard programming language which this sample app is written in:  https://developer.android.com/kotlin

Info about Kotlin Lifecycle-Scope Coroutines, which is what allows this app to require such little actual code:  https://developer.android.com/topic/libraries/architecture/coroutines

Info about Kotlin Synthetic Properties, which is what allows us to casually reference the View components in code by their XML `id` without any `findViewById()` calls but is perhaps not the most code space efficient way to go but it is nice and simple:  https://medium.com/@iateyourmic/synthetic-accessors-in-kotlin-a60184afd94e  

Info about how to view the Android `logcat` log output in Android Studio:  https://developer.android.com/studio/debug/am-logcat  

Info about how to view the Android `logcat` log output with a command-line tool:  https://developer.android.com/studio/command-line/logcat
