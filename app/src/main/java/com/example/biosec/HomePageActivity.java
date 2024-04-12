package com.example.biosec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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

import com.example.biosec.network.YourApiService;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Callback;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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
        setContentView(R.layout.activity_homepage);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        drawerToggle = new ActionBarDrawerToggle(this,drawerLayout,R.string.open,R.string.close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        scanButton = findViewById(R.id.scanner);

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

        ImageView notificationIcon = findViewById(R.id.notificationIcon);   //notification icon
        notificationIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //initializing ZXing integrator
                IntentIntegrator integrator = new IntentIntegrator(HomePageActivity.this);
                integrator.setCaptureActivity(HomePageActivity.class);
                integrator.setPrompt("Scan a QR Code");
                integrator.setOrientationLocked(false);  //to lock the screen orientation as portrait
            }
        });

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

        if(biometricPrompt==null){
            biometricPrompt=new BiometricPrompt(this,executor,callback);
        }
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

    //handle the result of the QR code scan
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                // Handle the scanned QR code data
                String scannedData = result.getContents();
                processScannedData(scannedData);    //call method to process scanned data
//                if (scannedData.equals("YOUR_DEVICE_ID")) {
//                    // Device ID matched, do something
//                    Toast.makeText(this, "Device ID matched: " + scannedData, Toast.LENGTH_LONG).show();
//                } else {
//                    // Device ID didn't match
//                    Toast.makeText(this, "Invalid device ID", Toast.LENGTH_LONG).show();
//                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    //device registration process
    private void processScannedData(String scannedData) {
        //send the scanned data to the server for registration
        registerDeviceWithServer(scannedData);
    }

    private void registerDeviceWithServer(String scannedData) {
        //here we will send scanned data to server using Retrofit library
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://your-server.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        YourApiService service = retrofit.create(YourApiService.class);

        //creating a request body containing the scanned data
        RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), scannedData);

        //Making a POST request to register the device
        Call<Void> call = service.registerDevice(requestBody);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    //device registered successfully
                    Toast.makeText(HomePageActivity.this, "Device Registered Successfully", Toast.LENGTH_SHORT).show();
                } else {
                    //error registering device
                    Toast.makeText(HomePageActivity.this, "Error Registering Device", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Network error
                Toast.makeText(HomePageActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //communication process b/w the Android and Desktop apps using the REST API server
    //a method to unlock folders
    private void unlockFolders() {
        // Assuming you have the authentication token obtained during registration
        String authToken = "your_authentication_token";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://your-server.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        YourApiService service = retrofit.create(YourApiService.class);

        // Create a request body containing necessary data
        RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), authToken);

        // Make a POST request to unlock folders
        Call<Void> call = service.unlockFolders(requestBody);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Folders unlocked successfully
                    Toast.makeText(HomePageActivity.this, "Folders unlocked successfully", Toast.LENGTH_SHORT).show();
                } else {
                    // Error unlocking folders
                    Toast.makeText(HomePageActivity.this, "Error unlocking folders", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Network error
                Toast.makeText(HomePageActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
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
                BiometricPrompt.PromptInfo promptInfo = buildPrompt();
                biometricPrompt.authenticate(promptInfo);
                break;

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "Device Doesn't Support Fingerprint Sensor", Toast.LENGTH_SHORT).show();
                break;

            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "Biometric Authentication Unsuccessful", Toast.LENGTH_SHORT).show();

            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, "No Fingerprint Assigned", Toast.LENGTH_SHORT).show();
        }
    }

    //method to build biometric prompt
    BiometricPrompt.PromptInfo buildPrompt() {
        return new BiometricPrompt.PromptInfo.Builder()
                .setTitle("BioSec")
                .build();
    }

    //method to build and authenticate biometric prompt
    BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                if (biometricPrompt != null)
                    biometricPrompt.cancelAuthentication();
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(HomePageActivity.this, "Biometric Authentication Successful", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
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