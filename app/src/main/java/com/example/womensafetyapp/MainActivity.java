package com.example.womensafetyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth fAuth;
    FirebaseAuth.AuthStateListener fAuthListener;
    FirebaseFirestore db ;
    DocumentReference dRef ;
    FirebaseUser fb_user;


    private ProgressBar registerProgressBar , signInProgressBar;
    private EditText email_sign_in, pw_sign_in, user_name, email_register, pw_register, confirm_pw_register;

    private Button btn_sign_in, btn_register;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);


        try {
            fAuth = FirebaseAuth.getInstance();
        }catch( Exception e)
        {
            Log.e("Auth Instance", "exception e:"+e.getMessage());

        }

        db =  FirebaseFirestore.getInstance();

        fAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() != null) {
                    Log.d("User Auth listener", "User already logged in");
                    finish();
                    Intent i = new Intent(MainActivity.this, AlertActivity.class);
                    startActivity(i);
                }

            }
        };

        email_sign_in = findViewById(R.id.email_sign_in);
        pw_sign_in = findViewById(R.id.password_sign_in);
        user_name = findViewById(R.id.user_name_register);
        email_register = findViewById(R.id.email_register);
        pw_register = findViewById(R.id.password_register);
        confirm_pw_register = findViewById(R.id.confirm_password_register);

        btn_register = findViewById(R.id.btn_register);
        btn_sign_in = findViewById(R.id.btn_sign_in);
        registerProgressBar = findViewById(R.id.register_progress_bar);
        signInProgressBar = findViewById(R.id.sign_in_progress_bar);


        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("registration btn click", "inside onclick");
                registerProgressBar.setVisibility(View.VISIBLE);

                final String userName = user_name.getText().toString().trim();
                String email = email_register.getText().toString().trim();
                String password = pw_register.getText().toString().trim();
                String confirm_password = confirm_pw_register.getText().toString().trim();

                if (!confirm_password.equals(password)) {
                    Toast.makeText(MainActivity.this, "Passwords don't match", Toast.LENGTH_SHORT);
                    return;
                }

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(MainActivity.this, "Enter email and password", Toast.LENGTH_SHORT);
                    return;
                }

                fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d("creating user", "inside registration oncomplete");

                        if (task.isSuccessful()) {
                            Log.d("User Authentication", "User registered successfully");
                            Toast.makeText(MainActivity.this, "Registration Successful", Toast.LENGTH_SHORT);
                            fb_user = fAuth.getCurrentUser();
                            String  kj = fb_user.getEmail();

                            Map<String, Object> user = new HashMap<>();
                            user.put("user_name", userName);

                            db.collection("users").document(fb_user.getEmail())
                                    .set(user)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void a_void) {
                                            Log.d("MainActivity", "DocumentSnapshot added with ID: ");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w("MainActivity", "Error adding document", e);
                                        }
                                    });

                            registerProgressBar.setVisibility(View.GONE);
                            finish();
                            Intent i = new Intent(MainActivity.this, AlertActivity.class);
                            Log.d("Inside registration", "sending intent now");
                            startActivity(i);
                        } else {
                            Log.e("User Registration", "onComplete: Failed=" + task.getException().getMessage());
                            Log.d("User Authentication", "User registration failed");
                            Toast.makeText(MainActivity.this, "Registration Failed. Try again later", Toast.LENGTH_SHORT);
                            registerProgressBar.setVisibility(View.GONE);

                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Registration Failed. Try again later", Toast.LENGTH_SHORT);
                        Log.e("User Registration", "onComplete: Failed=" + e.getMessage());
                        registerProgressBar.setVisibility(View.GONE);

                    }
                });

            }
        });

        btn_sign_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("signin btn", "inside onclick");
                signInProgressBar.setVisibility(View.VISIBLE);
                String email = email_sign_in.getText().toString().trim();
                String password = pw_sign_in.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(MainActivity.this, "Enter email and password", Toast.LENGTH_SHORT);
                    return;
                }

                fAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("User SignIn", "User signed in Successfully");
                            signInProgressBar.setVisibility(View.GONE);
//                            finish();
//                            Intent i = new Intent(MainActivity.this, AlertActivity.class);
                            Log.d("Inside SignIn complete", "Sending intent now");
//                            startActivity(i);
                        } else {
                            Toast.makeText(MainActivity.this, "SignIn Failed. Try again later", Toast.LENGTH_SHORT);
                            Log.e("User Registration", "onComplete: Failed=" + task.getException().getMessage());
                            Log.d("User SignIn", "User sign in Unsuccessful");
                            signInProgressBar.setVisibility(View.GONE);
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("User SignIn", "User sign in Unsuccessful. exception: "+e.getMessage());
                        signInProgressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "SignIn Failed. Try again later", Toast.LENGTH_SHORT);
                    }
                });
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        fAuth.addAuthStateListener(fAuthListener);

//        FirebaseUser currentUser = fAuth.getCurrentUser();
//        if (currentUser == null) {
//            Log.i("Account Activity", "No user logged in");
//
//        } else {
//
//            Log.i("Account Activity", "One user logged in");
//        }
    }
}
