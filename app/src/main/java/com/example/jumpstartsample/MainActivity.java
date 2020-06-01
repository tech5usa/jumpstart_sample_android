package com.example.jumpstartsample;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.iwsinc.ims.api.IMS;
import com.iwsinc.ims.api.IMSBiometricResponse;
import com.iwsinc.ims.api.IMSException;
import com.iwsinc.ims.api.IMSMessage;
import com.iwsinc.ims.api.IMSMessageInfo;
import com.iwsinc.ims.api.IMSPerson;
import com.iwsinc.ims.listeners.IMSRegisterDeviceListener;
import com.iwsinc.ims.listeners.IMSResponseListener;
import com.iwsinc.ims.response.IMSResponse;

public class MainActivity extends AppCompatActivity {

    private Button btnInitServerAPI;
    private Button btnInitSDK;
    private Button btnCheckPerson;
    private Button btnRegisterDevice;
    private Button btnEnroll;
    private Button btnVerify;
    private EditText editUserID;

    String myGmiServer = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION";
    String myClientId = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION";
    String myClientSecret = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION";
    String myServerId = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION";
    String myServerSecret = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION";
    String myTenant = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION";
    String myApplication = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION";
    String myVerificationTemplate = "PLEASE_LOOK_FOR_CREDENTIALS_AND_CONFIGURATION";
    String myUserID = "someone@example.com";

    String serverAPIBearerToken = null;

    boolean initializeServerAPISuccessful = false;
    boolean initializeSDKSuccessful = false;
    boolean deviceRegistered = false;
    boolean deviceVerified = false;

    IMSPerson personInTenant = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editUserID = findViewById(R.id.edit_user_id);
        editUserID.setText(myUserID);

        btnInitServerAPI = findViewById(R.id.btn_init_server_api);
        btnInitSDK = findViewById(R.id.btn_init_sdk);
        btnCheckPerson = findViewById(R.id.btn_check_person);
        btnRegisterDevice = findViewById(R.id.btn_register_device);
        btnEnroll = findViewById(R.id.btn_enroll);
        btnVerify = findViewById(R.id.btn_verify);

