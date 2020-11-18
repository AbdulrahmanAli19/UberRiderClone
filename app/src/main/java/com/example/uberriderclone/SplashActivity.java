package com.example.uberriderclone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.uberriderclone.model.RiderModel;
import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.User;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;

public class SplashActivity extends AppCompatActivity {
    private final static int LOGIN_REQUEST = 700;
    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener listener;

    private FirebaseDatabase database;
    private DatabaseReference riderRef;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        delaySplashScreen();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (auth != null && listener != null)
            auth.removeAuthStateListener(listener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            } else {
                Toast.makeText(this, response.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void delaySplashScreen() {
        progressBar.setVisibility(View.VISIBLE);
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(() -> auth.addAuthStateListener(listener));
    }

    private void init() {
        ButterKnife.bind(this);

        database = FirebaseDatabase.getInstance();
        riderRef = database.getReference(Common.RIDER_INFO_REF);

        providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        auth = FirebaseAuth.getInstance();
        listener = myFirebaseListener -> {
            FirebaseUser user = myFirebaseListener.getCurrentUser();
            if (user != null) {
                checkUserFromFirebase();
            } else {
                showLoginlayout();
            }
        };
    }

    private void showLoginlayout() {
        AuthMethodPickerLayout pickerLayout = new AuthMethodPickerLayout
                .Builder(R.layout.layout_sign_in)
                .setPhoneButtonId(R.id.btn_phone_sign_in)
                .setGoogleButtonId(R.id.btn_google_sign_in)
                .build();

        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(pickerLayout)
                .setIsSmartLockEnabled(false)
                .setTheme(R.style.LoginScreen)
                .setAvailableProviders(providers)
                .build(), LOGIN_REQUEST);
    }

    private void checkUserFromFirebase() {
        riderRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            RiderModel riderModel = snapshot.getValue(RiderModel.class);
                            goToHomeActivity(riderModel);
                        } else {
                            showRegisterLayout();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SplashActivity.this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRegisterLayout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DilaogTheme);

        View view = LayoutInflater.from(this).inflate(R.layout.layout_registeration, null);

        TextInputLayout firstNameLayout = view.findViewById(R.id.input_first_name);
        TextInputLayout lastNameLayout = view.findViewById(R.id.input_last_name);
        TextInputLayout phoneNumberLayout = view.findViewById(R.id.input_phone_number);
        Button btnContinue = view.findViewById(R.id.btn_save);

        if (FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() != null
                && TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()))
            phoneNumberLayout.getEditText()
                    .setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        btnContinue.setOnClickListener(v -> {
            if (TextUtils.isEmpty(firstNameLayout.getEditText().getText().toString())) {
                firstNameLayout.setError("First name can't be empty");
                return;
            } else if (TextUtils.isEmpty(lastNameLayout.getEditText().getText().toString())) {
                lastNameLayout.setError("Last name can't be empty");
                return;
            } else if (TextUtils.isEmpty(phoneNumberLayout.getEditText().getText().toString())) {
                phoneNumberLayout.setError("Phone number can't be empty");
                return;
            } else {
                RiderModel driver = new RiderModel();
                driver.setFirstName(firstNameLayout.getEditText().getText().toString());
                driver.setLastName(lastNameLayout.getEditText().getText().toString());
                driver.setPhoneNumber(phoneNumberLayout.getEditText().getText().toString());


                riderRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(driver)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Registered success", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            goToHomeActivity(driver);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }


        });
    }

    private void goToHomeActivity(RiderModel riderModel) {
        Common.currentRider = riderModel;
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}