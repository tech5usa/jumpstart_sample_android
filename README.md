

The sample app was built on Android Studio v `4.1.1` - which was the current release at the time.


#IMPORTANT:
Once you clone this repo or download the ZIP file:
please do a find-in-files for `PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION` in the source code and replace with configuration provided to you separately by your ImageWare contact.
The `DEVICE_ID_TO_2FA` value can be retrieved from the Android logcat log output at the appropriate time during the sample app's operation - see below.





#WALKTHROUGH OF SAMPLE CODE USAGE:
NOTE - You might have to manually add the following import in order to work with the `Result` object properly (the IDE for some reason does not automatically import it) :
import com.iwsinc.ims.api.Result


1)  In `MainActivity.kt` - Ensure the `PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION` values have all been replaced by actual configuration values.  (Lines 26-32)

2)  Compile and run the `app` module on an Android device, have the `logcat` log output up.  

3)  The configuration values you entered should be displayed at the top of the screen on your Android device.  If needed, you may edit them here.  Your changes will not persist.

4)  Upon app startup, `MainActivity.kt` lines 56-58 handle initializing the GMI SDK and its Managers

5)  Tap the `REGISTER USER` button, wait for the dialog to confirm success.   This button event is handled in `MainActivity.kt` line 78.
First, we construct the needed Configuration and Profile objects on lines 83-97
Then, we call `accountServiceManager.register()` on line 100 to attempt to register the user / retrieve registration status for the user.
We utilize a Kotlin Flow return object with a .collect {} call to react to emitted events,
    such as Result.Success.RegistrationStatus.PENDING_VERIFICATION or Result.Success.RegistrationStatus.USER_VERIFIED, starting on line 101

5a) Once the user has been verified / 2 factor authentication is completed, you can tap any of the other buttons

6) Tap the `COUNT PENDING ENROLLS AND ALERTS` button to get separate counts for pending enrolls and alerts

7) Tap the `SYNC AND RUN FIRST EXPECTED ITEM` button to check messages and render the first one (enrolls first if pending).  Tap again to run the next item.

8) Tap the `SHOW PROFILE DETAILS` button to construct, sort, and display a basic profile detail showing enrollment status for each biometric for each associated tenant


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

Info about Kotlin Flow can be found here: https://developer.android.com/kotlin/flow

Info about Kotlin Lifecycle-Scope Coroutines, which is what allows this app to require such little actual code:  https://developer.android.com/topic/libraries/architecture/coroutines

Info about Kotlin Synthetic Properties, which is what allows us to casually reference the View components in code by their XML `id` without any `findViewById()` calls but is perhaps not the most code space efficient way to go but it is nice and simple:  https://medium.com/@iateyourmic/synthetic-accessors-in-kotlin-a60184afd94e  

Info about how to view the Android `logcat` log output in Android Studio:  https://developer.android.com/studio/debug/am-logcat  

Info about how to view the Android `logcat` log output with a command-line tool:  https://developer.android.com/studio/command-line/logcat