        btnInitServerAPI.setOnClickListener(v -> onInitServerAPIClicked());
        btnInitSDK.setOnClickListener(v -> onInitSDKClicked());
        btnCheckPerson.setOnClickListener(v -> onCheckPersonClicked());
        btnRegisterDevice.setOnClickListener(v -> onRegisterDeviceClicked());
        btnEnroll.setOnClickListener(v -> onEnrollClicked());
        btnVerify.setOnClickListener(v -> onVerifyClicked());
    }

    private void onInitServerAPIClicked() {
        Thread newThread = new Thread(() -> {

            // initialization of server side bearer token for making API calls (should be done in the back-end, not in the mobile app, shown here purely for illustration purposes)
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://" + myGmiServer +
                            "/usermanager/oauth/token?scope=SCOPE_TENANT_ADMIN&grant_type=client_credentials")
                    .post(RequestBody.create(null, new byte[]{}))
                    .addHeader("authorization", "Basic " + Base64.encodeToString((myServerId + ":" +
                            myServerSecret).getBytes(), Base64.NO_WRAP))
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("cache-control", "no-cache")
                    .build();
            Response response = null;
            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d("REQUEST", request.toString());
            Log.d("RESPONSE", String.valueOf(response));
            // use this bearer token in subsequent API calls
            try {
                if (response != null) {
                    serverAPIBearerToken = (new
                            JSONObject(Objects.requireNonNull(response.body()).string())).getString("access_token");
                    initializeServerAPISuccessful = true;
                    showDialog("Server API Initialized");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                showDialog(e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                showDialog(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                showDialog(e.getMessage());
            }

            Log.d("TOKEN", serverAPIBearerToken);

        });

        newThread.start();

    }

    private void onInitSDKClicked() {
        IMS.startIMS(getApplicationContext(), "https://" + myGmiServer + "/gmiserver", myTenant, myApplication);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    IMS.acquireOAuthCredentials(myClientId, myClientSecret);
                    initializeSDKSuccessful = true;
                    showDialog("SDK Initialized");
                } catch (Exception e) {
                    e.printStackTrace();
                    showDialog(e.getMessage());
                }
            }
        });
        thread.start();

    }

    private void onCheckPersonClicked() {
        if(!initializeSDKSuccessful) {
            showDialog("SDK Not initialized!");
        } else {
            myUserID = editUserID.getText().toString();
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // the person should already have been added to GMI using the Admin Portal, else, use API to add person to GMI and tenant
                    IMSPerson personInGMI = null;
                    try {
                        personInGMI = IMS.getPersonWithUserId(myUserID, IMS.getServer());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (personInGMI != null) {
                        try {
                            personInTenant = IMS.getPersonTenantData(personInGMI.id, IMS.getServer());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (personInTenant != null) {
                            // person is ready to be used - use personInTenant for all subsequent operations
                            showDialog("Check Person Complete");
                        } else {
                            // person is in GMI, but not in this tenant
                            // add this person to this tenant
                            addPersonToTenant(personInGMI.id);
                            try {
                                personInTenant = IMS.getPersonTenantData(personInGMI.id, IMS.getServer());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            showDialog("Check Person Complete");
                        }
                    } else {
                        // person not in GMI
                        // add this person to GMI and to the tenant
                        addPersonToGMIAndTenant(myUserID);
                        try {
                            personInGMI = IMS.getPersonWithUserId(myUserID, IMS.getServer());
                            personInTenant = IMS.getPersonTenantData(personInGMI.id, IMS.getServer());
                            showDialog("Check Person Complete");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            thread.start();
        }
    }

    private void onRegisterDeviceClicked() {
        if(personInTenant == null) {
            showDialog("personInTenant Not initialized!");
        } else {
            IMS.registerDeviceWithUserId(personInTenant.userId, new IMSRegisterDeviceListener() {
                @Override
                public void onDeviceRegistered(int i, String s, String s1) {
                    deviceRegistered = true;

                    // Device was registered -- now verify the device registration from server side.
                    verifyDeviceRegistration();
                }

                @Override
                public void onDeviceRegistrationError(IMSException e) {
                    deviceRegistered = false;
                    deviceVerified = false;
                    showDialog("Failed to register/verify device");
                }
            });
        }
    }

    private void onEnrollClicked() {
        if(!deviceVerified) {
            showDialog("Device Registration and validation not complete!!");
        } else {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        IMSMessage[] msgs = IMS.getPendingEnrollments(personInTenant);

                        if (msgs != null && msgs.length > 0) {
                            // process one enrollment message at a time
                            IMSMessage msg = msgs[0];

                            IMS.renderMessage(msg, getApplicationContext(), new IMSResponseListener() {
                                @Override
                                public void onResponse(IMSResponse imsResponse) {
                                    IMSBiometricResponse[] enrollEvents = null;
                                    try {
                                        enrollEvents = imsResponse.getEnrollEvents();
                                    } catch (Exception ignore) {

                                    }

                                    boolean isEnrollResponse = enrollEvents != null && enrollEvents.length > 0;
                                    if (isEnrollResponse) {
                                        try {
                                            personInTenant = IMS.getPersonTenantData(personInTenant.id, IMS.getServer());
                                        } catch (Exception e) {

                                        }
                                        // there may be more enroll events....
                                        onEnrollClicked();
                                    } else {
                                        String errorStr = (imsResponse.getRejectionString() != null) ? imsResponse.getRejectionString() : "User Cancelled.";
                                        showDialog(errorStr);
                                    }
                                }
                            });
                        } else {
                            // We have looped all enrollment messages and user is Enrolled. We are done.
                            // Fire message back to calling Activity.
                            showDialog("All enrollments complete!");
                            personInTenant = IMS.getPersonTenantData(personInTenant.id, IMS.getServer());
                        }
                    } catch (Exception e) {
                        showDialog(e.getMessage());
                    }
                }
            });
            thread.start();
        }
    }

    private void onVerifyClicked() {
        if(!deviceVerified) {
            showDialog("Device Registration and validation not complete!!");
        } else {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String messageId = CreateVerificationRequest();
                    try {
                        IMSMessageInfo messageInfo = IMS.getInfoFromId(messageId, personInTenant.id);
                        IMSMessage messageToVerify = IMS.pullMessage(messageInfo, personInTenant);
                        IMS.renderMessage(messageToVerify, getApplicationContext(), new IMSResponseListener() {
                            @Override
                            public void onResponse(IMSResponse imsResponse) {
                                boolean success = false;
                                String errorStr = "";
                                IMSBiometricResponse[] verifyEvents = imsResponse.getVerifyEvents();
                                if (verifyEvents.length > 0) {
                                    // user may have retried. So look for a successful event
                                    for (IMSBiometricResponse event : verifyEvents) {
                                        if (event.success) {
                                            success = true;
                                            break;
                                        }
                                    }

                                    // did the user back out (hit cancel)?
                                    Boolean userBackHit = imsResponse.didUserBackOutOfMessage();
                                    try {
                                        //do not mark as read/rejected if user backed out, in background, or sleep.
                                        if (!userBackHit) {
                                            if (imsResponse.getRejectionString() != null) {
                                                errorStr = imsResponse.getRejectionString();
                                                IMS.markMessageAsRejected(imsResponse.getMessageInfo(), imsResponse.getRejectionString());
                                                showDialog(errorStr);
                                                return;
                                            } else {
                                                IMS.markMessageAsRead(imsResponse.getMessageInfo());
                                                return;
                                            }
                                        }
                                    } catch (Exception e) {

                                    }


                                    // use the value of the "success" boolean variable as appropriate
                                    if(success) {
                                        showDialog("Verification successful");
                                    } else {
                                        showDialog("Verification failed");
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {

                    }
                }
            });
            thread.start();
        }
    }

    private String CreateVerificationRequest() {
        // create a verification request using API calls (should be done in the back-end, not in the mobile app, shown here purely for illustration purposes)
        if(!initializeServerAPISuccessful) {
            showDialog("API Not initialized!");
        } else {
            try {
                OkHttpClient client = new OkHttpClient();

                JSONObject json = new JSONObject();
                json.put("maxResponseAttempts", "2");
                json.put("template", myVerificationTemplate);

                JSONObject metadata = new JSONObject();
                metadata.put("reason", "Please verify your identity before proceeding");

                json.put("metadata", metadata);

                Request request = new Request.Builder()
                        .url("https://" + myGmiServer +
                                "/gmiserver/tenant/" + myTenant + "/app/" + myApplication + "/template/" + myVerificationTemplate + "/person/" + personInTenant.id + "/message")
                        .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json.toString()))
                        .addHeader("authorization", "Bearer " + serverAPIBearerToken)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("cache-control", "no-cache")
                        .build();
                Response response = null;
                try {
                    response = client.newCall(request).execute();
                    String messageId = (new JSONObject(response.body().string())).getString("messageId");
                    return messageId;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {

            }
            return "";
        }
        return "";
    }


    private void addPersonToTenant(String personId) {
        // adding an existing person in GMI to the tenant using API calls (should be done in the back-end, not in the mobile app, shown here purely for illustration purposes)
        if(!initializeServerAPISuccessful) {
            showDialog("API Not initialized!");
        } else {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://" + myGmiServer +
                            "/gmiserver/tenant/" + myTenant + "/person/" + personId)
                    .put(RequestBody.create(null, new byte[]{}))
                    .addHeader("authorization", "Bearer " + serverAPIBearerToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("cache-control", "no-cache")
                    .build();
            Response response = null;
            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addPersonToGMIAndTenant(String userId){
        // adding a person to GMI and the tenant using API calls (should be done in the back-end, not in the mobile app, shown here purely for illustration purposes)
        if(!initializeServerAPISuccessful) {
            showDialog("API Not initialized!");
        } else {
            try {
                OkHttpClient client = new OkHttpClient();

                JSONObject json = new JSONObject();
                json.put("userId", userId);

                Request request = new Request.Builder()
                        .url("https://" + myGmiServer +
                                "/gmiserver/tenant/" + myTenant + "/person")
                        .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json.toString()))
                        .addHeader("authorization", "Bearer " + serverAPIBearerToken)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("cache-control", "no-cache")
                        .build();
                Response response = null;
                try {
                    response = client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {

            }
        }
    }

    private void verifyDeviceRegistration() {
        // verifying device registration using API calls (should be done in the back-end, not in the mobile app, shown here purely for illustration purposes)
        if(!initializeServerAPISuccessful) {
            showDialog("API Not initialized!");
        } else {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("https://" + myGmiServer +
                                "/gmiserver/tenant/" + myTenant + "/person/validate?userId=" + URLEncoder.encode(personInTenant.userId, "UTF-8") + "&deviceId=" + IMS.getThisDevice().deviceId + "&appCode=" + myApplication)
                        .post(RequestBody.create(null, new byte[]{}))
                        .addHeader("authorization", "Bearer " + serverAPIBearerToken)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("cache-control", "no-cache")
                        .build();
                Response response = null;
                try {
                    response = client.newCall(request).execute();
                    deviceVerified = true;
                    showDialog("Device registered and verified");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void showDialog(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                        //.setTitle("Alert")
                        .setMessage(msg)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                builder.show();
            }
        });
    }


}
