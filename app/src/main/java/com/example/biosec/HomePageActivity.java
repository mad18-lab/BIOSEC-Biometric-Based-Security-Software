package com.example.biosec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.biosec.network.SecApiService;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import com.example.biosec.network.ApiService;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
//import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import retrofit2.Callback;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import com.example.biosec.util.*;
import com.example.biosec.model.*;

public class HomePageActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    GoogleSignInClient gsc;
    BiometricPrompt biometricPrompt = null;
    Executor executor = Executors.newSingleThreadExecutor();
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    ActionBarDrawerToggle drawerToggle;
    Button scanButton;
    SharedPreferences sharedPreferences;
    private static final int REQUEST_CODE_ENROLL_FINGERPRINT = 103;
    OkHttpClient client;
    WebSocket webSocket;
    Handler handler;
    ApiService apiService;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(drawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);  //to lock the home page screen as portrait
        setContentView(R.layout.activity_homepage);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        drawerToggle = new ActionBarDrawerToggle(this,drawerLayout,R.string.open,R.string.close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        scanButton = findViewById(R.id.scanner);


        mAuth = FirebaseAuth.getInstance();     //for signing users out

        //functionality to sign users out
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().build();

        gsc = GoogleSignIn.getClient(this, gso);

//        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);      //initalizing shared preferences

        String userName = sharedPreferences.getString("userName", "Default Name");

        // Update the navigation drawer header with the user's name
        updateNavigationDrawerHeader(userName);

        ImageView notificationIcon = findViewById(R.id.notificationIcon);   //notification icon
        notificationIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.passcodes:
                        Toast.makeText(HomePageActivity.this, "Passcodes Will Be Displayed Here", Toast.LENGTH_SHORT).show();
                        break;

                    case R.id.devices:
                        Toast.makeText(HomePageActivity.this, "Devices Enrolled Will Be Displayed Here", Toast.LENGTH_SHORT).show();
                        break;

                    case R.id.prompt:
                        checkAndauth();
                        break;

                    case R.id.logOut:
                        signOut();
                        break;
                }
                return false;
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //initializing ZXing integrator
                IntentIntegrator integrator = new IntentIntegrator(HomePageActivity.this);
                integrator.setPrompt("Scan a QR Code");
                integrator.setOrientationLocked(false);  //to lock the screen orientation as portrait
                integrator.initiateScan();
            }
        });


        if (biometricPrompt==null){
            biometricPrompt=new BiometricPrompt(this,executor,callback);
        }

//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder()
//                .url("ws://cpp-server-ip-address:5080")
//                .build();
//
//        WebSocketListener webSocketListener = new WebSocketListener() {
//            @Override
//            public void onOpen(WebSocket webSocket, Response response) {
//                super.onOpen(webSocket, response);
//                // WebSocket connection established
//            }
//
//            @Override
//            public void onMessage(WebSocket webSocket, String text) {
//                super.onMessage(webSocket, text);
//                // Received a text message
//            }
//
//            @Override
//            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
//                super.onFailure(webSocket, t, response);
//                // WebSocket connection failure
//            }
//        };
//
//        webSocket = client.newWebSocket(request, webSocketListener);

