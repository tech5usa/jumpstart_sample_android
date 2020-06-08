#!/bin/sh

#The below PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION strings should be replaced with your details
GMI_SERVER_URL='PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
GMI_USERMANAGER_URL='PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
GMI_APP_CODE='PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
GMI_TENANT_CODE='PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
GMI_TENANT_OAUTH_CREDENTIALS_BASE64_ENCODED='PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
URLENCODED_USER_ID_TO_2FA='PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
DEVICE_ID_TO_2FA='PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'

echo 'Requesting bearer token to access tenant API using provided Authorization in this performTenant2faValidation.sh script... if this fails please add your Base64-encoded OAuth credentials below in place of the text PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION'
curl --location --request POST "${GMI_USERMANAGER_URL}/oauth/token?scope=IGNORED&grant_type=client_credentials" \
--header "Authorization: Basic ${GMI_TENANT_OAUTH_CREDENTIALS_BASE64_ENCODED}" \
--header 'Content-Type: application/x-www-form-urlencoded'

echo
echo
echo 'If the bearer token request was successful, it should be shown above this line in the response text.  For the next step, please copy and paste that token into the following prompt, then press Enter'
echo
TOKEN=""
while [ -z "${TOKEN}" ]; do
    echo -n "Please enter your bearer token: "
    read T
    if [ -z "${T}" ]; then
        echo
        echo "ERROR: You must set the auth token."
        echo
    else
        echo
        echo -n "Setting bearer token to '${T}'.  Is this correct? [Y/n] "
        read YESNONAME
        [ "${YESNONAME}" = "" -o "${YESNONAME}" = "y" -o "${YESNONAME}" = "Y" ] && TOKEN=${T}
        echo
    fi
done

curl --location --request POST "${GMI_SERVER_URL}/tenant/${GMI_TENANT_CODE}/person/validate?userId=${URLENCODED_USER_ID_TO_2FA}&deviceId=${DEVICE_ID_TO_2FA}&appCode=${GMI_APP_CODE}" \
--header "Authorization: Bearer ${TOKEN}" \
--header 'Accept: application/json' \
--data-raw ''

echo
echo
echo '2FA complete if you do not see any errors above!  A 404 indicates that no matching pending registration was found.'
