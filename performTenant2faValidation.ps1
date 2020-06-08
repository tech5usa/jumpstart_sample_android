# The below PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION strings should be replaced with your details
$GMI_SERVER_URL='PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
$GMI_USERMANAGER_URL='PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
$GMI_APP_CODE='PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
$GMI_TENANT_CODE='PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
$GMI_TENANT_OAUTH_CREDENTIALS_BASE64_ENCODED='PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
$URLENCODED_USER_ID_TO_2FA='PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
$DEVICE_ID_TO_2FA='PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
 
$headers = @{
    Authorization = "Basic $GMI_TENANT_OAUTH_CREDENTIALS_BASE64_ENCODED"
    'Content-Type' = 'application/x-www-form-urlencoded'
    }


Write-Host 'Requesting bearer token to access tenant API using provided Authorization in this performTenant2faValidation.sh script... if this fails please add your Base64-encoded OAuth credentials below in place of the text PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
$response = Invoke-WebRequest -Uri "$GMI_USERMANAGER_URL/oauth/token?scope=IGNORED&grant_type=client_credentials" -Headers $headers -Method Post -UseBasicParsing

Write-Host $response
Write-Host
Write-Host
Write-Host 'If the bearer token request was successful, it should be shown above this line in the response text.  For the next step, please copy and paste that token into the following prompt, then press Enter'
Write-Host
$TOKEN=""

DO
{
    "Bearer token variable is currently $TOKEN - if blank let user enter manually"
    $TOKEN = Read-Host -Prompt 'Please enter the bearer token from the above request'
    "Bearer token variable has been updated to $TOKEN, checking if valid to continue..."
} While ($TOKEN -eq "")

$headers = @{
    Authorization = "Bearer $TOKEN"
    Accept = 'application/json'
}

$response = Invoke-WebRequest -Uri "$GMI_SERVER_URL/tenant/$GMI_TENANT_CODE/person/validate?userId=$URLENCODED_USER_ID_TO_2FA&deviceId=$DEVICE_ID_TO_2FA&appCode=$GMI_APP_CODE" -Headers $headers -Method Post -UseBasicParsing

Write-Host $response
Write-Host
Write-Host
Write-Host '2FA complete if you do not see any errors above!  A 404 means that no matching pending registration exists....'
