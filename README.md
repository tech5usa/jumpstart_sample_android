
#IMPORTANT:
Once you clone this repo or download the ZIP file:
please do a find-in-files for `PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION` in the source code and replace with configuration provided to you separately by your ImageWare contact.
The `DEVICE_ID_TO_2FA` value can be retrieved from the Android logcat log output at the appropriate time during the sample app's operation - see below.




#NOTE: 
In the walkthrough, if at any point you get an unexpected error in the response, it is very possible that you either:
A)  Missed a previous step (one must initialize the SDK before anything else will function, for example -- and one can not check enrolls or alerts without first successfully validating 2nd factor authentication / 2FA)
B)  Entered some information incorrectly






#WALKTHROUGH OF SAMPLE CODE USAGE:
1)  In `MainActivity.kt` - Ensure the `PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION` values have all been replaced by actual configuration values. 

2)  Compile and run the `app` module on an Android device, have the `logcat` log output up.

3)  The configuration values you entered should be displayed at the top of the screen on your Android device.  If needed, you may edit them here.  Your changes will not persist.

4)  Tap the `INITIALIZE GMI SDK` button, wait for the dialog to confirm success. 

5)  The User ID or email address you entered should be displayed in the `Email / UserID` field.  If needed, you may edit it here.  Your changes will not persist.

6)  Tap the `CHECK AND SET EMAIL / USERID` button to check with the server if this user is valid, and if so set the SDK's current user for future operations.  Wait for the dialog to confirm success.

7)  Tap the `REGISTER DEVICE TO USER` button to associate the current device with the current user ID on the GMI server.  Wait for the dialog to confirm success and read it carefully.  

8)  Perform your 2nd factor authentication, which may be either Email or Tenant.  If Email, click the link in the email you received.  
If Tenant, you can copy the `Device ID` value from your Android `logcat` log output - search for the `REGISTER` tag to find it.  
Once copied, paste this value into the `DEVICE_ID_TO_2FA` field in either `performTenant2faValidation.sh` (for Mac or Linux) or the `performTenant2faValidation.ps1` (for Windows 10 Powershell).
Ensure the script you are about to use also has the appropriate GMI configuration information entered in place of `PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION` 
Run the script and follow the directions to perform your 2nd factor authentication.

9)  Tap the `GET PERSON TENANT DATA` button to retrieve the user's tenant information.  Wait for the dialog to confirm success.

10) Tap the `COUNT PENDING ENROLLMENTS` button to count pending enrollments.  Wait for the dialog to confirm success and display count.

11) Tap the `PERFORM PENDING ENROLLMENTS` button to perform pending enrollments; the app will retrieve the list of them and then iterate through them one at a time and allow you to enroll.  
Wait for the dialog at the end to confirm successes.

12) Tap the `COUNT PENDING ALERTS` button to count pending alerts.  Wait for the dialog to confirm success and display count.

13) Tap the `PERFORM PENDING ALERTS` button to perform pending alerts; the app will retrieve the list of them and then iterate through them one at a time and allow you to perform verifications.  
Wait for the dialog at the end to confirm successes.







#VIDEOS: 
(NOTE: Need sanitized videos without actual credentials if we are to share them with external customers)





#LINKS:
Please also see the quick-reference integration.txt file located here:  https://imageware.atlassian.net/wiki/spaces/MA/pages/704938246

The sample app Github repository is located here:  https://github.com/ImageWare/jumpstart_sample_android
