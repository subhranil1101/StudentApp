package com.project.studentattendance;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.FirebaseApp;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.common.api.ApiException;



import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long UPDATE_INTERVAL = 5000; // 5 seconds
    private static final long FASTEST_INTERVAL = 2000; // 2 seconds
    private static final int RC_SIGN_IN = 9001;

    private Button btnGetLocation;
    private TextView tvLocation;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private DatabaseReference databaseReference;

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Add this line at the beginning of your onCreate method


        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);

        btnGetLocation = findViewById(R.id.btnGetLocation);
        tvLocation = findViewById(R.id.tvLocation);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            Log.d("Firebase", "User is authenticated: " + user.getDisplayName());
            // Continue with writing data to Firebase
        } else {
            Log.d("Firebase", "User is not authenticated");
            // Handle the case when the user is not authenticated
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://location-data-b5fe2-default-rtdb.firebaseio.com/");

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize Google Sign-In
        // Initialize Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("411672249625-eg552pfl6s8vljn2vnh8e8dd8eiibgeb.apps.googleusercontent.com")
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);


        btnGetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocationPermission();
            }
        });

        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                updateLocationUI(location);

                // Send student data to Firebase when button is clicked
                if (location != null) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        sendStudentDataToFirebase(user.getDisplayName(), user.getEmail(), location.getLatitude(), location.getLongitude());
                    }
                }
            }
        };
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission has already been granted
            requestLocationUpdates();
        }
    }

    private void requestLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void updateLocationUI(Location location) {
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            String locationText = "Latitude: " + latitude + "\nLongitude: " + longitude;
            tvLocation.setText(locationText);
        }
    }

    private void sendStudentDataToFirebase(String name, String email, double latitude, double longitude) {
        // Create a Map to store student data
        Map<String, Object> studentMap = new HashMap<>();
        studentMap.put("name", name);
        studentMap.put("email", email);
        studentMap.put("latitude", latitude);
        studentMap.put("longitude", longitude);

        // Push data to Firebase Realtime Database
        databaseReference.child(name).setValue(studentMap)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle the error
                        Log.e("Firebase", "Error writing to database", e);
                    }
                }).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Data successfully written to the database
                        Log.e("Firebase", "Error writing to database");
                        // Add any additional actions here
                    }
                });

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                requestLocationUpdates();
            } else {
                // Permission denied
                tvLocation.setText("Location permission denied");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            signIn();
        }
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

// ...

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                    .addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
                        @Override
                        public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                            try {
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                // Google Sign-In successful, update UI accordingly
                                handleGoogleSignInResult(account);
                            } catch (ApiException e) {
                                // Google Sign-In failed, update UI accordingly
                                Log.w("GoogleSignIn", "Google sign-in failed", e);
                            }
                        }
                    });
        }
    }

    private void handleGoogleSignInResult(GoogleSignInAccount account) {
        try {
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success
                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                Log.d("Firebase", "User signed in: " + user.getDisplayName());
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w("Firebase", "signInWithCredential:failure", task.getException());
                            }
                        }
                    });
        } catch (NullPointerException e) {
            Log.e("GoogleSignIn", "NullPointerException during handleGoogleSignInResult", e);
        }
    }

}