//        //initialize OkHttpClient and WebSocket connection
//        client = new OkHttpClient.Builder().build();
//        Request request = new Request.Builder()
//                .url("ws://your-server-address:port/your-websocket-endpoint") // Replace with your endpoint
//                .build();
//
//        //to receive and check for message
//        webSocket = client.newWebSocket(request, new WebSocketListener() {
//            @Override
//            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
//                super.onMessage(webSocket, text);
//                //check if the message has the word "Bio"
//                if (text.toLowerCase().contains("bio")) {
//                    checkAndauth();
//                }
//            }
//        });

        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        handler = new Handler();
        handler.postDelayed(checkRunnable, 1000);
    }

    private Runnable checkRunnable = new Runnable() {
        @Override
        public void run() {
            checkCall();
            handler.postDelayed(this, 1000);
        }
    };

    private void checkCall() {
        Call<CheckResponse> call = apiService.checkCall();
        call.enqueue(new Callback<CheckResponse>() {
            @Override
            public void onResponse(Call<CheckResponse> call, retrofit2.Response<CheckResponse> response) {
                if (response.isSuccessful()) {
                    CheckResponse checkResponse = response.body();
                    if (checkResponse != null) {
                        String status = checkResponse.getStatus();
                        if ("not_found".equals(status)) {
                            //continue loop
                        } else if ("request_found".equals(status)) {
                            //initiate biometric authentication by first checking and authenticating user device
                            checkAndauth();
                        }
                    } else {
                        //handle error
                        Toast.makeText(HomePageActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<CheckResponse> call, Throwable t) {
                //handle failure
                Toast.makeText(getApplicationContext(), "An error has occured", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Function to update navigation drawer header with user's name
    private void updateNavigationDrawerHeader(String userName) {
        View headerView = navigationView.getHeaderView(0);
        TextView textViewUserName = headerView.findViewById(R.id.textViewUserName);

        // Concatenate "Welcome, " with the user's display name
        String welcomeMessage = "Welcome, " + userName;

        // Update the TextView with the concatenated message
        textViewUserName.setText(welcomeMessage);
    }

    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START))
        {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        else
        {
            super.onBackPressed();
        }
    }

    //handle the result of the QR code scan
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            String scannedData = result.getContents();

            if (scannedData != null && isValidQRCode(scannedData)) {
//                processScannedData(scannedData);
            } else {
                //display invalid QR code message
                Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //method to validate whether QR code is in the correct format
    private boolean isValidQRCode(String data) {
        //validation logic here
        return data.startsWith("PREFIX_");
    }

//    //device registration process
//    private void processScannedData(String scannedData) {
//        //send the scanned data to the server for registration
//        registerDeviceWithServer(scannedData);
//    }

//    private void registerDeviceWithServer(String scannedData) {
//        //here we will send scanned data to server using Retrofit library
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl("http://your-server.com/")
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        YourApiService service = retrofit.create(YourApiService.class);
//
//        //creating a request body containing the scanned data
//        RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), scannedData);
//
//        //Making a POST request to connect the device
//        Call<ResponseBody> call = service.establishConnection(requestBody);
//        call.enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                if (response.isSuccessful()) {
//                    //device registered successfully
//                    Toast.makeText(HomePageActivity.this, "Device Registered Successfully", Toast.LENGTH_SHORT).show();
//                } else {
//                    //error registering device
//                    Toast.makeText(HomePageActivity.this, "Error Registering Device", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                // Network error
//                Toast.makeText(HomePageActivity.this, "Network error", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    //communication process b/w the Android and Desktop apps using the REST API server
//    //a method to unlock folders
//    private void unlockFolders() {
//        // Assuming you have the authentication token obtained during registration
//        String authToken = "your_authentication_token";
//
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl("http://your-server.com/")
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        YourApiService service = retrofit.create(YourApiService.class);
//
//        // Create a request body containing necessary data
//        RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), authToken);
//
//        // Make a POST request to unlock folders
//        Call<Void> call = service.unlockFolders(requestBody);
//        call.enqueue(new Callback<Void>() {
//            @Override
//            public void onResponse(Call<Void> call, Response<Void> response) {
//                if (response.isSuccessful()) {
//                    // Folders unlocked successfully
//                    Toast.makeText(HomePageActivity.this, "Folders unlocked successfully", Toast.LENGTH_SHORT).show();
//                } else {
//                    // Error unlocking folders
//                    Toast.makeText(HomePageActivity.this, "Error unlocking folders", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<Void> call, Throwable t) {
//                // Network error
//                Toast.makeText(HomePageActivity.this, "Network error", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }

    //for bottom sheet notification dialog
    private void showDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheetlayout);

        LinearLayout editLayout = dialog.findViewById(R.id.layoutEdit);

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    //to check whether the device has fingerprint sensor functionality or not
    void checkAndauth() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch(biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                showBiometricPrompt(buildPrompt());
                break;

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "Device Doesn't Support Fingerprint Sensor", Toast.LENGTH_SHORT).show();
                break;

            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "Biometric Authentication Unsuccessful", Toast.LENGTH_SHORT).show();
                break;

            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                promptEnrollFingerprint();
                break;
        }
    }

    //method to launch fingerprint registration system
    private void promptEnrollFingerprint() {
        // Redirect the user to enroll their fingerprint
        // For example, you can launch the system settings to enroll fingerprint
        Intent enrollIntent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
        startActivityForResult(enrollIntent, REQUEST_CODE_ENROLL_FINGERPRINT);
    }

    //method to build Biometric Prompt
    BiometricPrompt.PromptInfo buildPrompt() {
        String title = "Unlock Your Files";
        String subtitle = "Use your registered fingerprint to unlock files on your connected desktop device";
        String description = "This will securely authenticate and unlock the files using your fingerprint";

        return new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButtonText("Cancel") // Set a non-empty string for the negative button, VERY important for the prompt to work
                .build();
    }

    // method to show Biometric Prompt
    void showBiometricPrompt(BiometricPrompt.PromptInfo promptInfo) {
        biometricPrompt = new BiometricPrompt(this, Executors.newSingleThreadExecutor(), callback);
        biometricPrompt.authenticate(promptInfo);
    }

    //method to authenticate Biometric Prompt and provide appropriate callback
    BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
        //create instances of interface
        SecApiService secApiService = postRetrofitClient.getRetrofitInstance().create(SecApiService.class);

        @Override
        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
            if (biometricPrompt != null)
                biometricPrompt.cancelAuthentication();
            super.onAuthenticationError(errorCode, errString);
        }

        @Override
        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(HomePageActivity.this, "Biometric Authentication Successful: Files Unlocked", Toast.LENGTH_SHORT).show();
//                    Toast.makeText(HomePageActivity.this, "REST API call made successfully", Toast.LENGTH_SHORT).show();
//                }
//            });
//
//            String response = "Biometric authentication successful!";
//            webSocket.send(response);



            //send the request asynchronously
            Call<CreateResponse> call = secApiService.postCreateResponse(new CreateResponse("Correct"));

            call.enqueue(new Callback<CreateResponse>() {
                @Override
                public void onResponse(Call<CreateResponse> call, Response<CreateResponse> response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HomePageActivity.this, "Biometric Authentication Successful: Files Unlocked", Toast.LENGTH_SHORT).show();
                            Toast.makeText(HomePageActivity.this, "REST API call made successfully", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onFailure(Call<CreateResponse> call, Throwable t) {
                    Toast.makeText(HomePageActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            //send the request asynchronously
            Call<CreateResponse> call = secApiService.postCreateResponse(new CreateResponse("Wrong"));

            call.enqueue(new Callback<CreateResponse>() {
                @Override
                public void onResponse(Call<CreateResponse> call, Response<CreateResponse> response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HomePageActivity.this, "Biometric Authentication Unsuccessful: Files Unlocked", Toast.LENGTH_SHORT).show();
                            Toast.makeText(HomePageActivity.this, "REST API call was not made", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onFailure(Call<CreateResponse> call, Throwable t) {
                    Toast.makeText(HomePageActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    //method to sign users out from GoogleSignInClient
    void signOut() {
        mAuth.signOut();
        gsc.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Clear saved authentication state
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("isLoggedIn", false);
                    editor.apply();

                    // Redirect to MainActivity
                    Intent intent = new Intent(HomePageActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    // Handle sign-out failure
                    Toast.makeText(HomePageActivity.this, "Sign out failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
